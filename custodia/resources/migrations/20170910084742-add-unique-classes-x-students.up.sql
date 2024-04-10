ALTER TABLE overseer.classes_X_students
ADD CONSTRAINT classes_x_students_unique
    UNIQUE (student_id, class_id);
