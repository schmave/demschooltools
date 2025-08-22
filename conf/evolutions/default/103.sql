
# --- !Ups



alter table manual_change alter column date_entered type timestamptz using date_entered at time zone 'UTC';

# --- !Downs

alter table manual_change alter column date_entered type timestamp using date_entered at time zone 'UTC';
