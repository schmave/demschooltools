ALTER TABLE phillyfreeschool.swipes
ADD COLUMN swipe_day date DEFAULT null;
--;;
UPDATE phillyfreeschool.swipes s1
SET swipe_day =
    (select
    (CASE WHEN date(s2.in_time AT TIME ZONE 'America/New_York')  IS NULL
    THEN date(s2.out_time AT TIME ZONE 'America/New_York')
    ELSE date(s2.in_time AT TIME ZONE 'America/New_York') END)
    from phillyfreeschool.swipes s2 where s1._id = s2._id);
--;;
ALTER TABLE demo.swipes
ADD COLUMN swipe_day date DEFAULT null;
--;;
UPDATE demo.swipes s1
SET swipe_day =
(select
(CASE WHEN date(s2.in_time AT TIME ZONE 'America/New_York')  IS NULL
THEN date(s2.out_time AT TIME ZONE 'America/New_York')
ELSE date(s2.in_time AT TIME ZONE 'America/New_York') END)
from demo.swipes s2 where s1._id = s2._id);
--;;
DROP FUNCTION IF EXISTS phillyfreeschool.student_school_days(bigint,text,bigint);
--;;
CREATE OR REPLACE FUNCTION phillyfreeschool.student_school_days(stu_id BIGINT, y_name TEXT, cls_id BIGINT)
RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
  BEGIN
    CREATE TEMP TABLE temp1 ON COMMIT DROP AS
    SELECT DISTINCT s.swipe_day as days
    FROM phillyfreeschool.swipes s
    JOIN phillyfreeschool.classes_X_students cXs
         ON (cXs.class_id = $3 AND s.student_id = cXs.student_id)
    INNER JOIN phillyfreeschool.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
    WHERE y.name = $2
    ORDER BY s.swipe_day;
  END;
  BEGIN
    RETURN QUERY
    SELECT a.days, s._id student_id, s.archived, s.olderdate
    FROM temp1 AS a
    JOIN phillyfreeschool.students s ON (s._id = $1)
    WHERE (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION demo.student_school_days(stu_id BIGINT, y_name TEXT, cls_id BIGINT)
RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
BEGIN
CREATE TEMP TABLE temp1 ON COMMIT DROP AS
SELECT DISTINCT s.swipe_day as days
FROM demo.swipes s
JOIN demo.classes_X_students cXs
ON (cXs.class_id = $3 AND s.student_id = cXs.student_id)
INNER JOIN demo.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
WHERE y.name = $2
ORDER BY s.swipe_day;
END;
BEGIN
RETURN QUERY
SELECT a.days, s._id student_id, s.archived, s.olderdate
FROM temp1 AS a
JOIN demo.students s ON (s._id = $1)
WHERE (s.start_date < a.days OR s.start_date is null);
END;
RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION phillyfreeschool.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
  BEGIN
    CREATE TEMP TABLE temp1 ON COMMIT DROP AS
    SELECT DISTINCT s.swipe_day as days
    FROM phillyfreeschool.swipes s
    JOIN phillyfreeschool.classes_X_students cXs
         ON (cXs.class_id = $2 AND s.student_id = cXs.student_id)
    INNER JOIN phillyfreeschool.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
    WHERE y.name = $1
    ORDER BY s.swipe_day;
  END;
  BEGIN
    RETURN QUERY
    SELECT a.days, s._id student_id, s.archived, s.olderdate
    FROM temp1 AS a
    JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = $2)
    JOIN phillyfreeschool.students s ON (s._id = cXs.student_id)
    WHERE cXs.class_id = $2
    AND (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION demo.school_days(year_name TEXT, class_id BIGINT)
RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
  BEGIN
    CREATE TEMP TABLE temp1 ON COMMIT DROP AS
    SELECT DISTINCT s.swipe_day as days
    FROM demo.swipes s
    JOIN demo.classes_X_students cXs
         ON (cXs.class_id = $2 AND s.student_id = cXs.student_id)
    INNER JOIN demo.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
    WHERE y.name = $1
    ORDER BY s.swipe_day;
  END;
  BEGIN
    RETURN QUERY
    SELECT a.days, s._id student_id, s.archived, s.olderdate
    FROM temp1 AS a
    JOIN demo.classes_X_students cXs ON (cXs.class_id = $2)
    JOIN demo.students s ON (s._id = cXs.student_id)
    WHERE cXs.class_id = $2
    AND (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
