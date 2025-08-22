# --- !Ups

alter table manual_change add column show_date_in_history boolean default true NOT NULL;


# --- !Downs

alter table manual_change drop column show_date_in_history;
