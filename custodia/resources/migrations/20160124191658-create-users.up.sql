CREATE TABLE users(
  user_id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255),
  roles VARCHAR(255),
  inserted_date TIMESTAMP DEFAULT NOW()
);
