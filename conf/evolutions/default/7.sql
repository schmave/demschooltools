# --- !Ups

ALTER TABLE person ALTER person_id SET DEFAULT nextval('person_seq');
ALTER TABLE person ALTER is_family SET DEFAULT FALSE;
UPDATE person SET is_family=FALSE WHERE is_family IS NULL;
ALTER TABLE person ALTER is_family SET NOT NULL;

ALTER TABLE comments ALTER id SET DEFAULT nextval('comments_seq');

ALTER TABLE tag ALTER id SET DEFAULT nextval('tag_seq');

# --- !Downs

ALTER TABLE person ALTER person_id DROP DEFAULT;
ALTER TABLE comments ALTER id DROP DEFAULT;
ALTER TABLE tag ALTER id DROP DEFAULT;

