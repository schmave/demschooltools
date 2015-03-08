# --- !Ups

ALTER TABLE organization ADD COLUMN mailchimp_last_sync_person_changes timestamp without time zone;
ALTER TABLE organization ALTER COLUMN mailchimp_api_key SET DEFAULT '';
update organization set mailchimp_api_key='' where mailchimp_api_key is null;
ALTER TABLE organization ALTER COLUMN mailchimp_api_key SET NOT NULL;

CREATE TABLE person_change
(
   person_id INTEGER NOT NULL,
   old_email VARCHAR(255) NOT NULL,
   new_email VARCHAR(255) NOT NULL,
   "time" timestamp without time zone DEFAULT now(),

   constraint fk_person_change_person foreign key(person_id) references person(person_id)
);


# --- !Downs

DROP TABLE person_change;

ALTER TABLE organization DROP mailchimp_last_sync_person_changes;