# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table student (
  student_id                integer not null,
  first_name                varchar(255),
  last_name                 varchar(255),
  constraint pk_student primary key (student_id))
;

create sequence student_seq;




# --- !Downs

drop table if exists student cascade;

drop sequence if exists student_seq;

