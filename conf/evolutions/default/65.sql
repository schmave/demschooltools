# --- !Ups

update person set
   first_name=TRIM(CONCAT(first_name, ' ', last_name)),
   last_name=''
  where last_name != '' and is_family=true;


# --- !Downs

