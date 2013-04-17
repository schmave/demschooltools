# --- !Ups


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

# --- !Downs

DROP TABLE IF EXISTS completed_task;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS task_list;

