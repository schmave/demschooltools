# --- !Ups

alter table linked_account alter column id set default nextval('linked_account_seq'::regclass);
alter table users alter column id set default nextval('users_seq'::regclass);

# --- !Downs

alter table linked_account alter column id DROP DEFAULT;
alter table users alter column id drop default;
