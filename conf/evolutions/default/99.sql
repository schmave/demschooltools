# --- !Ups

ALTER TABLE organization ADD COLUMN attendance_show_rate_in_checkin boolean NOT NULL DEFAULT(false);

# --- !Downs

ALTER TABLE organization DROP COLUMN attendance_show_rate_in_checkin;
