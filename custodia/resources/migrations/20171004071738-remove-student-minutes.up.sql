ALTER TABLE overseer.students DROP COLUMN olderdate DATE;
--;;
CREATE TABLE overseer.students_required_minutes (student_id BIGINT NOT NULL, required_minutes INT NOT NULL, fromdate DATE NOT NULL);
