ALTER TABLE phillyfreeschool.swipes
ADD COLUMN rounded_out_time timestamp  with time zone;

--;;

ALTER TABLE phillyfreeschool.swipes
ADD COLUMN rounded_in_time timestamp  with time zone;

--;;

UPDATE phillyfreeschool.swipes SET rounded_in_time = in_time;

--;;

UPDATE phillyfreeschool.swipes SET rounded_out_time = out_time;

--;;

ALTER TABLE demo.swipes
ADD COLUMN rounded_out_time timestamp  with time zone;

--;;

ALTER TABLE demo.swipes
ADD COLUMN rounded_in_time timestamp  with time zone;

--;;

UPDATE demo.swipes SET rounded_in_time = in_time;

--;;

UPDATE demo.swipes SET rounded_out_time = out_time;
