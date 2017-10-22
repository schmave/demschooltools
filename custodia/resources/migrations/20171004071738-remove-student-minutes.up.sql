CREATE TABLE overseer.students_required_minutes (student_id BIGINT NOT NULL, required_minutes INT NOT NULL, fromdate DATE NOT NULL);
--;;
INSERT INTO overseer.students_required_minutes
SELECT
  _id AS student_id, 300 AS required_minutes, date '2000, 1, 1' AS fromdate
FROM
  overseer.students
WHERE
  olderdate IS NULL;
--;;
INSERT INTO overseer.students_required_minutes
SELECT
_id AS student_id, 330 AS required_minutes, olderdate AS fromdate
FROM
overseer.students
WHERE
olderdate IS NOT NULL;
--;;
ALTER TABLE overseer.students DROP COLUMN olderdate DATE;
--;;
DROP FUNCTION IF EXISTS overseer.student_school_days(bigint,text,bigint);
--;;
DROP FUNCTION IF EXISTS overseer.school_days( TEXT,  BIGINT);
--;;
CREATE VIEW overseer.school_days AS (
  SELECT DISTINCT
         s.swipe_day AS school_day,
         cXs.class_id,
         y.name AS year_name
  FROM overseer.swipes s
  JOIN overseer.classes_X_students cXs
       ON (s.student_id = cXs.student_id)
  INNER JOIN overseer.years y
       ON (s.swipe_day BETWEEN y.from_date AND y.to_date)
  GROUP BY school_day, class_id, year_name
  ORDER BY s.swipe_day
);
--;;
CREATE VIEW overseer.student_newest_required_minutes AS (
SELECT DISTINCT fromdate, srm.required_minutes, srm.student_id
FROM overseer.students_required_minutes srm
GROUP BY fromdate, required_minutes, student_id
);
