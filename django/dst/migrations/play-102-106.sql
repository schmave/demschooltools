
--- 102.sql

create unique index person_swipe_day_empty_out_unique on custodia_swipe(person_id, swipe_day) WHERE out_time is null;

--- 103.sql

alter table manual_change alter column date_entered type timestamptz using date_entered at time zone 'UTC';

--- 104.sql

alter table manual_change add column effective_date date NULL default NULL;
alter table manual_change add column user_id int default NULL;
alter table manual_change add constraint fk_user_id foreign key (user_id) references users (id) on delete restrict on update restrict;

--- 105.sql

alter table manual_change add column show_date_in_history boolean default true NOT NULL;

--- 106.sql

DROP INDEX idx_entry_index;
DROP MATERIALIZED VIEW entry_index;
