# --- !Ups

ALTER TABLE attendance_day ADD COLUMN off_campus_minutes_exempted integer;

# --- !Downs

ALTER TABLE attendance_day DROP COLUMN off_campus_minutes_exempted;
