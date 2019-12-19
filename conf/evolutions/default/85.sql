# --- !Ups
ALTER TABLE account ADD COLUMN is_active boolean NOT NULL DEFAULT true;

# --- !Downs
ALTER TABLE account DROP COLUMN is_active;
