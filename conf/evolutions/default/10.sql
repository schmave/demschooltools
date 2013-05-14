# --- !Ups

alter table person add column gender varchar(10) not null default 'Unknown'
   check(gender = 'Unknown' or gender='Male' or gender='Female');

# --- !Downs

alter table person drop column gender;

