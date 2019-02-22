# --- !Ups
ALTER TABLE users ADD COLUMN hashed_password TEXT NOT NULL DEFAULT('');

# --- !Downs

ALTER TABLE users DROP COLUMN hashed_password;
