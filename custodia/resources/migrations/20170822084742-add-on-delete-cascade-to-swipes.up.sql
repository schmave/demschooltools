ALTER TABLE overseer.swipes
ADD CONSTRAINT swipes_student_id_fkey
    FOREIGN KEY (student_id)
    REFERENCES overseer.students(_id)
    ON DELETE CASCADE;

ALTER TABLE overseer.classes_X_students
DROP CONSTRAINT classes_x_students_student_id_fkey,
ADD CONSTRAINT classes_x_students_student_id_fkey
    FOREIGN KEY (student_id)
    REFERENCES overseer.students(_id)
    ON DELETE CASCADE;
