
# --- !Ups

-- Run this to delete duplicates if needed

-- DELETE FROM custodia_swipe a USING (
--     SELECT MIN(ctid) as ctid, person_id, swipe_day
--     FROM custodia_swipe
--     WHERE out_time is null
--     GROUP BY person_id, swipe_day HAVING COUNT(*) > 1
-- ) b
-- WHERE a.person_id = b.person_id and a.swipe_day=b.swipe_day and out_time is null
-- AND a.ctid <> b.ctid;


create unique index person_swipe_day_empty_out_unique on custodia_swipe(person_id, swipe_day) WHERE out_time is null;



# --- !Downs

drop index person_swipe_day_empty_out_unique;
