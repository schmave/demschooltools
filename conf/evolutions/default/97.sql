# --- !Ups

ALTER TABLE tag ALTER COLUMN show_in_account_balances SET DEFAULT false;
ALTER TABLE tag ALTER COLUMN show_in_account_balances SET NOT NULL;

# --- !Downs

ALTER TABLE tag ALTER COLUMN show_in_account_balances DROP NOT NULL;
ALTER TABLE tag ALTER COLUMN show_in_account_balances DROP DEFAULT;
