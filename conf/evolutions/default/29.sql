# --- !Ups

alter table charge drop CONSTRAINT fk_charge_rule;
delete from charge;
alter table charge add CONSTRAINT fk_charge_entry foreign key(rule_id) references entry (id) on update restrict on delete restrict;

drop table rule;

# --- !Downs

CREATE TABLE rule
(
  id serial NOT NULL,
  title character varying(255) NOT NULL DEFAULT ''::character varying,
  removed boolean NOT NULL DEFAULT false,
  CONSTRAINT pk_rule PRIMARY KEY (id)
);

alter table charge drop CONSTRAINT fk_charge_entry;
alter table charge add CONSTRAINT fk_charge_rule FOREIGN KEY (rule_id)
      REFERENCES rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;
