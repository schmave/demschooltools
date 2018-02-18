# --- !Ups

CREATE TABLE institution (
   id serial,
   organization_id integer NOT NULL,
   name text NOT NULL,
   type integer NOT NULL,
   CONSTRAINT pk_institution primary key(id),
   CONSTRAINT fk_institution_organization FOREIGN KEY (organization_id) references organization(id)
);

CREATE TABLE account (
   id serial,
   organization_id integer NOT NULL,
   person_id integer,
   institution_id integer,
   type integer NOT NULL,
   initial_balance decimal NOT NULL,
   CONSTRAINT pk_account primary key(id),
   CONSTRAINT fk_account_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_account_person FOREIGN KEY (person_id) references person(person_id),
   CONSTRAINT fk_account_institution FOREIGN KEY (institution_id) references institution(id)
);

# --- !Downs

DROP TABLE account;
DROP TABLE institution;