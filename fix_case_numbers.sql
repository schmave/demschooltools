# See how many case numbers of what length there are
select count(*), o.name, LENGTH(case_number) c
from "case" join meeting m on "case".meeting_id=m.id
join organization o on m.organization_id=o.id
group by o.name, c;



# See how many case numbers needing upgrade there are
select count(*), o.name, LENGTH(case_number) c
from "case" join meeting m on "case".meeting_id=m.id
join organization o on m.organization_id=o.id
where m.date < '2016-08-01' and length(case_number)=8
group by o.name, c;



# Add a year number to old case numbers
update "case"
set case_number=date_part('year', m.date) || '-' || case_number
from meeting m
where LENGTH(case_number)=8 and m.date < '2016-08-01' and "case".meeting_id=m.id;
