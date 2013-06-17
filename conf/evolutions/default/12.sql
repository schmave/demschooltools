# --- !Ups

create table donation (
   id serial,
   dollar_value float,
   is_cash boolean not null,
   description TEXT not null,
   person_id integer not null,
   date timestamp without time zone not null default now(),

   thanked boolean not null,
   thanked_by_user_id integer default null,
   thanked_time timestamp without time zone default null,

   indiegogo_reward_given boolean not null,
   indiegogo_reward_by_user_id integer default null,
   indiegogo_reward_given_time timestamp without time zone default null,

   constraint pk_donation primary key(id),
   constraint fk_donation_person foreign key(person_id) references person(person_id) ON DELETE cascade ON UPDATE restrict,

   constraint fk_donation_user_1 foreign key(thanked_by_user_id) references users(id) on delete restrict on update restrict,
   constraint fk_donation_user_2 foreign key(indiegogo_reward_by_user_id) references users(id) on delete restrict on update restrict
);

# --- !Downs

drop table donation;
