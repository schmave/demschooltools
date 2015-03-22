select
      stu.student_id 
     , stu.student_id as _id
     , (select s.name from students s where s._id = stu.student_id) as name
     , sum(CASE WHEN oid IS NOT NULL THEN stu.requiredmin/60 ELSE stu.intervalmin/60 END) as total_hours
     , sum(CASE WHEN oid IS NOT NULL 
                OR stu.intervalmin >= stu.requiredmin
           THEN 1 ELSE 0 END) as good
     , sum(CASE WHEN (oid IS NULL 
                      AND eid IS NULL)
               AND (stu.intervalmin < stu.requiredmin 
                    AND stu.intervalmin IS NOT NULL)
           THEN 1 ELSE 0 END) as short
     , sum(CASE WHEN oid IS NOT NULL THEN 1 ELSE 0 END) as overrides
     , sum(CASE WHEN eid IS NOT NULL THEN 1 ELSE 0 END) as excuses
     , sum(CASE WHEN anyswipes IS NULL 
                    AND eid IS NULL
                    AND oid IS NULL
            THEN 1 ELSE 0 END) as unexcused
from (
      SELECT 
        schooldays.student_id
        , max(s._id) anyswipes
        , max(o._id) oid
        , max(e._id) eid
        , schooldays.olderdate
        , (CASE WHEN schooldays.olderdate IS NULL 
                     OR schooldays.olderdate > schooldays.days
                     THEN 300 ELSE 330 END) as requiredmin
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) AS intervalmin
         , schooldays.days AS day

      FROM (SELECT a.days, students._id student_id, students.olderdate FROM (SELECT DISTINCT days2.days
            FROM (SELECT
                   date(s.in_time at time zone 'America/New_York') as days
                    FROM swipes s
                    INNER JOIN years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    WHERE y.name='2014-06-01 2015-06-01') days2
            ORDER BY days2.days) as a
            JOIN students on (1=1)) as schooldays

      LEFT JOIN swipes s
                ON (schooldays.days = date(s.in_time at time zone 'America/New_York')
                    AND schooldays.student_id = s.student_id) 
      LEFT JOIN overrides o 
           ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN excuses e 
           ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
      where schooldays.student_id = 11
      GROUP BY schooldays.student_id, day, schooldays.olderdate
) as stu
group by stu.student_id;



-- WHERE y.name=  '2014-06-01 2015-06-01') days2
-- where schooldays.student_id = 11

-- alter table students alter column olderdate type date using olderdate::date;
-- update students set olderdate = '2015-03-18' where _id = 10;

-- School Migration 

--      Step 1 - olderdate to date
-- alter table students alter column olderdate type date using olderdate::date;
-- select * from students ;

--      Step 2 - year dates

-- select * from years ;

-- alter table years 
-- alter column from_date type timestamp with time zone
-- using from_date::timestamp with time zone ;

-- alter table years 
-- alter column to_date type timestamp with time zone
-- using to_date::timestamp with time zone ;

-- select column_name, data_type, character_maximum_length
-- from INFORMATION_SCHEMA.COLUMNS where table_name = 'years';

--       Step 3 - Swipes

-- select column_name, data_type, character_maximum_length
-- from INFORMATION_SCHEMA.COLUMNS where table_name = 'swipes';

-- alter table swipes 
-- alter column out_time type timestamp with time zone
-- using out_time::timestamp with time zone ;

-- alter table swipes 
-- alter column in_time type timestamp with time zone
-- using in_time::timestamp with time zone ;


-- select * from swipes where _id = 515 ;

--       Step 4 - Overrides

-- select column_name, data_type, character_maximum_length
-- from INFORMATION_SCHEMA.COLUMNS where table_name = 'overrides';

-- alter table overrides 
-- alter column date type date
-- using date::date; 

-- select * from overrides;

--       Step 5 - Excuses

-- select column_name, data_type, character_maximum_length
-- from INFORMATION_SCHEMA.COLUMNS where table_name = 'excuses';

-- alter table excuses 
-- alter column date type date
-- using date::date; 

-- select * from excuses;
