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
WITH student_school_days AS (
      SELECT
      a.school_day AS days,
      s._id        AS student_id
      , s.archived
      , (CASE WHEN stumin.required_minutes IS NULL
              THEN c.required_minutes ELSE stumin.required_minutes END)
              AS requiredmin
      FROM overseer.school_days AS a
      JOIN overseer.students s ON (s._id = :student_id)
      LEFT JOIN overseer.student_newest_required_minutes stumin
           ON (stumin.student_id = :student_id
              AND stumin.fromdate = (SELECT MAX(isrm.fromdate)
                                     FROM overseer.students_required_minutes isrm
                                     WHERE isrm.fromdate <= a.school_day
                                     AND stumin.student_id = isrm.student_id))
      JOIN overseer.classes c ON (c._id = :class_id)
      WHERE (s.start_date < a.school_day OR s.start_date IS NULL)
            AND a.class_id = :class_id
            AND a.year_name = :year_name
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
ORDER BY schooldays.days DESC, s.in_time ASC;

-- name: students-required-minutes-y
SELECT * FROM overseer.students_required_minutes WHERE student_id = :student_id AND fromdate = :fromdate;

-- name: student-list-in-out-y
SELECT
  stu.name,
  stu._id,
  stu.show_as_absent,
  stu.is_teacher,
  stu.archived,
  c.late_time AS late_time,
  l.last_swipe_type,
  l.last_swipe_date,
  l.last_swipe_date > (current_timestamp at time zone sch.timezone)::date as swiped_today,
  l.first_in_today at time zone sch.timezone >
       ((current_timestamp at time zone sch.timezone)::date + c.late_time) as swiped_today_late
FROM
  overseer.students stu
  LEFT JOIN (
  (SELECT
      CASE WHEN subl.outs >= subl.ins THEN 'out' ELSE 'in' END AS last_swipe_type,
      CASE WHEN subl.outs >= subl.ins THEN subl.outs ELSE subl.ins END AS last_swipe_date,
      subl.min_in as first_in_today,
      subl.student_id
      FROM (SELECT
              max(s.in_time) AS ins,
              max(s.out_time) AS outs,
              min(s.in_time) as min_in,
              s.student_id
            FROM overseer.swipes s
            JOIN overseer.students stu on s.student_id = stu._id
            JOIN overseer.schools sch on stu.school_id = sch._id
            WHERE (s.in_time at time zone sch.timezone)::date =
                      (current_timestamp at time zone sch.timezone)::date
                AND sch._id = :school_id
            GROUP BY s.student_id
            ORDER BY ins, outs) as subl)) AS l ON (l.student_id = stu._id)
  INNER JOIN overseer.classes c ON (1 = 1)
  INNER JOIN overseer.classes_X_students cXs ON (cXs.student_id = stu._id
          AND cXs.class_id = c._id)
  INNER JOIN overseer.schools sch on c.school_id=sch._id
  WHERE (stu.archived = :show_archived
        OR stu.archived = FALSE)
    AND c.active = TRUE
    AND stu.school_id = :school_id
  ORDER BY
      stu.name;

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
WITH
    school_days AS (
      SELECT a.school_day, s._id student_id, s.archived
      , (CASE WHEN stumin.required_minutes IS NULL
         THEN c.required_minutes ELSE stumin.required_minutes END) AS requiredmin
      FROM overseer.school_days AS a
      JOIN overseer.classes_X_students cXs ON (cXs.class_id = :class_id)
      JOIN overseer.students s ON (s._id = cXs.student_id)
      JOIN overseer.classes c ON (c._id = :class_id)
      LEFT JOIN overseer.student_newest_required_minutes AS stumin ON (
           stumin.student_id = s._id
           AND stumin.fromdate = (SELECT MAX(isrm.fromdate)
                                 FROM overseer.students_required_minutes isrm
                                 WHERE isrm.fromdate <= a.school_day
                                 AND stumin.student_id = isrm.student_id))
      WHERE cXs.class_id = :class_id
      AND a.class_id = :class_id
      AND a.year_name = :year_name
      AND (s.start_date < a.school_day OR s.start_date IS NULL)
  ), eachDay AS (
      SELECT
      schooldays.student_id
      , max(s._id) anyswipes
      , max(o._id) oid
      , max(e._id) eid
      , schooldays.requiredmin
      , sum(s.intervalmin) as intervalmin
      , schooldays.school_day AS day
      FROM school_days as schooldays
      LEFT JOIN overseer.swipes s
      ON (schooldays.school_day = date(s.in_time AT TIME ZONE :timezone)
      AND schooldays.student_id = s.student_id)
      LEFT JOIN overseer.overrides o
      ON (schooldays.school_day = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN overseer.excuses e
      ON (schooldays.school_day = e.date AND e.student_id = schooldays.student_id)
      WHERE schooldays.school_day IS NOT NULL
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
SELECT s.*
       , (CASE WHEN snrm.required_minutes IS NULL
          THEN c.required_minutes ELSE snrm.required_minutes END)
           AS required_minutes
FROM overseer.students s
LEFT JOIN overseer.student_newest_required_minutes snrm
     ON (snrm.student_id = s._id
         AND snrm.fromdate = (SELECT MAX(isrm.fromdate)
                              FROM overseer.students_required_minutes isrm
                              WHERE isrm.fromdate <= current_date
                              AND snrm.student_id = isrm.student_id))
LEFT JOIN overseer.classes_X_students cXs ON cXs.student_id = s._id
LEFT JOIN overseer.classes c ON cXs.class_id=c._id
WHERE s.school_id = :school_id;

-- name: get-student-y
SELECT s.*
       , (CASE WHEN snrm.required_minutes IS NULL
         THEN c.required_minutes ELSE snrm.required_minutes END)
         AS required_minutes
FROM overseer.students s
LEFT JOIN overseer.student_newest_required_minutes snrm
     ON (snrm.student_id = s._id
         AND snrm.fromdate = (SELECT MAX(isrm.fromdate)
                              FROM overseer.students_required_minutes isrm
                              WHERE isrm.fromdate <= current_date
                              AND snrm.student_id = isrm.student_id))
LEFT JOIN overseer.classes_X_students cXs
     ON cXs.student_id = s._id
LEFT JOIN overseer.classes c ON cXs.class_id = c._id
WHERE s._id = :student_id
    AND s.school_id = :school_id;

-- name: get-years-y
SELECT * from overseer.years where school_id = :school_id;

-- name: get-schools-y
SELECT * from overseer.schools;

-- name: get-classes-y
SELECT
  c.name,
  c._id,
  c.from_date,
  c.to_date,
  c.active,
  cXs.student_id,
  s.name student_name,
  c.required_minutes,
  ((current_timestamp at time zone sch.timezone)::date + c.late_time) as late_time
FROM
  overseer.classes c
LEFT JOIN overseer.schools sch on c.school_id = sch._id
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
  where t.show_in_attendance=true and p.organization_id=:school_id
  GROUP BY p.person_id, stu._id;

-- name: get-schools-with-dst-y
SELECT *
  from organization o
  left join overseer.schools s on o.id=s._id;
