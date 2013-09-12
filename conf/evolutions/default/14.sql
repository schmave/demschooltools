# --- !Ups

alter table task add column enabled boolean default true;

# --- !Downs

alter table task drop column enabled;
