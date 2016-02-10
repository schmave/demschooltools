# --- !Ups

create table phone_numbers (
  id                        bigint not null,
  person_id                 integer not null,
  comment                   varchar(255),
  number                    varchar(255),
  constraint pk_phone_numbers primary key (id))
;

create sequence phone_numbers_seq;

alter table phone_numbers add constraint fk_phone_numbers_person_1 foreign key (person_id) references person (person_id) on delete restrict on update restrict;
create index ix_phone_numbers_1 on phone_numbers (person_id);


# --- !Downs

drop table if exists phone_numbers;
drop sequence if exists phone_numbers_seq;

