# --- !Ups

ALTER TABLE account ADD COLUMN name text NOT NULL DEFAULT '';

ALTER TABLE account DROP CONSTRAINT fk_account_institution;
ALTER TABLE account DROP COLUMN institution_id;

DROP TABLE institution;

# --- !Downs

CREATE TABLE institution (
   id serial,
   organization_id integer NOT NULL,
   name text NOT NULL,
   type integer NOT NULL,
   CONSTRAINT pk_institution primary key(id),
   CONSTRAINT fk_institution_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
);

ALTER TABLE account ADD COLUMN institution_id integer;
ALTER TABLE account ADD CONSTRAINT fk_account_institution FOREIGN KEY (institution_id) REFERENCES institution(id)

ALTER TABLE account DROP COLUMN name;