# --- !Ups

alter table person drop constraint person_gender_check;
alter table person add constraint person_gender_check
   check(gender = 'Unknown' or gender='Male' or gender='Female' or gender='Other');

# --- !Downs

alter table person drop constraint person_gender_check;
alter table person add constraint person_gender_check
   check(gender = 'Unknown' or gender='Male' or gender='Female');

