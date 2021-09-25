# --- !Ups
ALTER TABLE attendance_code ADD COLUMN not_counted boolean NOT NULL DEFAULT false;

# --- !Downs
ALTER TABLE attendance_code DROP COLUMN not_counted;
