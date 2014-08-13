# --- !Ups

alter table section alter num type varchar(8);
alter table entry alter num type varchar(8);

# --- !Downs

alter table section alter num type int;
alter table entry alter num type int;
