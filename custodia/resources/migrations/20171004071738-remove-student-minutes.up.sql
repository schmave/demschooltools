ALTER TABLE overseer.students DROP COLUMN olderdate DATE;
--;;
CREATE TABLE overseer.students_required_minutes (student_id BIGINT NOT NULL, required_minutes INT NOT NULL, fromdate DATE NOT NULL);
--;;
DROP FUNCTION IF EXISTS overseer.student_school_days(bigint,text,bigint);
--;;
DROP FUNCTION IF EXISTS overseer.school_days( TEXT,  BIGINT);
