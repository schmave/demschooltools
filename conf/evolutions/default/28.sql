# --- !Ups

alter table chapter add column deleted boolean not null default false;
alter table section add column deleted boolean not null default false;
alter table entry add column deleted boolean not null default false;

# --- !Downs

alter table chapter drop column deleted;
alter table section drop column deleted;
alter table entry drop column deleted;
