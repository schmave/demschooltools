# --- !Ups

ALTER TABLE meeting ADD CONSTRAINT unq_org_date UNIQUE (organization_id, date);

# --- !Downs

ALTER TABLE meeting DROP CONSTRAINT unq_org_date;
