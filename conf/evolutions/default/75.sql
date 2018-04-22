# --- !Ups

ALTER TABLE account ADD COLUMN date_last_monthly_credit timestamp;

# --- !Downs

ALTER TABLE account DROP COLUMN date_last_monthly_credit;