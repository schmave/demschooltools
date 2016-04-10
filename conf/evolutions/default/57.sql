# --- !Ups

ALTER TABLE attendance_week add constraint u_attendance_week unique(person_id, monday);
ALTER TABLE attendance_day add constraint u_attendance_day unique(person_id, day);

# --- !Downs

alter table attendance_week drop constraint u_attendance_week;
alter table attendance_day drop constraint u_attendance_day;
