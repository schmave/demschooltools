# --- !Ups

ALTER TABLE attendance_day ADD COLUMN off_campus_departure_time time;
ALTER TABLE attendance_day ADD COLUMN off_campus_return_time time;

# --- !Downs

ALTER TABLE attendance_day DROP COLUMN off_campus_departure_time;
ALTER TABLE attendance_day DROP COLUMN off_campus_return_time;
