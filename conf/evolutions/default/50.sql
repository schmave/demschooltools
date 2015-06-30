# --- !Ups

ALTER TABLE attendance_week ALTER COLUMN extra_hours TYPE REAL;

# --- !Downs

ALTER TABLE attendance_week ALTER COLUMN extra_hours TYPE INTEGER;

