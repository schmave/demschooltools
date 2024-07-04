ALTER TABLE phillyfreeschool.classes
ADD COLUMN from_date timestamp  with time zone;

--;;

ALTER TABLE phillyfreeschool.classes
ADD COLUMN to_date timestamp  with time zone;

--;;

ALTER TABLE demo.classes
ADD COLUMN from_date timestamp  with time zone;

--;;

ALTER TABLE demo.classes
ADD COLUMN to_date timestamp  with time zone;

