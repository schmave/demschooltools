ALTER TABLE overseer.students
DROP CONSTRAINT fk_student_school;

--;; 

ALTER TABLE overseer.students DROP COLUMN school_id;
