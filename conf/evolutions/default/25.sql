# --- !Ups

CREATE TABLE email
   (
    id serial,
    message TEXT,
	sent boolean not null,
	deleted boolean not null,
    constraint pk_email primary key (id)
    );

# --- !Downs

DROP TABLE email;
