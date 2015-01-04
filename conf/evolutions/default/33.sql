  first_name character varying(255),
  last_name character varying(255),
  notes text,
  address character varying(255),
  city character varying(255),
  state character varying(255),
  zip character varying(255),
  neighborhood character varying(255),
  email character varying(255),

# --- !Ups

UPDATE person SET last_name='' WHERE last_name IS NULL;
ALTER TABLE person ALTER COLUMN last_name SET DEFAULT '';
ALTER TABLE person ALTER COLUMN last_name SET NOT NULL;

UPDATE person SET first_name='' WHERE first_name IS NULL;
ALTER TABLE person ALTER COLUMN first_name SET DEFAULT '';
ALTER TABLE person ALTER COLUMN first_name SET NOT NULL;

UPDATE person SET notes='' WHERE notes IS NULL;
ALTER TABLE person ALTER COLUMN notes SET DEFAULT '';
ALTER TABLE person ALTER COLUMN notes SET NOT NULL;

UPDATE person SET address='' WHERE address IS NULL;
ALTER TABLE person ALTER COLUMN address SET DEFAULT '';
ALTER TABLE person ALTER COLUMN address SET NOT NULL;

UPDATE person SET city='' WHERE city IS NULL;
ALTER TABLE person ALTER COLUMN city SET DEFAULT '';
ALTER TABLE person ALTER COLUMN city SET NOT NULL;

UPDATE person SET state='' WHERE state IS NULL;
ALTER TABLE person ALTER COLUMN state SET DEFAULT '';
ALTER TABLE person ALTER COLUMN state SET NOT NULL;

UPDATE person SET zip='' WHERE zip IS NULL;
ALTER TABLE person ALTER COLUMN zip SET DEFAULT '';
ALTER TABLE person ALTER COLUMN zip SET NOT NULL;

UPDATE person SET neighborhood='' WHERE neighborhood IS NULL;
ALTER TABLE person ALTER COLUMN neighborhood SET DEFAULT '';
ALTER TABLE person ALTER COLUMN neighborhood SET NOT NULL;

UPDATE person SET email='' WHERE email IS NULL;
ALTER TABLE person ALTER COLUMN email SET DEFAULT '';
ALTER TABLE person ALTER COLUMN email SET NOT NULL;

# --- !Downs

ALTER TABLE person ALTER COLUMN last_name DROP DEFAULT;
ALTER TABLE person ALTER COLUMN last_name DROP NOT NULL;

ALTER TABLE person ALTER COLUMN first_name drop DEFAULT;
ALTER TABLE person ALTER COLUMN first_name drop NOT NULL;

ALTER TABLE person ALTER COLUMN notes drop DEFAULT;
ALTER TABLE person ALTER COLUMN notes drop NOT NULL;

ALTER TABLE person ALTER COLUMN address drop DEFAULT;
ALTER TABLE person ALTER COLUMN address drop NOT NULL;

ALTER TABLE person ALTER COLUMN city drop DEFAULT;
ALTER TABLE person ALTER COLUMN city drop NOT NULL;

ALTER TABLE person ALTER COLUMN state drop DEFAULT;
ALTER TABLE person ALTER COLUMN state drop NOT NULL;

ALTER TABLE person ALTER COLUMN zip drop DEFAULT;
ALTER TABLE person ALTER COLUMN zip drop NOT NULL;

ALTER TABLE person ALTER COLUMN neighborhood drop DEFAULT;
ALTER TABLE person ALTER COLUMN neighborhood drop NOT NULL;

ALTER TABLE person ALTER COLUMN email drop DEFAULT;
ALTER TABLE person ALTER COLUMN email drop NOT NULL;

