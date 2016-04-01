CREATE TABLE classes(
       _id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255),
       inserted_date timestamp default now(),
       active BOOLEAN NOT NULL DEFAULT FALSE
);
-- ;;
CREATE TABLE classes_X_students(
       class_id BIGINT NOT NULL REFERENCES classes(_id),
       student_id BIGINT NOT NULL REFERENCES students(_id)
);
