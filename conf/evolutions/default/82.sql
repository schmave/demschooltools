# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_admin_pin VARCHAR(10) NOT NULL DEFAULT('');

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_admin_pin;
