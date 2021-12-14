# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_show_reports boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_report_latest_departure_time time;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_show_reports;
ALTER TABLE organization DROP COLUMN attendance_report_latest_departure_time;