# --- !Ups

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
# --- !Downs

drop table entry;
drop table section;
drop table chapter;
