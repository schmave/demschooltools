# --- !Ups

alter table phone_numbers alter column id set default nextval('phone_numbers_seq'::regclass);

# --- !Downs

alter table phone_numbers alter column id DROP DEFAULT;
