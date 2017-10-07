ALTER TABLE overseer.students DROP COLUMN olderdate date;
--;;
CREATE TABLE overseer.students_required_minutes (student_id BIGINT NOT NULL, required_minutes INT NOT NULL, fromdate DATE NOT NULL);
--;;
DROP FUNCTION IF EXISTS overseer.student_newest_required_minutes(bigint,text,bigint);
--;;
CREATE OR REPLACE FUNCTION overseer.student_newest_required_minutes(stu_id BIGINT, afterDate DATE)
RETURNS TABLE (fromdate date, requiredmin int) AS $$
BEGIN
  RETURN QUERY
    SELECT fromdate, srm.required_minutes
    FROM overseer.students_required_minutes srm
    WHERE srm.student_id = $1
    AND fromdate = (SELECT MAX(fromdate) FROM overseer.students_required_minutes WHERE srm.student_id = $1)
    AND fromdate <= $2;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
DROP FUNCTION IF EXISTS overseer.student_school_days(bigint,text,bigint);
--;;
CREATE OR REPLACE FUNCTION overseer.student_school_days(stu_id BIGINT, y_name TEXT, cls_id BIGINT)
RETURNS TABLE (days date, student_id BIGINT, archived boolean, requiredmin int) AS $$
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
    SELECT a.days, s._id student_id
           , s.archived
           , (CASE WHEN stumin.required_minutes IS NULL
           THEN c.required_minutes ELSE stumin.required_minutes END) as requiredmin
    FROM temp1 AS a
    JOIN overseer.students s ON (s._id = $1)
    LEFT JOIN (overseer.student_newest_required_minutes($1,a.days)) stumin ON (1=1)
    JOIN overseer.classes c ON (c._id = $3)
    WHERE (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION overseer.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, requiredmin int) AS $$
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
    SELECT a.days, s._id student_id, s.archived
    , (CASE WHEN stumin.required_minutes IS NULL
       THEN c.required_minutes ELSE stumin.required_minutes END) as requiredmin
    FROM temp1 AS a
    JOIN overseer.classes_X_students cXs ON (cXs.class_id = $2)
    JOIN overseer.students s ON (s._id = cXs.student_id)
    JOIN overseer.classes c ON (c._id = $2)
    LEFT JOIN (SELECT fromdate, srm.required_minutes
               FROM overseer.students_required_minutes srm
               WHERE srm.student_id = $1
               AND fromdate = (SELECT MAX(fromdate) FROM overseer.students_required_minutes WHERE srm.student_id = $1)
              ) stumin ON (stumin.fromdate <= a.days)
    WHERE cXs.class_id = $2
    AND (s.start_date < a.days OR s.start_date is null);
  END;
  RETURN;
END;
$$
LANGUAGE plpgsql;
--;;
