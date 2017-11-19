create table person (
person_id                 integer not null,
first_name                varchar(255),
last_name                 varchar(255),
notes                     TEXT,
address                   varchar(255),
city                      varchar(255),
state                     varchar(255),
zip                       varchar(255),
neighborhood              varchar(255),
email                     varchar(255),
dob                       timestamp,
approximate_dob           timestamp,
is_family                 boolean,
family_person_id          integer,
constraint pk_person primary key (person_id))
;

create sequence person_seq;

alter table person add constraint fk_person_family_1 foreign key (family_person_id) references person (person_id);
create index ix_person_family_1 on person (family_person_id);

create table linked_account (
id                        bigint not null,
user_id                   bigint,
provider_user_id          varchar(255),
provider_key              varchar(255),
constraint pk_linked_account primary key (id))
;

create table users (
id                        bigint not null,
email                     varchar(255),
name                      varchar(255),
active                    boolean,
email_validated           boolean,
constraint pk_users primary key (id))
;

create sequence linked_account_seq;

create sequence users_seq;

alter table linked_account add constraint fk_linked_account_user_1 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_linked_account_user_1 on linked_account (user_id);

create table phone_numbers (
id                        bigint not null,
person_id                 integer not null,
comment                   varchar(255),
number                    varchar(255),
constraint pk_phone_numbers primary key (id))
;

create sequence phone_numbers_seq;

alter table phone_numbers add constraint fk_phone_numbers_person_1 foreign key (person_id) references person (person_id) on delete restrict on update restrict;
create index ix_phone_numbers_1 on phone_numbers (person_id);

CREATE TABLE person_tag (
tag_id          bigint NOT NULL,
person_id       INTEGER NOT NULL,
creator_id      bigint NOT NULL,
created         timestamp DEFAULT NOW(),
constraint pk_person_tag primary key(tag_id, person_id)
);


CREATE TABLE tag (
id            bigint NOT NULL,
title         VARCHAR(255) NOT NULL,
constraint pk_tag primary key(id),
constraint unique_title UNIQUE(title)
);

create sequence tag_seq;



alter table person_tag add constraint fk_person_tag_person_1 foreign key (person_id) references person (person_id) on delete restrict on update restrict;
create index ix_person_tag_1 on person_tag (person_id);

alter table person_tag add constraint fk_person_tag_tag_1 foreign key (tag_id) references tag (id) on delete restrict on update restrict;
create index ix_person_tag_2 on person_tag (tag_id);

alter table person_tag add constraint fk_person_tag_creator_1 foreign key (creator_id) references users (id) on delete restrict on update restrict;


ALTER TABLE person ALTER COLUMN dob TYPE DATE;
ALTER TABLE person ALTER COLUMN approximate_dob TYPE DATE;

CREATE TABLE comments (
id              INTEGER NOT NULL,
person_id       INTEGER NOT NULL,
user_id       INTEGER NOT NULL,
message         TEXT,
created         timestamp DEFAULT NOW(),
constraint pk_comments primary key(id),
constraint fk_comments_person foreign key (person_id) references person (person_id) on delete restrict on update restrict,
constraint fk_comments_user foreign key (user_id) references users (id) on delete restrict on update restrict
);

create sequence comments_seq;

ALTER TABLE users ADD constraint users_unique_email_1 UNIQUE(email);

ALTER TABLE person ALTER person_id SET DEFAULT nextval('person_seq');
ALTER TABLE person ALTER is_family SET DEFAULT FALSE;
UPDATE person SET is_family=FALSE WHERE is_family IS NULL;
ALTER TABLE person ALTER is_family SET NOT NULL;

ALTER TABLE comments ALTER id SET DEFAULT nextval('comments_seq');

ALTER TABLE tag ALTER id SET DEFAULT nextval('tag_seq');

CREATE TABLE task_list (
id serial,
title VARCHAR(255),
tag_id INTEGER,
constraint pk_task_list primary key(id),
constraint fk_task_list_tag foreign key(tag_id) references tag(id) ON DELETE restrict ON UPDATE restrict
);

CREATE TABLE TASK
(
id serial,
title VARCHAR(255),
task_list_id INTEGER,
sort_order INTEGER,
constraint pk_task primary key (id),
constraint fk_task_task_list foreign key(task_list_id) references task_list(id) ON DELETE restrict ON UPDATE restrict
);

CREATE TABLE completed_task (
id serial,
task_id INTEGER,
person_id INTEGER,
comment_id INTEGER,
constraint pk_completed_task primary key (id),
constraint fk_completed_task_task foreign key(task_id) references TASK (id) ON DELETE restrict ON UPDATE restrict,
constraint fk_completed_task_person foreign key(person_id) references person (person_id) ON DELETE restrict ON UPDATE restrict,
constraint fk_completed_task_comment foreign key(comment_id) references comments(id)  ON DELETE restrict ON UPDATE restrict,
constraint unique_completed_task_1 UNIQUE(task_id, person_id)
);

ALTER TABLE phone_numbers drop constraint fk_phone_numbers_person_1;
alter table phone_numbers add constraint fk_phone_numbers_person_1 foreign key (person_id) references person (person_id) on delete cascade on update restrict;

ALTER TABLE person_tag drop constraint fk_person_tag_person_1;
alter table person_tag add constraint fk_person_tag_person_1 foreign key (person_id) references person (person_id) on delete cascade on update restrict;

alter table person_tag drop constraint fk_person_tag_tag_1;
alter table person_tag add constraint fk_person_tag_tag_1 foreign key (tag_id) references tag (id) on delete cascade on update restrict;

ALTER TABLE comments drop constraint fk_comments_person;
ALTER TABLE comments ADD constraint fk_comments_person foreign key (person_id) references person (person_id) on delete cascade on update restrict;

ALTER TABLE completed_task drop constraint fk_completed_task_person;
ALTER TABLE completed_task ADD constraint fk_completed_task_person foreign key(person_id) references person (person_id) ON DELETE cascade ON UPDATE restrict;
ALTER TABLE completed_task drop constraint fk_completed_task_comment;
ALTER TABLE completed_task ADD constraint fk_completed_task_comment foreign key(comment_id) references comments(id)  ON DELETE cascade ON UPDATE restrict;

alter table person add column created timestamp default '2012-03-01';
alter table person alter column created set default NOW();

alter table person add column gender varchar(10) not null default 'Unknown'
check(gender = 'Unknown' or gender='Male' or gender='Female');

alter table person drop constraint person_gender_check;
alter table person add constraint person_gender_check
check(gender = 'Unknown' or gender='Male' or gender='Female' or gender='Other');

create table donation (
id serial,
dollar_value float,
is_cash boolean not null,
description TEXT not null,
person_id integer not null,
date timestamp without time zone not null default now(),

thanked boolean not null,
thanked_by_user_id integer default null,
thanked_time timestamp without time zone default null,

indiegogo_reward_given boolean not null,
indiegogo_reward_by_user_id integer default null,
indiegogo_reward_given_time timestamp without time zone default null,

constraint pk_donation primary key(id),
constraint fk_donation_person foreign key(person_id) references person(person_id) ON DELETE cascade ON UPDATE restrict,

constraint fk_donation_user_1 foreign key(thanked_by_user_id) references users(id) on delete restrict on update restrict,
constraint fk_donation_user_2 foreign key(indiegogo_reward_by_user_id) references users(id) on delete restrict on update restrict
);

alter table tag add column use_student_display boolean default false;

alter table task add column enabled boolean default true;

alter table person add column previous_school character varying(255) default '' not null;
alter table person add column school_district character varying(255) default '' not null;

CREATE TABLE "case"
(
case_number VARCHAR(255) NOT NULL DEFAULT '',
findings text,
DATE DATE,
constraint pk_case primary key (case_number)
);

CREATE TABLE rule
(
id serial,
title VARCHAR(255) NOT NULL DEFAULT '',
constraint pk_rule primary key(id)
);

CREATE TABLE charge
(
id serial,
case_id VARCHAR(255) NOT NULL DEFAULT '',
person_id int,
rule_id int,
plea VARCHAR(255) NOT NULL DEFAULT '',
resolution_plan text,
constraint pk_charge primary key (id),
constraint fk_charge_person foreign key (person_id) references person(person_id),
constraint fk_charge_rule foreign key (rule_id) references rule(id)
);

CREATE TABLE testify_record
(
case_number VARCHAR(255) NOT NULL DEFAULT '',
person_id int,
constraint pk_testify_record primary key (case_number, person_id),
constraint fk_testify_person foreign key (person_id) references person(person_id),
constraint fk_testify_case foreign key (case_number) references "case"(case_number)
);

ALTER TABLE charge ADD constraint fk_charge_case foreign key (case_Id) references "case"(case_number);

CREATE TABLE meeting
(
id serial,
"date" DATE,
constraint pk_meeting primary key (id)
);

CREATE TABLE person_at_meeting
(
meeting_id int,
person_id int,
role int,
constraint pk_person_at_meeting primary key (meeting_id, person_id, role),
constraint fk_PAM_meeting foreign key (meeting_id) references meeting(id),
constraint fk_PAM_person foreign key (person_id) references person(person_id)
);

ALTER TABLE "case" ADD COLUMN meeting_id int;
ALTER TABLE "case" ADD constraint fk_case_meeting foreign key (meeting_id) references meeting(id);

ALTER TABLE "person" ADD COLUMN grade varchar(8) not null default '';

ALTER TABLE person_at_meeting ADD COLUMN id serial;

ALTER TABLE "case" ALTER meeting_id SET NOT NULL;
ALTER TABLE "case" ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE "case" ADD COLUMN writer_id INTEGER;
ALTER TABLE "case" ADD constraint fk_case_writer foreign key (writer_id) references person(person_id);

UPDATE "case" SET findings='' WHERE findings IS NULL;
ALTER TABLE "case" ALTER findings SET NOT NULL;

ALTER TABLE "testify_record" ADD COLUMN id serial;

UPDATE charge SET resolution_plan='' WHERE resolution_plan IS NULL;
ALTER TABLE charge ALTER resolution_plan SET NOT NULL;

ALTER TABLE person add column display_name varchar(255) not null default '';

CREATE TABLE allowed_ips (ip VARCHAR(30) primary key);
INSERT INTO allowed_ips (ip) VALUES('127.0.0.1');

ALTER TABLE charge ADD COLUMN referred_to_sm BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN sm_decision text;

ALTER TABLE rule ADD COLUMN removed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE charge ADD COLUMN sm_decision_date date;

CREATE TABLE email
(
id serial,
message TEXT,
sent boolean not null,
deleted boolean not null,
constraint pk_email primary key (id)
);

CREATE TABLE chapter
(
id serial,
num integer not null,
title varchar(255) not null,
constraint pk_chapter primary key (id)
);

CREATE TABLE section
(
id serial,
num integer not null,
title varchar(255) not null,
chapter_id integer not null,
constraint pk_section primary key (id),
constraint fk_section_chapter foreign key(chapter_id) references chapter(id) on delete restrict on update restrict
);

CREATE TABLE entry
(
id serial,
num integer not null,
title varchar(255) not null,
section_id integer not null,
content text not null,
constraint pk_entry primary key (id),
constraint fk_entry_section foreign key(section_id) references section(id) on delete restrict on update restrict
);

alter table section alter num type varchar(8);
alter table entry alter num type varchar(8);

alter table chapter add column deleted boolean not null default false;
alter table section add column deleted boolean not null default false;
alter table entry add column deleted boolean not null default false;

alter table charge drop CONSTRAINT fk_charge_rule;
delete from charge;
alter table charge add CONSTRAINT fk_charge_entry foreign key(rule_id) references entry (id) on update restrict on delete restrict;

drop table rule;

CREATE TABLE manual_change
(
id serial,

chapter_id INTEGER,
section_id INTEGER,
entry_id INTEGER,

was_deleted BOOLEAN NOT NULL,
was_created BOOLEAN NOT NULL,

old_title VARCHAR(255),
new_title VARCHAR(255),
old_content text,
new_content text,
old_num VARCHAR(8),
new_num VARCHAR(8),

date_entered timestamp without time zone NOT NULL,

CONSTRAINT pk_manual_change PRIMARY KEY(id),
CONSTRAINT fk_change_chapter FOREIGN KEY (chapter_id)
REFERENCES chapter (id) MATCH SIMPLE
ON UPDATE RESTRICT ON DELETE RESTRICT,
CONSTRAINT fk_change_section FOREIGN KEY (section_id)
REFERENCES section (id) MATCH SIMPLE
ON UPDATE RESTRICT ON DELETE RESTRICT,
CONSTRAINT fk_change_entry FOREIGN KEY (entry_id)
REFERENCES entry (id) MATCH SIMPLE
ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE TABLE organization
(
id serial,
name VARCHAR(255),
constraint pk_organization PRIMARY KEY (id)
);

INSERT INTO organization (id, name) VALUES (1, 'Three Rivers Village School');

CREATE TABLE organization_hosts
(
host text,
organization_id INTEGER NOT NULL,
constraint pk_organization_hosts PRIMARY KEY(host),
constraint fk_organization_hosts_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict
);

ALTER TABLE allowed_ips ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE allowed_ips ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE allowed_ips ADD constraint fk_allowed_ips_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE chapter ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE chapter ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE chapter ADD constraint fk_chapter_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE email ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE email ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE email ADD constraint fk_email_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE meeting ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE meeting ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE meeting ADD constraint fk_meeting_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE person ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE person ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE person ADD constraint fk_person_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE tag ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE tag ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE tag ADD constraint fk_tag_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE task_list ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE task_list ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE task_list ADD constraint fk_task_list_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE users ADD COLUMN organization_id INTEGER NOT NULL DEFAULT 1;
ALTER TABLE users ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE users ADD constraint fk_users_organization foreign key(organization_id) references organization (id) on update restrict on delete restrict;

ALTER TABLE tag DROP constraint unique_title;
ALTER TABLE tag ADD CONSTRAINT unique_title_org UNIQUE (title, organization_id);

UPDATE person SET last_name='' WHERE last_name IS NULL;
ALTER TABLE person ALTER COLUMN last_name SET DEFAULT '';
ALTER TABLE person ALTER COLUMN last_name SET NOT NULL;

UPDATE person SET first_name='' WHERE first_name IS NULL;
ALTER TABLE person ALTER COLUMN first_name SET DEFAULT '';
ALTER TABLE person ALTER COLUMN first_name SET NOT NULL;

UPDATE person SET notes='' WHERE notes IS NULL;
ALTER TABLE person ALTER COLUMN notes SET DEFAULT '';
ALTER TABLE person ALTER COLUMN notes SET NOT NULL;

UPDATE person SET address='' WHERE address IS NULL;
ALTER TABLE person ALTER COLUMN address SET DEFAULT '';
ALTER TABLE person ALTER COLUMN address SET NOT NULL;

UPDATE person SET city='' WHERE city IS NULL;
ALTER TABLE person ALTER COLUMN city SET DEFAULT '';
ALTER TABLE person ALTER COLUMN city SET NOT NULL;

UPDATE person SET state='' WHERE state IS NULL;
ALTER TABLE person ALTER COLUMN state SET DEFAULT '';
ALTER TABLE person ALTER COLUMN state SET NOT NULL;

UPDATE person SET zip='' WHERE zip IS NULL;
ALTER TABLE person ALTER COLUMN zip SET DEFAULT '';
ALTER TABLE person ALTER COLUMN zip SET NOT NULL;

UPDATE person SET neighborhood='' WHERE neighborhood IS NULL;
ALTER TABLE person ALTER COLUMN neighborhood SET DEFAULT '';
ALTER TABLE person ALTER COLUMN neighborhood SET NOT NULL;

UPDATE person SET email='' WHERE email IS NULL;
ALTER TABLE person ALTER COLUMN email SET DEFAULT '';
ALTER TABLE person ALTER COLUMN email SET NOT NULL;

ALTER TABLE chapter ALTER COLUMN num TYPE VARCHAR(8);

ALTER TABLE "case" ADD COLUMN id serial;

ALTER TABLE charge RENAME case_id TO case_number;

ALTER TABLE testify_record ADD COLUMN case_id INTEGER;
ALTER TABLE charge ADD COLUMN case_id INTEGER;

UPDATE testify_record tr SET case_id=(SELECT id FROM "case" c WHERE tr.case_number = c.case_number);
UPDATE charge cg SET case_id=(SELECT id FROM "case" c WHERE cg.case_number = c.case_number);

ALTER TABLE testify_record DROP constraint fk_testify_case;
ALTER TABLE testify_record DROP COLUMN case_number;
ALTER TABLE charge DROP constraint fk_charge_case;
ALTER TABLE charge DROP COLUMN case_number;

ALTER TABLE "case" DROP constraint pk_case;
ALTER TABLE "case" ADD constraint pk_case_2 primary key (id);
ALTER TABLE "case" ADD constraint u_case_number_meeting UNIQUE (case_number, meeting_id);

ALTER TABLE testify_record ADD CONSTRAINT fk_testify_case FOREIGN KEY (case_id) REFERENCES "case" (id);
ALTER TABLE charge ADD CONSTRAINT fk_charge_case FOREIGN KEY (case_id) REFERENCES "case" (id);

ALTER TABLE "case" ADD COLUMN time VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE "case" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE "charge" ADD COLUMN minor_referral_destination VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE "charge" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';
alter table "case" drop column severity;

CREATE TABLE person_tag_change
(
id serial NOT NULL,
person_id INTEGER NOT NULL,
tag_id INTEGER NOT NULL,
creator_id INTEGER NOT NULL,
time timestamp without time zone DEFAULT now(),
was_add BOOLEAN NOT NULL,
constraint pk_ptc primary key(id),
constraint fk_ptc_person foreign key(person_id) references person(person_id),
constraint fk_ptc_tag foreign key(tag_id) references tag(id),
constraint fk_ptc_creator foreign key(creator_id) references users(id)
);

INSERT INTO person_tag_change (person_id, tag_id, creator_id, time, was_add)
(SELECT person_id, tag_id, creator_id, created, true FROM person_tag);

ALTER TABLE person_tag DROP constraint fk_person_tag_creator_1;
ALTER TABLE person_tag DROP creator_id;
ALTER TABLE person_tag DROP created;

CREATE TABLE mailchimp_sync
(
id serial NOT NULL,
tag_id INTEGER NOT NULL,
mailchimp_list_id varchar(255) NOT NULL,
sync_local_adds BOOLEAN NOT NULL,
sync_local_removes BOOLEAN NOT NULL,
last_sync timestamp without time zone,

constraint pk_mailchimp_sync primary key(id),
constraint fk_mailchimp_sync_tag foreign key(tag_id) references tag(id)
);

ALTER TABLE organization ADD COLUMN mailchimp_api_key VARCHAR(255);

ALTER TABLE organization ADD COLUMN mailchimp_last_sync_person_changes timestamp without time zone;
ALTER TABLE organization ALTER COLUMN mailchimp_api_key SET DEFAULT '';
update organization set mailchimp_api_key='' where mailchimp_api_key is null;
ALTER TABLE organization ALTER COLUMN mailchimp_api_key SET NOT NULL;

CREATE TABLE person_change
(
person_id INTEGER NOT NULL,
old_email VARCHAR(255) NOT NULL,
new_email VARCHAR(255) NOT NULL,
"time" timestamp without time zone DEFAULT now(),

constraint fk_person_change_person foreign key(person_id) references person(person_id)
);

ALTER TABLE organization ADD COLUMN mailchimp_updates_email varchar(255) DEFAULT '' NOT NULL;

ALTER TABLE testify_record ADD COLUMN role INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE testify_record RENAME TO person_at_case;

INSERT INTO person_at_case (person_id, case_id, role)
(SELECT writer_id, id, 1 FROM "case" WHERE writer_id IS NOT NULL);

ALTER TABLE "case" DROP constraint fk_case_writer;
ALTER TABLE "case" DROP writer_id;

ALTER TABLE ENTRY ALTER num TYPE text;
ALTER TABLE section ALTER num TYPE text;
ALTER TABLE chapter ALTER num TYPE text;

ALTER TABLE manual_change ALTER old_num TYPE text;
ALTER TABLE manual_change ALTER new_num TYPE text;

ALTER TABLE person ALTER gender TYPE text;
ALTER TABLE person ALTER grade TYPE text;

ALTER TABLE charge ADD COLUMN rp_complete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN rp_complete_date timestamp;

UPDATE charge SET rp_complete = TRUE;
UPDATE charge SET rp_complete_date = meeting.DATE FROM "case", meeting
WHERE charge.case_id="case".id AND "case".meeting_id=meeting.id;

CREATE TABLE case_meeting
(
case_id INTEGER NOT NULL,
meeting_id integer NOT NULL,
CONSTRAINT pk_case_meeting PRIMARY KEY (case_id, meeting_id),

CONSTRAINT fk_case_meeting_case FOREIGN KEY (case_id)
REFERENCES "case" (id) MATCH SIMPLE
ON UPDATE RESTRICT ON DELETE CASCADE,

CONSTRAINT fk_case_meeting_meeting FOREIGN KEY (meeting_id)
REFERENCES meeting (id) MATCH SIMPLE
ON UPDATE RESTRICT ON DELETE CASCADE
);

ALTER TABLE "case" ADD COLUMN date_closed DATE;

UPDATE "case" SET date_closed=m.DATE FROM meeting m WHERE "case".meeting_id=m.id;

alter table phone_numbers alter column id set default nextval('phone_numbers_seq'::regclass);

CREATE TABLE attendance_code
(
id serial NOT NULL,
organization_id integer NOT NULL,
code    VARCHAR(8) NOT NULL,
description text NOT NULL,
color VARCHAR(16) NOT NULL,
CONSTRAINT pk_attendance_code PRIMARY KEY(id),
CONSTRAINT fk_attendance_code_organization FOREIGN KEY (organization_id) references organization(id)
);

CREATE TABLE attendance_day
(
id serial NOT NULL,
day DATE NOT NULL,
person_id INTEGER NOT NULL,
code VARCHAR(8),
start_time time,
end_time time,
CONSTRAINT pk_attendance_day PRIMARY KEY(id),
CONSTRAINT fk_attendance_day_person FOREIGN KEY (person_id) references person(person_id)
);

CREATE TABLE attendance_week
(
id serial NOT NULL,
person_id INTEGER NOT NULL,
monday DATE NOT NULL,
extra_hours INTEGER NOT NULL,
CONSTRAINT pk_attendance_week PRIMARY KEY(id),
CONSTRAINT fk_attendance_week_person FOREIGN KEY (person_id) references person(person_id)
);

ALTER TABLE attendance_week ALTER COLUMN extra_hours TYPE REAL;

alter table linked_account alter column id set default nextval('linked_account_seq'::regclass);
alter table users alter column id set default nextval('users_seq'::regclass);

CREATE TABLE notification_rule (
id serial,
tag_id INTEGER,
the_type INTEGER NOT NULL,
email text NOT NULL,
organization_id INTEGER NOT NULL,
CONSTRAINT pk_notification_rule PRIMARY KEY(id),
CONSTRAINT fk_notifiaction_rule_tag FOREIGN KEY(tag_id) references tag(id),
CONSTRAINT fk_notifiaction_rule_organization FOREIGN KEY(organization_id) references organization(id)
);

ALTER TABLE TASK ALTER enabled DROP DEFAULT;

CREATE TABLE user_role (
id serial,
user_id INTEGER NOT NULL,
role text NOT NULL,
CONSTRAINT pk_user_role primary key(id),
CONSTRAINT fk_user_role_users foreign key(user_id) references users(id)
);

INSERT INTO user_role(user_id, role) (SELECT id, 'all-access' FROM users);

ALTER table users ALTER COLUMN id TYPE INTEGER;

ALTER TABLE users ALTER COLUMN organization_id DROP NOT NULL;

ALTER TABLE attendance_code ALTER COLUMN code TYPE VARCHAR(64);
ALTER TABLE attendance_day ALTER COLUMN code TYPE VARCHAR(64);

ALTER TABLE donation ALTER COLUMN DATE SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_tag_change ALTER COLUMN time SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_change ALTER COLUMN "time" SET DEFAULT (now() at time zone 'utc');
ALTER TABLE comments ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');

ALTER TABLE attendance_week add constraint u_attendance_week unique(person_id, monday);
ALTER TABLE attendance_day add constraint u_attendance_day unique(person_id, day);

CREATE MATERIALIZED VIEW entry_index AS
SELECT entry.id,
chapter.organization_id,
(to_tsvector(entry.content) || to_tsvector(entry.title)) as document
FROM entry
JOIN section ON entry.section_id = section.id
JOIN chapter ON section.chapter_id = chapter.id
WHERE entry.deleted=false AND section.deleted=false AND chapter.deleted=false;

CREATE INDEX idx_entry_index ON entry_index USING gin(document);

alter table linked_account add constraint u_linked_account UNIQUE(provider_key, provider_user_id);

ALTER TABLE organization ADD COLUMN printer_email varchar(255) DEFAULT '' NOT NULL;

ALTER TABLE tag ADD COLUMN show_in_jc boolean DEFAULT false NOT NULL;

UPDATE tag set show_in_jc=(title='Staff' or title='Current Student');

ALTER TABLE tag add column show_in_menu boolean default true not null;

ALTER TABLE organization ADD COLUMN jc_reset_day int DEFAULT 3 NOT NULL;

ALTER TABLE organization ADD COLUMN show_history_in_print boolean DEFAULT true NOT NULL;
ALTER TABLE organization ADD COLUMN show_last_modified_in_print boolean DEFAULT true NOT NULL;
