# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_day_earliest_departure_time time;
ALTER TABLE attendance_rule ADD COLUMN earliest_departure_time time;

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_day_earliest_departure_time;
ALTER TABLE attendance_rule DROP COLUMN earliest_departure_time;