# --- !Ups

ALTER TABLE charge ADD COLUMN sm_decision_date date;

# --- !Downs

ALTER TABLE charge DROP COLUMN sm_decision_date;
