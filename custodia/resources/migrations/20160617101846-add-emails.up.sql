CREATE TABLE emails(
       _id BIGSERIAL PRIMARY KEY,
       email VARCHAR(255),
       inserted_date TIMESTAMP DEFAULT NOW()
);
