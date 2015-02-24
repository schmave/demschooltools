# --- !Ups

CREATE TABLE mailchimp_sync
(
   id serial NOT NULL,
   tag_id INTEGER NOT NULL,
   mailchimp_list_id varchar(255) NOT NULL,
   sync_local_adds BOOLEAN NOT NULL,
   sync_local_removes BOOLEAN NOT NULL,
   last_sync timestamp without time zone,

   constraint pk_mailchimp_sync primary key(id),
   constraint fk_mailchimp_sync_tag foreign key(tag_id) references tag(id)
);

ALTER TABLE organization ADD COLUMN mailchimp_api_key VARCHAR(255);

# --- !Downs

DROP TABLE mailchimp_sync;

ALTER TABLE organization DROP mailchimp_api_key;
