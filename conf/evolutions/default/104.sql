# --- !Ups

alter table manual_change add column effective_date date NULL default NULL;
alter table manual_change add column user_id int default NULL;

alter table manual_change add constraint fk_user_id foreign key (user_id) references users (id) on delete restrict on update restrict;


# --- !Downs

alter table manual_change drop constraint fk_user_id;
alter table manual_change drop column user_id;
alter table manual_change drop column effective_date;
