
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
                    WHERE y.name=?) days2
            ORDER BY days2.days) as a
            JOIN students on (1=1)) as schooldays
      LEFT JOIN swipes s
                ON (schooldays.days = date(s.in_time at time zone 'America/New_York')
                    AND schooldays.student_id = s.student_id) 
      LEFT JOIN overrides o 
           ON (schooldays.days = o.date AND o.student_id = schooldays.student_id)
      LEFT JOIN excuses e 
           ON (schooldays.days = e.date AND e.student_id = schooldays.student_id)
      where schooldays.days is not null
      GROUP BY schooldays.student_id, day, schooldays.olderdate
) as stu
group by stu.student_id;



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
      where schooldays.days is not null
      and schooldays.student_id = 11
      GROUP BY schooldays.student_id, day, schooldays.olderdate ;



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

-- Aliasing names
-- update students set name='Arathilion Boom' where _id=1;
-- update students set name='John Darran' where _id=2;
-- update students set name='Jol Carlstein' where _id=3;
-- update students set name='Galak Daklan' where _id=4;
-- update students set name='Brodie Ananillka' where _id=5;
-- update students set name='Aria Xergo' where _id=6;
-- update students set name='Edea Rostoni' where _id=7;
-- update students set name='Elayne Janin' where _id=8;
-- update students set name='Klai Berus' where _id=9;
-- update students set name='Boc Kodd' where _id=10;
-- update students set name='Aztin Tess' where _id=11;
-- update students set name='Burin Cruz' where _id=12;
-- update students set name='Rine Grainer' where _id=13;
-- update students set name='Garr Salis' where _id=14;
-- update students set name='Nikana Kwai' where _id=15;
-- update students set name='Herub Arcturus' where  _id=16;
-- update students set name='Proddo Scutu' where _id=17;
-- update students set name='Galven Twilight' where _id=18;
-- update students set name='Riyec Lester' where _id=19;
-- update students set name='Stanza Eisahn' where _id=20;
-- update students set name='Delcep Jasha' where _id=21;
-- update students set name='Sybegh Febri' where _id=22;
-- update students set name='Jorel Makesa' where _id=23;
-- update students set name='Nil Sunspot'where  _id=24;
-- update students set name='Syrena Modun' where _id=25;
-- update students set name='Addison Nunes' where _id=26;
-- update students set name='Hiram Hethna' where _id=27;
-- update students set name='Nosh Ker Ghent' where _id=28;
-- update students set name='Squessibionaro Volsh' where _id=29;
-- update students set name='Giriz Cata' where _id=30;
-- update students set name='Darth Trammer' where _id=31;
-- update students set name='Derin Youngblood' where _id=32;
-- update students set name='Iocasta Dewan' where _id=33;
-- update students set name='Dei Dol' where _id=34;
-- update students set name='Leon Zih' where _id=35;
-- update students set name='Auugu Roeder' where _id=36;
-- update students set name='Wile Ktrame' where _id=37;
-- update students set name='Philipp Umdal' where _id=38;
-- update students set name='Yurist Quizan' where _id=39;
-- update students set name='Ala Taurendil' where _id=40;
-- update students set name='Fuil Chance' where _id=41;
-- update students set name='Paldamar Athan' where _id=42;
-- update students set name='Ben Brin' where _id=43;
-- update students set name='Europa Kestal' where _id=44;
-- update students set name='Xathas Cage' where _id=45;
-- update students set name='Warryk Joyriak' where _id=46;
-- update students set name='Pexereca Pollard' where _id=47;
-- update students set name='Dorn Kosokhan' where _id=48;
-- update students set name='Philipp Waray' where _id=49;
-- update students set name='Pacer Sixxkiller' where _id=50;
-- update students set name='Imay Ashen' where _id=51;
-- update students set name='Perth Warner' where _id=52;
-- update students set name='Maxon Lund' where _id=53;
-- update students set name='Keyan Omega' where _id=54;
-- update students set name='Cyern Brahnx' where _id=55;
-- update students set name='Frank Jahsop' where _id=56;
-- update students set name='Natan Tendoora' where _id=57;
-- update students set name='Orus Lassic' where _id=58;
-- update students set name='Jaden Holst' where _id=59;
-- update students set name='Lizzy Versio' where _id=60;
-- update students set name='Daska Nizzre' where _id=61;
-- update students set name='Ingo Ran-shok' where _id=62;
-- update students set name='Vosh Sheotah' where _id=63;
-- update students set name='Thaneo Rethana' where _id=64;
-- update students set name='Gaen Onasi' where _id=65;
-- update students set name='Bentha Lassic' where _id=66;
-- update students set name='Icio Kavos' where _id=67;
-- update students set name='Rhil Thaxton' where _id=68;
-- update students set name='Drago Solomon' where _id=69;
-- update students set name='Jens Landala' where _id=70;
-- update students set name='Plaba Senreiko' where _id=71;
-- update students set name='Korwin McGhee' where _id=72;
-- update students set name='Jensi Schmitt' where _id=73;
-- update students set name='Zev Riburn' where _id=74;
-- update students set name='Logra Mefrid' where _id=75;
-- update students set name='Rayfe Dorien' where _id=76;
-- update students set name='Hurley Mindar' where _id=77;
-- update students set name='Alejandro Brower' where _id=78;
-- update students set name='Ranneth Thane' where _id=79;
-- update students set name='Cerone Thek' where _id=80;
-- update students set name='Estefan  Kothari' where _id=81;
-- update students set name='Seit Dymos' where _id=82;
-- update students set name='Ariel Denive' where _id=83;
-- update students set name='Skye Vin Deova' where _id=84;
-- update students set name='Kath Kennison' where _id=85;
-- update students set name='Darren Marshall' where _id=86;
-- update students set name='Kasari Nise' where _id=87;
-- update students set name='Remmy Ashukahwa' where _id=88;
-- update students set name='Talon Rehal' where _id=89;
-- update students set name='Tressk Allerti' where _id=90;
-- update students set name='Candurous Obarel' where _id=91;
-- update students set name='Darnius Cridmeen' where _id=92;
-- update students set name='Qurzit Torwyn' where _id=93;

-- select * from students;
