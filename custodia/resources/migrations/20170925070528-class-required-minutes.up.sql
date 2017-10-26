ALTER TABLE overseer.classes
ADD COLUMN required_minutes int NOT NULL DEFAULT 345;
--;;
UPDATE overseer.classes SET required_minutes = 345;

