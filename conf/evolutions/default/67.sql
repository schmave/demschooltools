# --- !Ups

alter table organization add column show_accounting boolean not null default false;

# --- !Downs

alter table organization drop column show_accounting;
