# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_enable_off_campus boolean NOT NULL DEFAULT(false);

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_enable_off_campus;