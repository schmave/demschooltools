
--- 1.sql

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

--- 2.sql

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

--- 3.sql

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

--- 4.sql

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

--- 5.sql

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

--- 6.sql

ALTER TABLE users ADD constraint users_unique_email_1 UNIQUE(email);

--- 7.sql

ALTER TABLE person ALTER person_id SET DEFAULT nextval('person_seq');
ALTER TABLE person ALTER is_family SET DEFAULT FALSE;
UPDATE person SET is_family=FALSE WHERE is_family IS NULL;
ALTER TABLE person ALTER is_family SET NOT NULL;
ALTER TABLE comments ALTER id SET DEFAULT nextval('comments_seq');
ALTER TABLE tag ALTER id SET DEFAULT nextval('tag_seq');

--- 8.sql

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

--- 9.sql

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

--- 10.sql

alter table person add column gender varchar(10) not null default 'Unknown'
   check(gender = 'Unknown' or gender='Male' or gender='Female');

--- 11.sql

alter table person drop constraint person_gender_check;
alter table person add constraint person_gender_check
   check(gender = 'Unknown' or gender='Male' or gender='Female' or gender='Other');

--- 12.sql

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

--- 13.sql

alter table tag add column use_student_display boolean default false;

--- 14.sql

alter table task add column enabled boolean default true;

--- 15.sql

alter table person add column previous_school character varying(255) default '' not null;
alter table person add column school_district character varying(255) default '' not null;

--- 16.sql

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

--- 17.sql

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

--- 18.sql

ALTER TABLE "person" ADD COLUMN grade varchar(8) not null default '';

--- 19.sql

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

--- 20.sql

ALTER TABLE person add column display_name varchar(255) not null default '';

--- 21.sql

CREATE TABLE allowed_ips (ip VARCHAR(30) primary key);
INSERT INTO allowed_ips (ip) VALUES('127.0.0.1');

--- 22.sql

ALTER TABLE charge ADD COLUMN referred_to_sm BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN sm_decision text;

--- 23.sql

ALTER TABLE rule ADD COLUMN removed BOOLEAN NOT NULL DEFAULT FALSE;

--- 24.sql

ALTER TABLE charge ADD COLUMN sm_decision_date date;

--- 25.sql

CREATE TABLE email
   (
    id serial,
    message TEXT,
	sent boolean not null,
	deleted boolean not null,
    constraint pk_email primary key (id)
    );

--- 26.sql

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

--- 27.sql

alter table section alter num type varchar(8);
alter table entry alter num type varchar(8);

--- 28.sql

alter table chapter add column deleted boolean not null default false;
alter table section add column deleted boolean not null default false;
alter table entry add column deleted boolean not null default false;

--- 29.sql

alter table charge drop CONSTRAINT fk_charge_rule;
delete from charge;
alter table charge add CONSTRAINT fk_charge_entry foreign key(rule_id) references entry (id) on update restrict on delete restrict;
drop table rule;

--- 30.sql

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

--- 31.sql

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

--- 32.sql

ALTER TABLE tag DROP constraint unique_title;
ALTER TABLE tag ADD CONSTRAINT unique_title_org UNIQUE (title, organization_id);

--- 33.sql

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

--- 34.sql

ALTER TABLE chapter ALTER COLUMN num TYPE VARCHAR(8);

--- 35.sql

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

--- 36.sql

ALTER TABLE "case" ADD COLUMN time VARCHAR(255) NOT NULL DEFAULT '';

--- 37.sql

ALTER TABLE "case" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';

--- 38.sql

ALTER TABLE "charge" ADD COLUMN minor_referral_destination VARCHAR(255) NOT NULL DEFAULT '';

--- 39.sql

ALTER TABLE "charge" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';
alter table "case" drop column severity;

--- 40.sql

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

--- 41.sql

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

--- 42.sql

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

--- 43.sql

ALTER TABLE organization ADD COLUMN mailchimp_updates_email varchar(255) DEFAULT '' NOT NULL;

--- 44.sql

ALTER TABLE testify_record ADD COLUMN role INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE testify_record RENAME TO person_at_case;
INSERT INTO person_at_case (person_id, case_id, role)
  (SELECT writer_id, id, 1 FROM "case" WHERE writer_id IS NOT NULL);
ALTER TABLE "case" DROP constraint fk_case_writer;
ALTER TABLE "case" DROP writer_id;

--- 45.sql

ALTER TABLE ENTRY ALTER num TYPE text;
ALTER TABLE section ALTER num TYPE text;
ALTER TABLE chapter ALTER num TYPE text;
ALTER TABLE manual_change ALTER old_num TYPE text;
ALTER TABLE manual_change ALTER new_num TYPE text;
ALTER TABLE person ALTER gender TYPE text;
ALTER TABLE person ALTER grade TYPE text;

--- 46.sql

ALTER TABLE charge ADD COLUMN rp_complete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN rp_complete_date timestamp;
UPDATE charge SET rp_complete = TRUE;
UPDATE charge SET rp_complete_date = meeting.DATE FROM "case", meeting
    WHERE charge.case_id="case".id AND "case".meeting_id=meeting.id;

--- 47.sql

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

--- 48.sql

alter table phone_numbers alter column id set default nextval('phone_numbers_seq'::regclass);

--- 49.sql

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

--- 50.sql

ALTER TABLE attendance_week ALTER COLUMN extra_hours TYPE REAL;

--- 51.sql

alter table linked_account alter column id set default nextval('linked_account_seq'::regclass);
alter table users alter column id set default nextval('users_seq'::regclass);

--- 52.sql

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

--- 53.sql

ALTER TABLE TASK ALTER enabled DROP DEFAULT;

--- 54.sql

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

--- 55.sql

ALTER TABLE attendance_code ALTER COLUMN code TYPE VARCHAR(64);
ALTER TABLE attendance_day ALTER COLUMN code TYPE VARCHAR(64);

--- 56.sql

ALTER TABLE donation ALTER COLUMN DATE SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_tag_change ALTER COLUMN time SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person_change ALTER COLUMN "time" SET DEFAULT (now() at time zone 'utc');
ALTER TABLE comments ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');
ALTER TABLE person ALTER COLUMN created SET DEFAULT (now() at time zone 'utc');

--- 57.sql

ALTER TABLE attendance_week add constraint u_attendance_week unique(person_id, monday);
ALTER TABLE attendance_day add constraint u_attendance_day unique(person_id, day);

--- 58.sql

CREATE MATERIALIZED VIEW entry_index AS
SELECT entry.id,
       chapter.organization_id,
       (to_tsvector(entry.content) || to_tsvector(entry.title)) as document
FROM entry
JOIN section ON entry.section_id = section.id
JOIN chapter ON section.chapter_id = chapter.id
WHERE entry.deleted=false AND section.deleted=false AND chapter.deleted=false;
CREATE INDEX idx_entry_index ON entry_index USING gin(document);

--- 59.sql

alter table linked_account add constraint u_linked_account UNIQUE(provider_key, provider_user_id);

--- 60.sql

ALTER TABLE organization ADD COLUMN printer_email varchar(255) DEFAULT '' NOT NULL;

--- 61.sql

ALTER TABLE tag ADD COLUMN show_in_jc boolean DEFAULT false NOT NULL;
UPDATE tag set show_in_jc=(title='Staff' or title='Current Student');
ALTER TABLE tag add column show_in_menu boolean default true not null;

--- 62.sql

ALTER TABLE organization ADD COLUMN jc_reset_day int DEFAULT 3 NOT NULL;

--- 63.sql

ALTER TABLE organization ADD COLUMN show_history_in_print boolean DEFAULT true NOT NULL;
ALTER TABLE organization ADD COLUMN show_last_modified_in_print boolean DEFAULT true NOT NULL;

--- 64.sql

ALTER TABLE organization ADD COLUMN show_custodia BOOLEAN default false not null;
ALTER TABLE organization ADD COLUMN show_attendance BOOLEAN default true not null;
ALTER TABLE organization ADD COLUMN short_name VARCHAR(255) default '' not null;
ALTER TABLE organization ADD COLUMN custodia_password VARCHAR(255) default '' not null;
update organization set short_name='TRVS' where id=1;
update organization set short_name='PFS' where id=2;
update organization set short_name='Fairhaven' where id=3;
update organization set short_name='TCS' where id=4;
update organization set short_name='MLC' where id=5;
update organization set short_name='TOS' where id=6;
update organization set short_name='HSS' where id=7;
update organization set short_name='CSS' where id=8;
update organization set short_name='Sandbox' where id=9;


--- 65.sql

update person set
   first_name=TRIM(CONCAT(first_name, ' ', last_name)),
   last_name=''
  where last_name != '' and is_family=true;

--- 66.sql

alter table tag add column show_in_attendance boolean not null default false;
update tag set show_in_attendance=show_in_jc;

--- 67.sql

alter table organization add column show_accounting boolean not null default false;

--- 68.sql

CREATE TABLE institution (
   id serial,
   organization_id integer NOT NULL,
   name text NOT NULL,
   type integer NOT NULL,
   CONSTRAINT pk_institution primary key(id),
   CONSTRAINT fk_institution_organization FOREIGN KEY (organization_id) references organization(id)
);
CREATE TABLE account (
   id serial,
   organization_id integer NOT NULL,
   person_id integer,
   institution_id integer,
   type integer NOT NULL,
   initial_balance decimal NOT NULL,
   CONSTRAINT pk_account primary key(id),
   CONSTRAINT fk_account_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_account_person FOREIGN KEY (person_id) references person(person_id),
   CONSTRAINT fk_account_institution FOREIGN KEY (institution_id) references institution(id)
);

--- 69.sql

CREATE TABLE transactions (
   id serial,
   organization_id integer NOT NULL,
   from_account_id integer,
   to_account_id integer,
   from_name text NOT NULL,
   to_name text NOT NULL,
   description text NOT NULL,
   type integer NOT NULL,
   amount decimal NOT NULL,
   date_created timestamp NOT NULL,
   CONSTRAINT pk_transaction primary key(id),
   CONSTRAINT fk_transaction_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_transaction_from_account FOREIGN KEY (from_account_id) references account(id),
   CONSTRAINT fk_transaction_to_account FOREIGN KEY (to_account_id) references account(id)
);

--- 70.sql

ALTER TABLE account ADD COLUMN name text NOT NULL DEFAULT '';
ALTER TABLE account DROP CONSTRAINT fk_account_institution;
ALTER TABLE account DROP COLUMN institution_id;
DROP TABLE institution;

--- 71.sql

ALTER TABLE transactions ADD COLUMN created_by_user_id integer;

--- 72.sql

ALTER TABLE tag ADD COLUMN show_in_account_balances boolean;
UPDATE tag SET show_in_account_balances = show_in_jc;

--- 73.sql

ALTER TABLE transactions ADD COLUMN archived boolean NOT NULL DEFAULT(false);

--- 74.sql

ALTER TABLE account ADD COLUMN monthly_credit decimal NOT NULL DEFAULT(0);

--- 75.sql

ALTER TABLE account ADD COLUMN date_last_monthly_credit timestamp;

--- 76.sql

ALTER TABLE meeting ADD CONSTRAINT unq_org_date UNIQUE (organization_id, date);

--- 77.sql

ALTER TABLE attendance_code ADD COLUMN counts_toward_attendance boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_show_percent boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_enable_partial_days boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_day_latest_start_time time;
ALTER TABLE organization ADD COLUMN attendance_day_min_hours integer;
ALTER TABLE organization ADD COLUMN attendance_partial_day_value decimal;

--- 78.sql

ALTER TABLE organization ADD COLUMN enable_case_references boolean NOT NULL DEFAULT(false);
ALTER TABLE entry ADD COLUMN is_breaking_res_plan boolean NOT NULL DEFAULT(false);
ALTER TABLE charge ADD COLUMN referenced_charge_id integer;
ALTER TABLE charge ADD CONSTRAINT fk_charge_charge FOREIGN KEY (referenced_charge_id) REFERENCES charge(id);
CREATE TABLE case_reference (
	referencing_case integer NOT NULL,
	referenced_case integer NOT NULL,
	CONSTRAINT fk_referencing_case FOREIGN KEY (referencing_case) REFERENCES "case"(id),
	CONSTRAINT fk_referenced_case FOREIGN KEY (referenced_case) REFERENCES "case"(id)
);
CREATE TABLE charge_reference (
	referencing_case integer NOT NULL,
	referenced_charge integer NOT NULL,
	CONSTRAINT fk_referencing_case FOREIGN KEY (referencing_case) REFERENCES "case"(id),
	CONSTRAINT fk_referenced_charge FOREIGN KEY (referenced_charge) REFERENCES charge(id)
);

--- 79.sql

ALTER TABLE users ADD COLUMN hashed_password TEXT NOT NULL DEFAULT('');

--- 80.sql

ALTER TABLE person ADD COLUMN pin VARCHAR(10) NOT NULL DEFAULT('');

--- 81.sql

ALTER TABLE organization ADD COLUMN show_electronic_signin boolean NOT NULL DEFAULT false;

--- 82.sql

ALTER TABLE organization ADD COLUMN attendance_admin_pin VARCHAR(10) NOT NULL DEFAULT('');

--- 83.sql

ALTER TABLE attendance_day ADD COLUMN off_campus_departure_time time;
ALTER TABLE attendance_day ADD COLUMN off_campus_return_time time;

--- 84.sql

ALTER TABLE organization ADD COLUMN attendance_enable_off_campus boolean NOT NULL DEFAULT(false);

--- 85.sql

ALTER TABLE account ADD COLUMN is_active boolean NOT NULL DEFAULT true;

--- 86.sql

ALTER TABLE attendance_code ADD COLUMN not_counted boolean NOT NULL DEFAULT false;

--- 87.sql

ALTER TABLE organization ADD COLUMN attendance_show_reports boolean NOT NULL DEFAULT(false);
ALTER TABLE organization ADD COLUMN attendance_report_latest_departure_time time;

--- 88.sql

DROP INDEX idx_entry_index;
DROP MATERIALIZED VIEW entry_index;
CREATE MATERIALIZED VIEW entry_index AS
SELECT entry.id,
       chapter.organization_id,
       (to_tsvector(entry.content) || to_tsvector(entry.title) ||
              to_tsvector(CONCAT(chapter.num, section.num, '.', entry.num))) as document
FROM entry
JOIN section ON entry.section_id = section.id
JOIN chapter ON section.chapter_id = chapter.id
WHERE entry.deleted=false AND section.deleted=false AND chapter.deleted=false;
CREATE INDEX idx_entry_index ON entry_index USING gin(document);

--- 89.sql

ALTER TABLE attendance_day ADD COLUMN off_campus_minutes_exempted integer;

--- 90.sql

ALTER TABLE organization ADD COLUMN attendance_rate_standard_time_frame integer;

--- 91.sql

ALTER TABLE organization ALTER COLUMN attendance_day_min_hours TYPE double precision;

--- 92.sql

ALTER TABLE organization ADD COLUMN attendance_report_late_fee integer;
ALTER TABLE organization ADD COLUMN attendance_report_late_fee_interval integer;

--- 93.sql

ALTER TABLE organization ADD COLUMN attendance_show_weighted_percent boolean NOT NULL DEFAULT(false);
ALTER TABLE organization DROP COLUMN attendance_rate_standard_time_frame;

--- 94.sql

CREATE TABLE attendance_rule (
   id serial,
   organization_id integer NOT NULL,
   category text,
   person_id integer,
   start_date date,
   end_date date,
   monday boolean NOT NULL,
   tuesday boolean NOT NULL,
   wednesday boolean NOT NULL,
   thursday boolean NOT NULL,
   friday boolean NOT NULL,
   absence_code VARCHAR(64),
   min_hours double precision,
   latest_start_time time,
   exempt_from_fees boolean NOT NULL,
   CONSTRAINT pk_attendance_rule PRIMARY KEY(id),
   CONSTRAINT fk_attendance_rule_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_attendance_rule_person FOREIGN KEY (person_id) references person(person_id)
);

--- 95.sql

ALTER TABLE organization ADD COLUMN attendance_report_late_fee_2 integer;
ALTER TABLE organization ADD COLUMN attendance_report_late_fee_interval_2 integer;
ALTER TABLE organization ADD COLUMN attendance_report_latest_departure_time_2 time;

--- 96.sql

ALTER TABLE organization ADD COLUMN attendance_day_earliest_departure_time time;
ALTER TABLE attendance_rule ADD COLUMN earliest_departure_time time;

--- 97.sql

ALTER TABLE tag ALTER COLUMN show_in_account_balances SET DEFAULT false;
ALTER TABLE tag ALTER COLUMN show_in_account_balances SET NOT NULL;

--- 98.sql

ALTER TABLE organization ADD COLUMN show_roles boolean NOT NULL DEFAULT false;
ALTER TABLE organization ADD COLUMN roles_individual_term text DEFAULT 'Clerk';
ALTER TABLE organization ADD COLUMN roles_committee_term text DEFAULT 'Committee';
ALTER TABLE organization ADD COLUMN roles_group_term text DEFAULT 'Group';
ALTER TABLE tag ADD COLUMN show_in_roles boolean NOT NULL DEFAULT true;
CREATE TABLE role (
   id serial,
   organization_id integer NOT NULL,
   is_active boolean NOT NULL DEFAULT true,
   type integer NOT NULL,
   eligibility integer NOT NULL,
   name text NOT NULL,
   notes text NOT NULL,
   description text NOT NULL,
   CONSTRAINT pk_role primary key(id),
   CONSTRAINT fk_role_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
);
CREATE TABLE role_record (
   id serial,
   role_id integer NOT NULL,
   role_name text NOT NULL,
   date_created timestamp NOT NULL,
   CONSTRAINT pk_role_record primary key(id),
   CONSTRAINT fk_role_record_role FOREIGN KEY (role_id) REFERENCES role(id)
);
CREATE TABLE role_record_member (
	record_id integer NOT NULL,
	person_id integer,
	person_name text,
	type integer NOT NULL,
	CONSTRAINT fk_role_record_member_record FOREIGN KEY (record_id) REFERENCES role_record(id),
	CONSTRAINT fk_role_record_member_person FOREIGN KEY (person_id) REFERENCES person(person_id)
);

--- 99.sql

ALTER TABLE organization ADD COLUMN attendance_show_rate_in_checkin boolean NOT NULL DEFAULT(false);

--- 100.sql

ALTER TABLE organization ADD COLUMN attendance_default_absence_code text;
ALTER TABLE organization ADD COLUMN attendance_default_absence_code_time time;

--- 101.sql

-- Prepare for migrating custodia columns to DST
ALTER TABLE organization ADD COLUMN	timezone varchar(255) DEFAULT 'America/New_York' NOT NULL;
ALTER TABLE organization ADD COLUMN late_time TIME DEFAULT '10:15:00'::time without time zone NULL;
ALTER TABLE organization ALTER COLUMN late_time DROP DEFAULT;
ALTER TABLE person ADD COLUMN custodia_show_as_absent date NULL;
ALTER TABLE person ADD COLUMN custodia_start_date date NULL;
--- Add some columns for Django user stuff
ALTER TABLE users ADD COLUMN date_joined timestamptz NOT NULL default '2000-01-01';
ALTER TABLE users ADD COLUMN last_login timestamptz NULL default NULL;
ALTER TABLE users ADD COLUMN first_name varchar(150) NOT NULL default '';
ALTER TABLE users ADD COLUMN last_name varchar(150) NOT NULL default '';
ALTER TABLE users ADD COLUMN is_superuser bool NOT NULL default false;
ALTER TABLE users ADD COLUMN is_staff bool NOT NULL default false;
ALTER TABLE users ADD COLUMN username varchar(150) NOT NULL default '';
--- Add primary keys where there were none
ALTER TABLE person_change ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE charge_reference ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE charge_reference ADD CONSTRAINT uk_charge_case UNIQUE (referenced_charge, referencing_case);
ALTER TABLE case_reference ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE case_reference ADD CONSTRAINT uk_case_case UNIQUE (referenced_case, referencing_case);
ALTER TABLE role_record_member ADD COLUMN id SERIAL PRIMARY KEY;
-- Add indexes where there should have been some
create index attendance_day_day_idx on attendance_day ("day");
create index attendance_week_monday_idx on attendance_week ("monday");
