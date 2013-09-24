# --- !Ups

CREATE TABLE "case"
  (
   case_number VARCHAR(255) NOT NULL DEFAULT '',
   findings text,
   DATE DATE,
   constraint pk_case primary key (case_number)
  );

CREATE TABLE rule
   (
    id serial,
    title VARCHAR(255) NOT NULL DEFAULT '',
    constraint pk_rule primary key(id)
    );

CREATE TABLE charge
   (
    id serial,
    case_id VARCHAR(255) NOT NULL DEFAULT '',
    person_id int,
    rule_id int,
    plea VARCHAR(255) NOT NULL DEFAULT '',
    resolution_plan text,
    constraint pk_charge primary key (id),
    constraint fk_charge_person foreign key (person_id) references person(person_id),
    constraint fk_charge_rule foreign key (rule_id) references rule(id)
    );


# --- !Downs

DROP TABLE "case";
DROP TABLE charge;
DROP TABLE rule;
