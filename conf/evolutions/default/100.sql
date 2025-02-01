# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_default_absence_code text;
ALTER TABLE organization ADD COLUMN attendance_default_absence_code_time time;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_default_absence_code;
ALTER TABLE organization DROP COLUMN attendance_default_absence_code_time;
