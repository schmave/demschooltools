# --- !Ups

CREATE TABLE notification_rule (
   id serial,
   tag_id INTEGER,
   the_type INTEGER NOT NULL,
   email text NOT NULL,
   organization_id INTEGER NOT NULL,
   CONSTRAINT pk_notification_rule PRIMARY KEY(id),
   CONSTRAINT fk_notifiaction_rule_tag FOREIGN KEY(tag_id) references tag(id),
   CONSTRAINT fk_notifiaction_rule_organization FOREIGN KEY(organization_id) references organization(id)
);

# --- !Downs

DROP TABLE notification_rule;
