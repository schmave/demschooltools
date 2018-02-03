# --- !Ups

ALTER TABLE tag ADD COLUMN show_in_account_balances boolean;
UPDATE tag SET show_in_account_balances = show_in_jc;

# --- !Downs

ALTER TABLE tag DROP COLUMN show_in_account_balances;