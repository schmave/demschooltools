ALTER TABLE overseer.students_required_minutes ADD CONSTRAINT student_id_fromdate UNIQUE (student_id, fromdate);
