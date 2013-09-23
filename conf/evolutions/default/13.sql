# --- !Ups

alter table tag add column use_student_display boolean default false;

# --- !Downs

alter table tag drop column use_student_display;
