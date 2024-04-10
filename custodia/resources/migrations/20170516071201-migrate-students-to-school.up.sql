ALTER TABLE overseer.students ADD COLUMN school_id bigserial;

--;;

UPDATE overseer.students SET school_id=1;

--;;

ALTER TABLE overseer.students
ADD CONSTRAINT fk_student_school
FOREIGN KEY (school_id)
REFERENCES overseer.schools(_id);

--;;

ALTER TABLE overseer.classes ADD COLUMN school_id bigserial;

--;;

UPDATE overseer.classes SET school_id=1;

--;;

ALTER TABLE overseer.classes
ADD CONSTRAINT fk_class_school
FOREIGN KEY (school_id)
REFERENCES overseer.schools(_id);

--;;

ALTER TABLE overseer.years ADD COLUMN school_id bigserial;

--;;

UPDATE overseer.years SET school_id=1;

--;;

ALTER TABLE overseer.years
ADD CONSTRAINT fk_class_school
FOREIGN KEY (school_id)
REFERENCES overseer.schools(_id);

--;;

ALTER TABLE users ADD COLUMN school_id bigserial;

--;;

UPDATE users SET school_id=1;

