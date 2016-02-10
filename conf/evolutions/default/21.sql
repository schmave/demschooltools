# --- !Ups

CREATE TABLE allowed_ips (ip VARCHAR(30) primary key);
INSERT INTO allowed_ips (ip) VALUES('127.0.0.1');

# --- !Downs

DROP TABLE allowed_ips;
