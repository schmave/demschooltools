select
      stu.student_id 
     , stu.student_id as _id
     , (select s.name from students s where s._id = stu.student_id) as name
     , sum(CASE WHEN oid IS NOT NULL THEN 300/60 ELSE stu.intervalmin/60 END) as total_hours
     , sum(CASE WHEN oid IS NOT NULL 
                OR stu.intervalmin >= 300 
           THEN 1 ELSE 0 END) as good
     , sum(CASE WHEN oid IS NOT NULL 
                OR eid IS NOT NULL
               OR stu.intervalmin >= 300 
           THEN 0 ELSE 1 END) as short
     , sum(CASE WHEN oid IS NOT NULL THEN 1 ELSE 0 END) as overrides
     , sum(CASE WHEN eid IS NOT NULL THEN 1 ELSE 0 END) as excuses
     , sum(CASE WHEN (stu.intervalmin IS NULl 
                     OR stu.intervalmin = 0)
                    AND eid IS NULL
            THEN 1 ELSE 0 END) as absent
from (
      SELECT 
        schooldays.student_id
        , o._id oid
        , e._id eid
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) AS intervalmin
         , schooldays.days AS day

      FROM (SELECT a.days, students._id student_id FROM (SELECT DISTINCT days2.days
            FROM (SELECT
                   date(s.in_time at time zone 'America/New_York') as days
                    FROM swipes s
                    INNER JOIN years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    WHERE y.name= '2014-06-01 2015-06-01' ) days2
            ORDER BY days2.days) as a
            JOIN students on (1=1)) as schooldays

      LEFT JOIN swipes s
                ON (schooldays.days = date(s.in_time at time zone 'America/New_York')
                    AND schooldays.student_id = s.student_id) 
      LEFT JOIN overrides o 
           ON (schooldays.days = date(o.date at time zone 'America/New_York') 
                       AND o.student_id = s.student_id)
      LEFT JOIN excuses e 
           ON (schooldays.days = date(e.date at time zone 'America/New_York') 
                       AND e.student_id = s.student_id)
      AND schooldays.student_id = 8
      GROUP BY schooldays.student_id , day,oid,eid
) as stu
where stu.student_id = 8
group by stu.student_id;

(select * from (select distinct days2.days
            from (select
                   date(s.in_time at time zone 'America/New_York') as days
                    from swipes s
                    inner join years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    where y.name= '2014-06-01 2015-06-01' ) days2
            order by days2.days) as a
join students on (1=1)
where students._id = 8)

SELECT 
        schooldays.student_id
        , o._id oid
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) AS intervalmin
         , schooldays.days AS day

      FROM (SELECT a.days, students._id student_id FROM (SELECT DISTINCT days2.days
            FROM (SELECT
                   date(s.in_time at time zone 'America/New_York') as days
                    FROM swipes s
                    INNER JOIN years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    WHERE y.name= '2014-06-01 2015-06-01' ) days2
            ORDER BY days2.days) as a
            JOIN students on (1=1)) as schooldays

      LEFT JOIN swipes s
                ON (schooldays.days = date(s.in_time at time zone 'America/New_York')
                    AND schooldays.student_id = s.student_id) 
      LEFT JOIN overrides o 
           ON (schooldays.days = date(o.date at time zone 'America/New_York') 
                       AND o.student_id = s.student_id)
      AND schooldays.student_id = 8
      GROUP BY schooldays.student_id , day,oid;
