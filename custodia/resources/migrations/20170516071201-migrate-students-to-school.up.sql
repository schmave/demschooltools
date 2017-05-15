ALTER TABLE overseer.students ADD COLUMN school_id bigserial;

--;;

UPDATE overseer.students SET school_id=1;

--;;

ALTER TABLE overseer.students
ADD CONSTRAINT fk_student_school
FOREIGN KEY (school_id)
REFERENCES overseer.schools(_id);
