ALTER TABLE phillyfreeschool.students
      ADD COLUMN start_date date;
ALTER TABLE demo.students
      ADD COLUMN start_date date;

CREATE OR REPLACE FUNCTION phillyfreeschool.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$

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
         JOIN phillyfreeschool.classes c ON (c.active = true)
         JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = c._id
                                         AND s.student_id = cXs.student_id)
         WHERE y.name = $1) days2
         ORDER BY days2.days) AS a
JOIN phillyfreeschool.classes_X_students cXs ON (1=1)
JOIN phillyfreeschool.students s ON (s._id = cXs.student_id)
WHERE cXs.class_id = $2
AND (s.start_date < a.days OR s.start_date is null);
$func$
LANGUAGE sql;

CREATE OR REPLACE FUNCTION demo.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$

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
         JOIN phillyfreeschool.classes c ON (c.active = true)
         JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = c._id
                                         AND s.student_id = cXs.student_id)
         WHERE y.name = $1) days2
         ORDER BY days2.days) AS a
JOIN phillyfreeschool.classes_X_students cXs ON (1=1)
JOIN phillyfreeschool.students s ON (s._id = cXs.student_id)
WHERE cXs.class_id = $2
AND (s.start_date < a.days OR s.start_date is null);
$func$
LANGUAGE sql;
