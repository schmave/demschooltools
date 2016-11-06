# --- !Ups

CREATE MATERIALIZED VIEW entry_index AS
SELECT entry.id,
       chapter.organization_id,
       (to_tsvector(entry.content) || to_tsvector(entry.title)) as document
FROM entry
JOIN section ON entry.section_id = section.id
JOIN chapter ON section.chapter_id = chapter.id
WHERE entry.deleted=false AND section.deleted=false AND chapter.deleted=false;

CREATE INDEX idx_entry_index ON entry_index USING gin(document);

# --- !Downs

DROP INDEX idx_entry_index;
DROP MATERIALIZED VIEW entry_index;
