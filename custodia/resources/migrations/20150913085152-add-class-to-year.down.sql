DROP FUNCTION school_days(text);

--;;

CREATE OR REPLACE FUNCTION school_days(year_name text)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$
SELECT a.days, s._id student_id, s.archived, s.olderdate
FROM (SELECT DISTINCT days2.days
      FROM (SELECT
            (CASE WHEN date(s.in_time AT TIME ZONE 'America/New_York')  IS NULL
            THEN date(s.out_time AT TIME ZONE 'America/New_York')
            ELSE date(s.in_time AT TIME ZONE 'America/New_York') END) AS days
         FROM roundedswipes s
         INNER JOIN years y
            ON ((s.out_time BETWEEN y.from_date AND y.to_date)
            OR (s.in_time BETWEEN y.from_date AND y.to_date))
         WHERE y.name = $1) days2
    ORDER BY days2.days) AS a
JOIN years y ON (1=1)
$func$
LANGUAGE sql;

--;;

ALTER TABLE years
DROP COLUMN class_id;
