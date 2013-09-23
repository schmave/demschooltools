# --- !Ups

create table person (
  person_id                 integer not null,
  first_name                varchar(255),
  last_name                 varchar(255),
  notes                     TEXT,
  address                   varchar(255),
  city                      varchar(255),
  state                     varchar(255),
  zip                       varchar(255),
  neighborhood              varchar(255),
  email                     varchar(255),
  dob                       timestamp,
  approximate_dob           timestamp,
  is_family                 boolean,
  family_person_id          integer,
  constraint pk_person primary key (person_id))
;

create sequence person_seq;

alter table person add constraint fk_person_family_1 foreign key (family_person_id) references person (person_id);
create index ix_person_family_1 on person (family_person_id);



# --- !Downs

drop table if exists person cascade;

drop sequence if exists person_seq;

