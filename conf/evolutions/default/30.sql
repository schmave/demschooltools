# --- !Ups

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


# --- !Downs

DROP TABLE manual_change;
