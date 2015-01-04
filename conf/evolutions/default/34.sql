# --- !Ups

ALTER TABLE chapter ALTER COLUMN num TYPE VARCHAR(8);

# --- !Downs

ALTER TABLE chapter ALTER COLUMN num TYPE int;
