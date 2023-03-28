# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_show_weighted_percent boolean NOT NULL DEFAULT(false);
ALTER TABLE organization DROP COLUMN attendance_rate_standard_time_frame;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_show_weighted_percent;
ALTER TABLE organization ADD COLUMN attendance_rate_standard_time_frame integer;
