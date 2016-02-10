# --- !Ups

ALTER TABLE charge ADD COLUMN referred_to_sm BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN sm_decision text;

# --- !Downs

ALTER TABLE charge DROP COLUMN referred_to_sm;
ALTER TABLE charge DROP COLUMN sm_decision;
