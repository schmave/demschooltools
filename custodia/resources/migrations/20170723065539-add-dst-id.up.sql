ALTER TABLE emails set schema overseer;

--;;

ALTER TABLE session_store set schema overseer;

--;;

ALTER TABLE users set schema overseer;

--;;

ALTER TABLE overseer.students ADD COLUMN dst_id int;

--;;

ALTER TABLE overseer.students ADD CONSTRAINT stu_uniq_dst_id unique(dst_id);
