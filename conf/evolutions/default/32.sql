# --- !Ups

ALTER TABLE tag DROP constraint unique_title;
ALTER TABLE tag ADD CONSTRAINT unique_title_org UNIQUE (title, organization_id);

# --- !Downs

ALTER TABLE tag remove constraint unique_title_org;
ALTER TABLE tag ADD CONSTRAINT unique_title UNIQUE (title);


