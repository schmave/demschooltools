# --- !Ups

ALTER TABLE organization ADD COLUMN mailchimp_updates_email varchar(255) DEFAULT '' NOT NULL;


# --- !Downs

ALTER TABLE organization DROP mailchimp_updates_email;