-- name: activate-class-y!
-- Set a single class to be active, and unactivate all others
UPDATE overseer.classes SET active = (_id = :id) where school_id = :school_id;

-- name: delete-student-from-class-y!
DELETE FROM overseer.classes_X_students
WHERE student_id = :student_id
AND class_id = :class_id;

-- name: delete-inactive-students-y!
-- delete all students not in a list
DELETE
FROM overseer.students
WHERE _id NOT IN(:students_to_keep);
