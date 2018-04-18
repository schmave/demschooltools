# --- !Ups

ALTER TABLE account ADD COLUMN monthly_credit decimal NOT NULL DEFAULT(0);

# --- !Downs

ALTER TABLE account DROP COLUMN monthly_credit;