
SELECT
  stu.student_id
    , stu.student_id as _id
    , (select s.name from students s WHERE s._id = stu.student_id) as name
    , sum(CASE WHEN oid IS NOT NULL THEN stu.requiredmin/60 ELSE stu.intervalmin/60 END) as total_hours
    , sum(CASE WHEN oid IS NOT NULL
               OR stu.intervalmin >= stu.requiredmin
          THEN 1 ELSE 0 END) as good
    , sum(CASE WHEN oid IS NULL
                AND eid IS NULL
                AND (stu.intervalmin < stu.requiredmin
                    OR stu.intervalmin IS NULL)
                AND stu.anyswipes IS NOT NULL
                THEN 1 ELSE 0 END) as short
    , sum(CASE WHEN oid IS NOT NULL THEN 1 ELSE 0 END) as overrides
    , sum(CASE WHEN eid IS NOT NULL THEN 1 ELSE 0 END) as excuses
    , sum(CASE WHEN anyswipes IS NULL
                   AND eid IS NULL
                   AND oid IS NULL
           THEN 1 ELSE 0 END) as unexcused
FROM (
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

      FROM school_days('2014-06-10 2016-10-04', 1) as schooldays
      LEFT JOIN roundedswipes s
                ON (schooldays.days = date(s.in_time AT TIME ZONE 'America/New_York')
                    AND schooldays.student_id = s.student_id)
      LEFT JOIN overrides o
           ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN excuses e
           ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
      WHERE schooldays.days IS NOT NULL
      AND schooldays.student_id = 8
      GROUP BY schooldays.student_id, day, schooldays.olderdate
  ) AS stu
GROUP BY stu.student_id;
