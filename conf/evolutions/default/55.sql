# --- !Ups

ALTER TABLE attendance_code ALTER COLUMN code TYPE VARCHAR(64);
ALTER TABLE attendance_day ALTER COLUMN code TYPE VARCHAR(64);

# --- !Downs

ALTER TABLE attendance_code ALTER COLUMN code TYPE VARCHAR(8);
ALTER TABLE attendance_day ALTER COLUMN code TYPE VARCHAR(8);

