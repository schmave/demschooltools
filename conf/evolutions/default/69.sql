# --- !Ups

CREATE TABLE transactions (
   id serial,
   organization_id integer NOT NULL,
   from_account_id integer,
   to_account_id integer,
   from_name text NOT NULL,
   to_name text NOT NULL,
   description text NOT NULL,
   type integer NOT NULL,
   amount decimal NOT NULL,
   date_created timestamp NOT NULL,
   CONSTRAINT pk_transaction primary key(id),
   CONSTRAINT fk_transaction_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_transaction_from_account FOREIGN KEY (from_account_id) references account(id),
   CONSTRAINT fk_transaction_to_account FOREIGN KEY (to_account_id) references account(id)
);

# --- !Downs

DROP TABLE transaction;