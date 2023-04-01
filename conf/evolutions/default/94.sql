# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_report_late_fee_2 integer;
ALTER TABLE organization ADD COLUMN attendance_report_late_fee_interval_2 integer;
ALTER TABLE organization ADD COLUMN attendance_report_latest_departure_time_2 time;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_report_late_fee_2;
ALTER TABLE organization DROP COLUMN attendance_report_late_fee_interval_2;
ALTER TABLE organization DROP COLUMN attendance_report_latest_departure_time_2;
