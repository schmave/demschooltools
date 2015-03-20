select
      stu.student_id 
     , stu.student_id as _id
     , (select s.name from students s where s._id = stu.student_id) as name
     , sum(CASE WHEN oid IS NOT NULL THEN 300/60 ELSE stu.intervalmin/60 END) as total_hours
     , sum(CASE WHEN oid IS NOT NULL OR stu.intervalmin >= 300 THEN 1 ELSE 0 END) as good
     , sum(CASE WHEN oid IS NOT NULL OR stu.intervalmin >= 300 THEN 0 ELSE 1 END) as short
     , sum(CASE WHEN oid IS NOT NULL THEN 1 ELSE 0 END) as overrides
     , sum(CASE WHEN stu.intervalmin is null or stu.intervalmin = 0 THEN 1 ELSE 0 END) as absent
from (select 
        st._id
        , o._id oid
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) as intervalmin
         , schooldays.days as day

      from swipes s
      join student st on (st._id = s.student_id)
      full outer join overrides o on (date(s.in_time at time zone 'America/New_York') 
                                       = date(o.date at time zone 'America/New_York') 
                                     and o.student_id = s.student_id)
      right outer join 
        (select distinct days2.days
            from (select
                   date(s.in_time at time zone 'America/New_York') as days
                    from swipes s
                    inner join years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    where y.name= '2014-06-01 2015-06-01' ) days2
            order by days2.days) as schooldays 
                on (schooldays.days = date(s.in_time at time zone 'America/New_York')) 
      and s.student_id = 8
      group by  s.student_id, day,oid) as stu
where student_id = 8
group by stu.student_id;



select * from students st on (st._id = s.student_id)
select 
        st._id
        , o._id oid
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) as intervalmin
         , schooldays.days as day

      from swipes s
      full outer join overrides o on (date(s.in_time at time zone 'America/New_York') 
                                       = date(o.date at time zone 'America/New_York') 
                                     and o.student_id = s.student_id)
      right outer join 
        (select distinct days2.days
            from (select
                   date(s.in_time at time zone 'America/New_York') as days
                    from swipes s
                    inner join years y 
                      ON ((s.out_time BETWEEN y.from_date AND y.to_date)
                          OR (s.in_time BETWEEN y.from_date AND y.to_date))
                    where y.name= '2014-06-01 2015-06-01' ) days2
            order by days2.days) as schooldays 
                on (schooldays.days = date(s.in_time at time zone 'America/New_York')) 
      and s.student_id = 8
      group by  st._id, day,oid;
