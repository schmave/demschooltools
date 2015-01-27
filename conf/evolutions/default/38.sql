# --- !Ups

ALTER TABLE "charge" ADD COLUMN minor_referral_destination VARCHAR(255) NOT NULL DEFAULT '';

# --- !Downs

ALTER TABLE "charge" DROP COLUMN minor_referral_destination;

