# --- !Ups

ALTER TABLE ENTRY ALTER num TYPE text;
ALTER TABLE section ALTER num TYPE text;
ALTER TABLE chapter ALTER num TYPE text;

ALTER TABLE manual_change ALTER old_num TYPE text;
ALTER TABLE manual_change ALTER new_num TYPE text;

ALTER TABLE person ALTER gender TYPE text;
ALTER TABLE person ALTER grade TYPE text;

# --- !Downs

ALTER TABLE ENTRY ALTER num TYPE VARCHAR(8);
ALTER TABLE section ALTER num TYPE VARCHAR(8);
ALTER TABLE chapter ALTER num TYPE VARCHAR(8);
ALTER TABLE manual_change ALTER old_num TYPE VARCHAR(8);
ALTER TABLE manual_change ALTER new_num TYPE VARCHAR(8);

ALTER TABLE person ALTER gender TYPE VARCHAR(10);
ALTER TABLE person ALTER grade TYPE VARCHAR(8);