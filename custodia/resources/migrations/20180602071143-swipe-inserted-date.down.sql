ALTER TABLE overseer.swipes add column inserted_date timestamp without time zone default null;
--;;
update overseer.swipes set inserted_date=in_time at time zone 'utc';
--;;
ALTER TABLE overseer.swipes alter column inserted_date set default now();
