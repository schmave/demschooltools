# --- !Ups

alter table public.account alter column date_last_monthly_credit type timestamptz;
alter table public.charge alter column rp_complete_date type timestamptz;

alter table public.comments alter column created type timestamptz;
alter table public.comments alter column created set default now();

alter table public.donation alter column date type timestamptz;
alter table public.donation alter column date set default now();

alter table public.donation alter column thanked_time type timestamptz;
alter table public.donation alter column indiegogo_reward_given_time type timestamptz;

alter table public.mailchimp_sync alter column last_sync type timestamptz;
alter table public.organization alter column mailchimp_last_sync_person_changes type timestamptz;

alter table public.person alter column created type timestamptz;
alter table public.person alter column created set default now();

alter table public.person_change alter column "time" type timestamptz;
alter table public.person_change alter column "time" set default now();

alter table public.person_tag_change alter column "time" type timestamptz;
alter table public.person_tag_change alter column "time" set default now();

alter table public.role_record alter column date_created type timestamptz;
alter table public.transactions alter column date_created type timestamptz;

# --- !Downs