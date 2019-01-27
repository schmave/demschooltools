# --- !Ups

ALTER TABLE attendance_code ADD COLUMN counts_toward_attendance boolean NOT NULL DEFAULT(false);

ALTER TABLE organization ADD COLUMN attendance_enable_partial_days boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_day_min_start_time time;
ALTER TABLE organization ADD COLUMN attendance_day_min_hours integer;
ALTER TABLE organization ADD COLUMN attendance_partial_day_value decimal;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_partial_day_value;
ALTER TABLE organization DROP COLUMN attendance_day_min_hours;
ALTER TABLE organization DROP COLUMN attendance_day_min_start_time;
ALTER TABLE organization DROP COLUMN attendance_enable_partial_days;

ALTER TABLE attendance_code DROP COLUMN counts_toward_attendance;