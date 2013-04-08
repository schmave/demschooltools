# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table student (
  id                        bigint auto_increment not null,
  first_name                varchar(255),
  last_name                 varchar(255),
  constraint pk_student primary key (id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table student;

SET FOREIGN_KEY_CHECKS=1;

