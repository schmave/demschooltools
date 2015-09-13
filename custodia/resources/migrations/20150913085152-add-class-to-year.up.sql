INSERT INTO classes (name, active) VALUES ('2014-2015', true);

-- ;;

ALTER TABLE years
ADD COLUMN class_id BIGINT NOT NULL REFERENCES classes(_id) DEFAULT 1;

-- ;;

ALTER TABLE years
ALTER COLUMN class_id DROP DEFAULT;
