# --- !Ups

ALTER TABLE organization ADD COLUMN enable_structured_res_plans boolean NOT NULL DEFAULT(false);

ALTER TABLE charge ADD COLUMN referenced_charge_id integer;
ALTER TABLE charge ADD CONSTRAINT fk_charge_charge FOREIGN KEY (referenced_charge_id) REFERENCES charge(id);

ALTER TABLE charge ADD COLUMN rp_progress integer NOT NULL DEFAULT(0);
ALTER TABLE charge ADD COLUMN rp_type integer NOT NULL DEFAULT(0);
ALTER TABLE charge ADD COLUMN rp_max_days integer DEFAULT(1);
ALTER TABLE charge ADD COLUMN rp_start_immediately boolean NOT NULL DEFAULT(false);
ALTER TABLE charge ADD COLUMN rp_escape_clause text NOT NULL DEFAULT('');
ALTER TABLE charge ADD COLUMN rp_text text NOT NULL DEFAULT('');

CREATE TABLE case_reference (
	referencing_case integer NOT NULL,
	referenced_case integer NOT NULL,
	CONSTRAINT fk_referencing_case FOREIGN KEY (referencing_case) references "case"(id),
	CONSTRAINT fk_referenced_case FOREIGN KEY (referenced_case) references "case"(id)
);

# --- !Downs

DROP TABLE case_reference;

ALTER TABLE charge DROP COLUMN rp_text;
ALTER TABLE charge DROP COLUMN rp_escape_clause;
ALTER TABLE charge DROP COLUMN rp_start_immediately;
ALTER TABLE charge DROP COLUMN rp_max_days;
ALTER TABLE charge DROP COLUMN rp_type;
ALTER TABLE charge DROP COLUMN rp_progress;

ALTER TABLE charge DROP CONSTRAINT fk_charge_charge;
ALTER TABLE charge DROP COLUMN referenced_charge_id;

ALTER TABLE organization DROP COLUMN enable_structured_res_plans;