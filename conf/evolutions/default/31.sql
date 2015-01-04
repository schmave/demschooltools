# --- !Ups

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

# --- !Downs

ALTER TABLE allowed_ips DROP COLUMN organization_id;
ALTER TABLE chapter DROP COLUMN organization_id;
ALTER TABLE email DROP COLUMN organization_id;
ALTER TABLE meeting DROP COLUMN organization_id;
ALTER TABLE person DROP COLUMN organization_id;
ALTER TABLE tag DROP COLUMN organization_id;
ALTER TABLE task_list DROP COLUMN organization_id;
ALTER TABLE users DROP COLUMN organization_id;

DROP TABLE organization_hosts;
DROP TABLE organization;

