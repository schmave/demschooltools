# --- !Ups

CREATE TABLE person_tag_change
(
   id serial NOT NULL,
   person_id INTEGER NOT NULL,
   tag_id INTEGER NOT NULL,
   creator_id INTEGER NOT NULL,
   time timestamp without time zone DEFAULT now(),
   was_add BOOLEAN NOT NULL,
   constraint pk_ptc primary key(id),
   constraint fk_ptc_person foreign key(person_id) references person(person_id),
   constraint fk_ptc_tag foreign key(tag_id) references tag(id),
   constraint fk_ptc_creator foreign key(creator_id) references users(id)
);

INSERT INTO person_tag_change (person_id, tag_id, creator_id, time, was_add)
  (SELECT person_id, tag_id, creator_id, created, true FROM person_tag);

ALTER TABLE person_tag DROP constraint fk_person_tag_creator_1;
ALTER TABLE person_tag DROP creator_id;
ALTER TABLE person_tag DROP created;

# --- !Downs


