ALTER TABLE emails set schema public;

--;;

ALTER TABLE session_store set schema public;

--;;

ALTER TABLE overseer.students DROP COLUMN dst_id;

--;;

ALTER TABLE overseer.students DROP CONSTRAINT stu_uniq_dst_id;
