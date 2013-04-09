# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table person (
  person_id                 integer not null,
  first_name                varchar(255),
  last_name                 varchar(255),
  constraint pk_person primary key (person_id))
;

create sequence person_seq;




# --- !Downs

drop table if exists person cascade;

drop sequence if exists person_seq;

