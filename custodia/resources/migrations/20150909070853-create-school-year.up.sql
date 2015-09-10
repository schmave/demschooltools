CREATE TABLE school_years(
       _id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255),
       active BOOLEAN NOT NULL DEFAULT FALSE
);

-- ;;


CREATE TABLE school_years_X_students(
       school_year_id BIGINT NOT NULL REFERENCES school_years(_id),
       student_id BIGINT NOT NULL REFERENCES students(_id)
);
