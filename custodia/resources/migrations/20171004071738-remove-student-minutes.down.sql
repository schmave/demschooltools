ALTER TABLE overseer.students ADD COLUMN olderdate date;
--;;
DROP FUNCTION IF EXISTS overseer.student_school_days(bigint,text,bigint);
--;;
CREATE OR REPLACE FUNCTION overseer.student_school_days(stu_id BIGINT, y_name TEXT, cls_id BIGINT)
RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
  BEGIN
    CREATE TEMP TABLE temp1 ON COMMIT DROP AS
    SELECT DISTINCT s.swipe_day as days
    FROM overseer.swipes s
    JOIN overseer.classes_X_students cXs
         ON (cXs.class_id = $3 AND s.student_id = cXs.student_id)
    INNER JOIN overseer.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
    WHERE y.name = $2
    ORDER BY s.swipe_day;
  END;
  BEGIN
    RETURN QUERY
    SELECT a.days, s._id student_id, s.archived, s.olderdate
    FROM temp1 AS a
    JOIN overseer.students s ON (s._id = $1)
    WHERE (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION overseer.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS $$
BEGIN
  BEGIN
    CREATE TEMP TABLE temp1 ON COMMIT DROP AS
    SELECT DISTINCT s.swipe_day as days
    FROM overseer.swipes s
    JOIN overseer.classes_X_students cXs
         ON (cXs.class_id = $2 AND s.student_id = cXs.student_id)
    INNER JOIN overseer.years y ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
    WHERE y.name = $1
    ORDER BY s.swipe_day;
  END;
  BEGIN
    RETURN QUERY
    SELECT a.days, s._id student_id, s.archived, s.olderdate
    FROM temp1 AS a
    JOIN overseer.classes_X_students cXs ON (cXs.class_id = $2)
    JOIN overseer.students s ON (s._id = cXs.student_id)
    WHERE cXs.class_id = $2
    AND (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
