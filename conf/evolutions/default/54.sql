# --- !Ups

CREATE TABLE user_role (
   id serial,
   user_id INTEGER NOT NULL,
   role text NOT NULL,
   CONSTRAINT pk_user_role primary key(id),
   CONSTRAINT fk_user_role_users foreign key(user_id) references users(id)
);

INSERT INTO user_role(user_id, role) (SELECT id, 'all-access' FROM users);

ALTER table users ALTER COLUMN id TYPE INTEGER;

ALTER TABLE users ALTER COLUMN organization_id DROP NOT NULL;

# --- !Downs

DROP TABLE user_role;
ALTER TABLE users ALTER COLUMN id TYPE bigint;
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
