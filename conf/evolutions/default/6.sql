# --- !Ups

ALTER TABLE users ADD constraint users_unique_email_1 UNIQUE(email);

# --- !Downs

ALTER TABLE users DROP constraint users_unique_email_1;
