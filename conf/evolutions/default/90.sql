# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_rate_standard_time_frame integer;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_rate_standard_time_frame;
