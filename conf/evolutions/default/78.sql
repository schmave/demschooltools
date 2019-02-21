# --- !Ups

ALTER TABLE organization ADD COLUMN enable_case_references boolean NOT NULL DEFAULT(false);

ALTER TABLE entry ADD COLUMN is_breaking_res_plan boolean NOT NULL DEFAULT(false);

ALTER TABLE charge ADD COLUMN referenced_charge_id integer;
ALTER TABLE charge ADD CONSTRAINT fk_charge_charge FOREIGN KEY (referenced_charge_id) REFERENCES charge(id);

CREATE TABLE case_reference (
	referencing_case integer NOT NULL,
	referenced_case integer NOT NULL,
	CONSTRAINT fk_referencing_case FOREIGN KEY (referencing_case) REFERENCES "case"(id),
	CONSTRAINT fk_referenced_case FOREIGN KEY (referenced_case) REFERENCES "case"(id)
);

CREATE TABLE charge_reference (
	referencing_case integer NOT NULL,
	referenced_charge integer NOT NULL,
	CONSTRAINT fk_referencing_case FOREIGN KEY (referencing_case) REFERENCES "case"(id),
	CONSTRAINT fk_referenced_charge FOREIGN KEY (referenced_charge) REFERENCES charge(id)
);

# --- !Downs

DROP TABLE charge_reference;
DROP TABLE case_reference;

ALTER TABLE charge DROP CONSTRAINT fk_charge_charge;
ALTER TABLE charge DROP COLUMN referenced_charge_id;

ALTER TABLE entry DROP COLUMN is_breaking_res_plan;

ALTER TABLE organization DROP COLUMN enable_case_references;