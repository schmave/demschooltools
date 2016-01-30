-- name: clean-swipes-y
-- Gives a list of swipes rounded
SELECT * FROM phillyfreeschool.roundedswipes;

-- name: get-overrides-in-year-y
-- Get a list of overrides in a year for a given student
SELECT e.*
       ,'overrides' AS type
FROM phillyfreeschool.overrides e
INNER JOIN phillyfreeschool.years y
      ON (e.date BETWEEN y.from_date AND y.to_date)
WHERE y.name= :year_name AND e.student_id = :student_id;

-- name: lookup-last-swipe-y
-- Get a student's last swipe
SELECT *
FROM phillyfreeschool.swipes s
WHERE s.student_id = :student_id AND s.in_time IS NOT NULL
ORDER BY s.in_time DESC
LIMIT 1;

-- name: get-excuses-in-year-y
-- Get a student's excuses for a year
SELECT e.*
       ,'excuses' AS type
FROM phillyfreeschool.excuses e
INNER JOIN phillyfreeschool.years y
      ON (e.date BETWEEN y.from_date AND y.to_date)
WHERE y.name= :year_name AND e.student_id = :student_id;

-- name: get-student-page-y
SELECT
  schooldays.student_id
  , to_char(s.in_time at time zone 'America/New_York', 'HH:MI:SS') as nice_in_time
  , to_char(s.out_time at time zone 'America/New_York', 'HH:MI:SS') as nice_out_time
  , s.out_time
  , s.in_time
  , extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60 as intervalmin
  , o._id has_override
  , e._id has_excuse
  , schooldays.olderdate
  , s._id
  , schooldays.archived
  , (CASE WHEN s._id IS NOT NULL THEN 'swipes' ELSE '' END) as type
  , (CASE WHEN schooldays.olderdate IS NULL
               OR schooldays.olderdate > schooldays.days
               THEN 300 ELSE 330 END) as requiredmin
  , schooldays.days AS day
  FROM phillyfreeschool.school_days(:year_name, :class_id) AS schooldays
  LEFT JOIN phillyfreeschool.roundedswipes s
      ON (
       ((schooldays.days = date(s.in_time AT TIME ZONE 'America/New_York'))
       OR
        (schooldays.days = date(s.out_time AT TIME ZONE 'America/New_York')))
        AND schooldays.student_id = s.student_id)
        LEFT JOIN phillyfreeschool.overrides o
      ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN phillyfreeschool.excuses e
      ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
    WHERE schooldays.days IS NOT NULL
          AND schooldays.student_id = :student_id
    ORDER BY schooldays.days DESC;

-- name: student-list-in-out-y
select stu.name
       , stu._id
       , stu.show_as_absent
       , stu.archived
       , CASE WHEN l.outs >= l.ins THEN 'out'
         ELSE 'in'
         END AS last_swipe_type
       , CASE WHEN l.outs >= l.ins THEN l.outs
         ELSE l.ins
         END AS last_swipe_date
         FROM phillyfreeschool.students stu
LEFT JOIN (SELECT max(s.in_time) AS ins
                , max(s.out_time) AS outs
                , s.student_id
                FROM phillyfreeschool.roundedswipes s
           GROUP BY s.student_id
           ORDER BY ins, outs) AS l
     ON (l.student_id = stu._id)
     INNER JOIN phillyfreeschool.classes c ON (1=1)
     INNER JOIN phillyfreeschool.classes_X_students cXs ON (cXs.student_id = stu._id AND cXs.class_id = c._id)
WHERE (stu.archived = :show_archived
       OR stu.archived = FALSE)
AND c.active = TRUE
;

-- name: get-school-days-y
SELECT DISTINCT days2.days
FROM (SELECT
             to_char(s.in_time at time zone 'America/New_York', 'YYYY-MM-DD') as days
             , s.in_time
             FROM phillyfreeschool.roundedswipes s
             INNER JOIN phillyfreeschool.years y
             ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                 OR (s.in_time BETWEEN y.from_date AND y.to_date))
      WHERE y.name = :year_name) days2
ORDER BY days2.days;

-- name: student-report-y
SELECT
  stu.student_id
    , stu.student_id as _id
    , (select s.name from phillyfreeschool.students s WHERE s._id = stu.student_id) as name
    , sum(CASE WHEN oid IS NOT NULL THEN stu.requiredmin/60 ELSE stu.intervalmin/60 END) as total_hours
    , sum(CASE WHEN oid IS NOT NULL
               OR stu.intervalmin >= stu.requiredmin
          THEN 1 ELSE 0 END) as good
    , sum(CASE WHEN oid IS NULL
               AND eid IS NULL
               AND (stu.intervalmin < stu.requiredmin
                    OR stu.intervalmin IS NULL)
               AND stu.anyswipes IS NOT NULL
          THEN 1 ELSE 0 END) as short
    , sum(CASE WHEN oid IS NOT NULL THEN 1 ELSE 0 END) as overrides
    , sum(CASE WHEN eid IS NOT NULL THEN 1 ELSE 0 END) as excuses
    , sum(CASE WHEN anyswipes IS NULL
                   AND eid IS NULL
                   AND oid IS NULL
           THEN 1 ELSE 0 END) as unexcused
FROM (
      SELECT
        schooldays.student_id
        , max(s._id) anyswipes
        , max(o._id) oid
        , max(e._id) eid
        , schooldays.olderdate
        , (CASE WHEN schooldays.olderdate IS NULL
                     OR schooldays.olderdate > schooldays.days
                     THEN 300 ELSE 330 END) as requiredmin
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) AS intervalmin
         , schooldays.days AS day

FROM phillyfreeschool.school_days(:year_name, :class_id) as schooldays
LEFT JOIN phillyfreeschool.roundedswipes s
                ON (schooldays.days = date(s.in_time AT TIME ZONE 'America/New_York')
                    AND schooldays.student_id = s.student_id)
                    LEFT JOIN phillyfreeschool.overrides o
           ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
           LEFT JOIN phillyfreeschool.excuses e
           ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
      WHERE schooldays.days IS NOT NULL
      GROUP BY schooldays.student_id, day, schooldays.olderdate
  ) AS stu
GROUP BY stu.student_id;

-- name: swipes-in-year-y
SELECT s.*
       ,'swipes' AS type
       , extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60 as interval
       , to_char(s.in_time at time zone 'America/New_York', 'HH:MI:SS') as nice_in_time
       , to_char(s.out_time at time zone 'America/New_York', 'HH:MI:SS') as nice_out_time
       FROM phillyfreeschool.roundedswipes s
       INNER JOIN phillyfreeschool.years y
      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
          OR (s.in_time BETWEEN y.from_date AND y.to_date))
WHERE y.name= :year_name
  AND s.student_id = :student_id;

-- name: activate-class-y!
-- Set a single class to be active, and unactivate all others
UPDATE phillyfreeschool.classes SET active = (_id = :id);

-- name: delete-student-from-class-y!
DELETE FROM phillyfreeschool.classes_X_students
WHERE student_id = :student_id
      AND class_id = :class_id;

-- name: get-classes-and-students-y
SELECT s._id as student_id
       , cXs.class_id
       FROM phillyfreeschool.students s
       LEFT JOIN phillyfreeschool.classes_X_students cXs
   ON (cXs.student_id = s._id AND cXs.class_id = :class_id);

-- name: get-active-class-y
SELECT _id from phillyfreeschool.classes where active = true;

-- name: get-classes-y
SELECT c.name, c._id, c.active, cXs.student_id, s.name student_name
FROM phillyfreeschool.classes c
LEFT JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = c._id)
LEFT JOIN phillyfreeschool.students s ON (cXs.student_id = s._id)
ORDER BY c.name;

-- name: school-days-for-class-year
SELECT a.days, s._id student_id, s.archived, s.olderdate
FROM (SELECT DISTINCT days2.days
    FROM (SELECT
            (CASE WHEN date(s.in_time AT TIME ZONE 'America/New_York')  IS NULL
            THEN date(s.out_time AT TIME ZONE 'America/New_York')
            ELSE date(s.in_time AT TIME ZONE 'America/New_York') END) AS days
            FROM phillyfreeschool.roundedswipes s
            INNER JOIN phillyfreeschool.years y
            ON ((s.out_time BETWEEN y.from_date AND y.to_date)
            OR (s.in_time BETWEEN y.from_date AND y.to_date))
            JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = y.class_id
                                         AND s.student_id = cXs.student_id)
         WHERE y.name = :year_name) days2
         ORDER BY days2.days) AS a
         JOIN phillyfreeschool.years y ON (1=1)
         JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = y.class_id)
         JOIN phillyfreeschool.students s ON (s._id = cXs.student_id)
WHERE y.name = :year_name;

-- name: get-user-y
SELECT * FROM users WHERE username = :username;
