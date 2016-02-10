# --- !Ups

CREATE TABLE comments (
   id              INTEGER NOT NULL,
   person_id       INTEGER NOT NULL,
   user_id       INTEGER NOT NULL,
   message         TEXT,
   created         timestamp DEFAULT NOW(),
   constraint pk_comments primary key(id),
   constraint fk_comments_person foreign key (person_id) references person (person_id) on delete restrict on update restrict,
   constraint fk_comments_user foreign key (user_id) references users (id) on delete restrict on update restrict
);

create sequence comments_seq;


# --- !Downs

DROP TABLE IF EXISTS comments;
DROP sequence IF EXISTS comments_seq;

