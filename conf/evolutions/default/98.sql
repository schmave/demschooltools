# --- !Ups

ALTER TABLE organization ADD COLUMN show_roles boolean NOT NULL DEFAULT false;
ALTER TABLE organization ADD COLUMN roles_individual_term text DEFAULT 'Clerk';
ALTER TABLE organization ADD COLUMN roles_committee_term text DEFAULT 'Committee';
ALTER TABLE organization ADD COLUMN roles_group_term text DEFAULT 'Group';

ALTER TABLE tag ADD COLUMN show_in_roles boolean NOT NULL DEFAULT true;

CREATE TABLE role (
   id serial,
   organization_id integer NOT NULL,
   is_active boolean NOT NULL DEFAULT true,
   type integer NOT NULL,
   eligibility integer NOT NULL,
   name text NOT NULL,
   notes text NOT NULL,
   description text NOT NULL,
   CONSTRAINT pk_role primary key(id),
   CONSTRAINT fk_role_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
);

CREATE TABLE role_record (
   id serial,
   role_id integer NOT NULL,
   role_name text NOT NULL,
   date_created timestamp NOT NULL,
   CONSTRAINT pk_role_record primary key(id),
   CONSTRAINT fk_role_record_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE role_record_member (
	record_id integer NOT NULL,
	person_id integer,
	person_name text,
	type integer NOT NULL,
	CONSTRAINT fk_role_record_member_record FOREIGN KEY (record_id) REFERENCES role_record(id),
	CONSTRAINT fk_role_record_member_person FOREIGN KEY (person_id) REFERENCES person(person_id)
);

# --- !Downs

DROP TABLE role_record_member;
DROP TABLE role_record;
DROP TABLE role;

ALTER TABLE organization DROP COLUMN roles_individual_term;
ALTER TABLE organization DROP COLUMN roles_committee_term;
ALTER TABLE organization DROP COLUMN roles_group_term;
ALTER TABLE organization DROP COLUMN show_roles;

ALTER TABLE tag DROP COLUMN show_in_roles;