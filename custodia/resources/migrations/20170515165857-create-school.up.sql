create table overseer.schools(
_id bigserial primary key,
name varchar(255),
timezone varchar(255) default  'America/New_York',
inserted_date timestamp default now()
);

--;;

INSERT INTO overseer.schools (_id, name, timezone)
VALUES (1, 'Philly Free School',   'America/New_York'),
       (2, 'Demo School',   'America/New_York');

