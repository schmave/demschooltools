ALTER TABLE phillyfreeschool.swipes
      ADD COLUMN intervalmin NUMERIC DEFAULT(0);
--;;
UPDATE phillyfreeschool.swipes s1
SET intervalmin = (select sum(extract(EPOCH FROM (rounded_out_time - rounded_in_time)::INTERVAL)/60) as intervalmin from phillyfreeschool.swipes s2 where s1._id = s2._id);
--;;
ALTER TABLE demo.swipes
      ADD COLUMN intervalmin NUMERIC DEFAULT(0);
--;;
UPDATE demo.swipes s1
SET intervalmin = (select sum(extract(EPOCH FROM (rounded_out_time - rounded_in_time)::INTERVAL)/60) as intervalmin from demo.swipes s2 where s1._id = s2._id);
