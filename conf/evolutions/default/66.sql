# --- !Ups

alter table tag add column show_in_attendance boolean not null default false;
update tag set show_in_attendance=show_in_jc;

# --- !Downs

alter table tag drop column show_in_attendance;
