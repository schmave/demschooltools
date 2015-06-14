# --- !Ups

ALTER TABLE charge ADD COLUMN rp_complete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE charge ADD COLUMN rp_complete_date timestamp;

UPDATE charge SET rp_complete = TRUE;
UPDATE charge SET rp_complete_date = meeting.DATE FROM "case", meeting
    WHERE charge.case_id="case".id AND "case".meeting_id=meeting.id;

# --- !Downs

ALTER TABLE charge DROP COLUMN rp_complete;
ALTER TABLE charge DROP COLUMN rp_complete_date;
