# --- !Ups

CREATE TABLE person_tag (
   tag_id          bigint NOT NULL,
   person_id       INTEGER NOT NULL,
   creator_id      bigint NOT NULL,
   created         timestamp DEFAULT NOW(),
   constraint pk_person_tag primary key(tag_id, person_id)
);


CREATE TABLE tag (
   id            bigint NOT NULL,
   title         VARCHAR(255) NOT NULL,
   constraint pk_tag primary key(id),
   constraint unique_title UNIQUE(title)
);

create sequence tag_seq;



alter table person_tag add constraint fk_person_tag_person_1 foreign key (person_id) references person (person_id) on delete restrict on update restrict;
create index ix_person_tag_1 on person_tag (person_id);

alter table person_tag add constraint fk_person_tag_tag_1 foreign key (tag_id) references tag (id) on delete restrict on update restrict;
create index ix_person_tag_2 on person_tag (tag_id);

alter table person_tag add constraint fk_person_tag_creator_1 foreign key (creator_id) references users (id) on delete restrict on update restrict;


ALTER TABLE person ALTER COLUMN dob TYPE DATE;
ALTER TABLE person ALTER COLUMN approximate_dob TYPE DATE;


# --- !Downs

DROP TABLE IF EXISTS person_tag;
DROP TABLE IF EXISTS tag;
DROP sequence IF EXISTS tag_seq;

ALTER TABLE person ALTER COLUMN dob TYPE timestamp;
ALTER TABLE person ALTER COLUMN approximate_dob TYPE timestamp;
