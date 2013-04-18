# --- !Ups

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

# --- !Downs

alter table person drop column created;
