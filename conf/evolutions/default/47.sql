# --- !Ups

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

# --- !Downs

DROP TABLE case_meeting;
ALTER TABLE "case" DROP COLUMN date_closed;
