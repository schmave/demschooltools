# --- !Ups

ALTER TABLE donation ALTER COLUMN DATE SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_tag_change ALTER COLUMN time SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_change ALTER COLUMN "time" SET DEFAULT (now() at time zone 'utc');
ALTER TABLE comments ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');

# --- !Downs

ALTER TABLE donation ALTER COLUMN DATE SET DEFAULT now();
ALTER TABLE person_tag_change ALTER COLUMN time SET DEFAULT now();
ALTER TABLE person_change ALTER COLUMN "time" SET DEFAULT now();
ALTER TABLE comments ALTER COLUMN created SET DEFAULT now();
ALTER TABLE person ALTER COLUMN created SET DEFAULT now();

