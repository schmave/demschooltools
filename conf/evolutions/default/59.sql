# --- !Ups

alter table linked_account add constraint u_linked_account UNIQUE(provider_key, provider_user_id);

# --- !Downs

alter table linked_account drop constraint u_linked_account;
