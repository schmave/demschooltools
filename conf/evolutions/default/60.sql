# --- !Ups

ALTER TABLE organization ADD COLUMN printer_email varchar(255) DEFAULT '' NOT NULL;


# --- !Downs

ALTER TABLE organization DROP printer_email;
