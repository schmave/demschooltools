# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_report_late_fee integer;
ALTER TABLE organization ADD COLUMN attendance_report_late_fee_interval integer;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_report_late_fee;
ALTER TABLE organization DROP COLUMN attendance_report_late_fee_interval;
