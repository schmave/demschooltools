# --- !Ups

ALTER TABLE attendance_day ADD COLUMN off_campus_departure_time time;
ALTER TABLE attendance_day ADD COLUMN off_campus_return_time time;

# --- !Downs

ALTER TABLE organization DROP COLUMN off_campus_departure_time;
ALTER TABLE organization DROP COLUMN off_campus_return_time;