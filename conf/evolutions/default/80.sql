# --- !Ups
ALTER TABLE person ADD COLUMN pin VARCHAR(10) NOT NULL DEFAULT('');

# --- !Downs

ALTER TABLE person DROP COLUMN pin;
