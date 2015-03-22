select
      stu.student_id 
     , stu.student_id as _id
     , (select s.name from students s where s._id = stu.student_id) as name
     , sum(CASE WHEN oid IS NOT NULL THEN 300/60 ELSE stu.intervalmin/60 END) as total_hours
     , sum(CASE WHEN oid IS NOT NULL 
                OR stu.intervalmin >= 300 
           THEN 1 ELSE 0 END) as good
     , sum(CASE WHEN (oid IS NULL 
                      AND eid IS NULL)
               AND (stu.intervalmin < 300
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





                    WHERE y.name=  '2014-06-01 2015-06-01') days2
      where schooldays.student_id = 11

-- se-- lect * from students ;
-- alter table students alter column olderdate type date using olderdate::date;
