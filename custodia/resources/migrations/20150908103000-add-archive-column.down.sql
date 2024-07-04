DROP FUNCTION school_days(text);

--;;

CREATE OR REPLACE FUNCTION school_days(year_name text)
RETURNS TABLE (days date, student_id BIGINT, olderdate date) AS
$func$
SELECT a.days, students._id student_id, students.olderdate FROM (SELECT DISTINCT days2.days
FROM (SELECT
(CASE WHEN date(s.in_time at time zone 'America/New_York')  IS NULL
THEN date(s.out_time at time zone 'America/New_York')
ELSE date(s.in_time at time zone 'America/New_York') END) as days
FROM roundedswipes s
INNER JOIN years y
ON ((s.out_time BETWEEN y.from_date AND y.to_date)
OR (s.in_time BETWEEN y.from_date AND y.to_date))
WHERE y.name= $1) days2
ORDER BY days2.days) as a
JOIN students on (1=1)
$func$
LANGUAGE sql;

--;;

ALTER TABLE students
DROP COLUMN IF EXISTS archived;
