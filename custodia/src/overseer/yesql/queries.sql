-- name: get-overrides-in-year-y
-- Get a list of overrides in a year for a given student
SELECT e.*
       ,'overrides' AS type
FROM overseer.overrides e
INNER JOIN overseer.years y
      ON (e.date BETWEEN y.from_date AND y.to_date)
WHERE y.name= :year_name AND e.student_id = :student_id;

-- name: lookup-last-swipe-y
-- Get a student's last swipe
SELECT *
FROM overseer.swipes s
WHERE s.student_id = :student_id AND s.in_time IS NOT NULL
ORDER BY s.in_time DESC
LIMIT 1;

-- name: get-excuses-in-year-y
-- Get a student's excuses for a year
SELECT e.*
       ,'excuses' AS type
FROM overseer.excuses e
INNER JOIN overseer.years y
      ON (e.date BETWEEN y.from_date AND y.to_date)
WHERE y.name= :year_name AND e.student_id = :student_id;

-- name: get-student-page-y
WITH days AS (
       SELECT DISTINCT s.swipe_day as days
       FROM overseer.swipes s
       JOIN overseer.classes_X_students cXs
           ON (cXs.class_id = :class_id AND s.student_id = cXs.student_id)
       INNER JOIN overseer.years y
             ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
       WHERE y.name = :year_name
       ORDER BY s.swipe_day
    ),
    student_newest_required_minutes as (
      SELECT fromdate, srm.required_minutes, srm.student_id
      FROM overseer.students_required_minutes srm
      WHERE fromdate = (SELECT MAX(fromdate)
                        FROM overseer.students_required_minutes isrm
                        WHERE srm.student_id = isrm.student_id)
      AND srm.student_id = :student_id
    ),
    student_school_days AS (
      SELECT a.days, s._id student_id
      , s.archived
      , (CASE WHEN stumin.required_minutes IS NULL
      THEN c.required_minutes ELSE stumin.required_minutes END) as requiredmin
      FROM days AS a
      JOIN overseer.students s ON (s._id = 3)
      LEFT JOIN student_newest_required_minutes stumin ON (stumin.fromdate <= a.days)
      JOIN overseer.classes c ON (c._id = 1)
      WHERE (s.start_date < a.days OR s.start_date IS NULL)
    )
SELECT
  schooldays.student_id
  , to_char(s.in_time at time zone :timezone, 'HH:MI:SS') as nice_in_time
  , to_char(s.out_time at time zone :timezone, 'HH:MI:SS') as nice_out_time
  , s.out_time
  , s.in_time
  , s.rounded_out_time
  , s.rounded_in_time
  , s.intervalmin
  , o._id has_override
  , e._id has_excuse
  , s._id
  , schooldays.archived
  , (CASE WHEN s._id IS NOT NULL THEN 'swipes' ELSE '' END) as type
  , schooldays.requiredmin
  , schooldays.days AS day
FROM student_school_days AS schooldays
LEFT JOIN overseer.swipes s
      ON (
       ((schooldays.days = date(s.in_time AT TIME ZONE :timezone))
       OR
        (schooldays.days = date(s.out_time AT TIME ZONE :timezone)))
        AND s.student_id = :student_id)
LEFT JOIN overseer.overrides o
     ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
LEFT JOIN overseer.excuses e
     ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
WHERE schooldays.days IS NOT NULL
ORDER BY schooldays.days DESC;

-- name: student-list-in-out-y
select stu.name
       , stu._id
       , stu.show_as_absent
       , stu.is_teacher
       , stu.archived
       , CASE WHEN l.outs >= l.ins THEN 'out'
         ELSE 'in'
         END AS last_swipe_type
       , CASE WHEN l.outs >= l.ins THEN l.outs
         ELSE l.ins
         END AS last_swipe_date
         FROM overseer.students stu
LEFT JOIN (SELECT max(s.in_time) AS ins
                , max(s.out_time) AS outs
                , s.student_id
                FROM overseer.swipes s
           GROUP BY s.student_id
           ORDER BY ins, outs) AS l
     ON (l.student_id = stu._id)
     INNER JOIN overseer.classes c ON (1=1)
     INNER JOIN overseer.classes_X_students cXs ON (cXs.student_id = stu._id AND cXs.class_id = c._id)
WHERE (stu.archived = :show_archived
       OR stu.archived = FALSE)
AND c.active = TRUE
AND stu.school_id = :school_id
ORDER BY stu.name
;

-- name: get-school-days-y
SELECT DISTINCT days2.days
FROM (SELECT
             to_char(s.in_time at time zone :timezone, 'YYYY-MM-DD') as days
             , s.in_time
             FROM overseer.swipes s
             INNER JOIN overseer.students stu on (stu._id = s.student_id AND stu.school_id = :school_id)
             INNER JOIN overseer.years y
             ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                 OR (s.in_time BETWEEN y.from_date AND y.to_date))
      WHERE y.name = :year_name) days2
ORDER BY days2.days;

-- name: student-report-y
WITH days AS (
       SELECT DISTINCT s.swipe_day as days
       FROM overseer.swipes s
       JOIN overseer.classes_X_students cXs
           ON (cXs.class_id = :class_id AND s.student_id = cXs.student_id)
       INNER JOIN overseer.years y
             ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
       WHERE y.name = :year_name
       ORDER BY s.swipe_day
    ),
    student_newest_required_minutes as (
      SELECT fromdate, srm.required_minutes, srm.student_id
      FROM overseer.students_required_minutes srm
      WHERE fromdate = (SELECT MAX(fromdate)
                        FROM overseer.students_required_minutes isrm
                        WHERE srm.student_id = isrm.student_id)
    ),
    school_days AS (
      SELECT a.days, s._id student_id, s.archived
      , (CASE WHEN stumin.required_minutes IS NULL
         THEN c.required_minutes ELSE stumin.required_minutes END) AS requiredmin
      FROM days AS a
      JOIN overseer.classes_X_students cXs ON (cXs.class_id = :class_id)
      JOIN overseer.students s ON (s._id = cXs.student_id)
      JOIN overseer.classes c ON (c._id = :class_id)
      LEFT JOIN student_newest_required_minutes stumin ON (
           stumin.student_id = s._id
           AND stumin.fromdate <= a.days
      )
      WHERE cXs.class_id = :class_id
      AND (s.start_date < a.days OR s.start_date IS NULL)
  ), eachDay AS (
      SELECT
      schooldays.student_id
      , max(s._id) anyswipes
      , max(o._id) oid
      , max(e._id) eid
      , schooldays.requiredmin
      , sum(s.intervalmin) as intervalmin
      , schooldays.days AS day
      FROM school_days as schooldays
      LEFT JOIN overseer.swipes s
      ON (schooldays.days = date(s.in_time AT TIME ZONE :timezone)
      AND schooldays.student_id = s.student_id)
      LEFT JOIN overseer.overrides o
      ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN overseer.excuses e
      ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
      WHERE schooldays.days IS NOT NULL
      GROUP BY schooldays.student_id, day, requiredmin
)
SELECT
  stu.student_id
    , stu.student_id as _id
    , (select s.name from overseer.students s WHERE s._id = stu.student_id) as name
    , round(sum(CASE WHEN oid IS NOT NULL THEN stu.requiredmin/60 ELSE stu.intervalmin/60 END)) as total_hours
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
FROM eachDay AS stu
GROUP BY stu.student_id;

-- name: swipes-in-year-y
SELECT s.*
       ,'swipes' AS type
       , s.intervalmin as interval
       , to_char(s.in_time at time zone :timezone, 'HH:MI:SS') as nice_in_time
       , to_char(s.out_time at time zone :timezone, 'HH:MI:SS') as nice_out_time
       FROM overseer.swipes s
       INNER JOIN overseer.students stu ON (stu._id = s.student_id)
       INNER JOIN overseer.years y
      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
          OR (s.in_time BETWEEN y.from_date AND y.to_date))
WHERE y.name= :year_name
  AND stu.school_id = :school_id
  AND s.student_id = :student_id;

-- name: get-classes-and-students-y
SELECT s._id as student_id
       , cXs.class_id
       FROM overseer.students s
       LEFT JOIN overseer.classes_X_students cXs
   ON (cXs.student_id = s._id AND cXs.class_id = :class_id)
WHERE s.school_id = :school_id
ORDER BY s.name;

-- name: get-active-class-y
SELECT _id from overseer.classes where active = true and school_id = :school_id;

-- name: get-students-y
WITH student_newest_required_minutes as (
  SELECT fromdate, srm.required_minutes, srm.student_id
  FROM overseer.students_required_minutes srm
  WHERE fromdate = (SELECT MAX(fromdate)
                    FROM overseer.students_required_minutes isrm
                    WHERE srm.student_id = isrm.student_id
                    AND fromdate <= current_date
                    )
)
SELECT s.* , snrm.required_minutes
FROM overseer.students s
LEFT JOIN student_newest_required_minutes snrm
     ON (snrm.student_id = s._id)
WHERE s.school_id = :school_id;

-- name: get-student-y
WITH student_newest_required_minutes as (
  SELECT fromdate, srm.required_minutes, srm.student_id
  FROM overseer.students_required_minutes srm
  WHERE fromdate = (SELECT MAX(fromdate)
                    FROM overseer.students_required_minutes isrm
                    WHERE srm.student_id = isrm.student_id
                    AND fromdate <= current_date
                    )
)
SELECT s.* , snrm.required_minutes
FROM overseer.students s
LEFT JOIN student_newest_required_minutes snrm
     ON (snrm.student_id = :student_id)
WHERE s._id = :student_id
AND s.school_id = :school_id;

-- name: get-years-y
SELECT * from overseer.years where school_id = :school_id;

-- name: get-schools-y
SELECT * from overseer.schools;

-- name: get-classes-y
SELECT c.name, c._id, c.from_date, c.to_date, c.active, cXs.student_id, s.name student_name, c.required_minutes
FROM overseer.classes c
LEFT JOIN overseer.classes_X_students cXs ON (cXs.class_id = c._id)
LEFT JOIN overseer.students s ON (cXs.student_id = s._id)
WHERE c.school_id = :school_id
ORDER BY c.name;

-- name: get-class-y
SELECT * from overseer.classes where name = :name and school_id = :school_id;

-- name: get-students-with-dst-y
SELECT p.first_name, p.last_name, p.display_name, p.person_id, stu.*
  from tag t
  join person_tag pt on t.id=pt.tag_id
  join person p on pt.person_id=p.person_id
  left join overseer.students stu on stu.dst_id=p.person_id
  where t.show_in_jc=true and p.organization_id=:school_id
  GROUP BY p.person_id, stu._id;

-- name: get-schools-with-dst-y
SELECT *
  from organization o
  left join overseer.schools s on o.id=s._id;
