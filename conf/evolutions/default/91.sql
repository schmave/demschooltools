# --- !Ups

ALTER TABLE organization ALTER COLUMN attendance_day_min_hours TYPE double precision;

# --- !Downs

ALTER TABLE organization ALTER COLUMN attendance_day_min_hours TYPE integer;