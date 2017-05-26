# --- !Ups

ALTER TABLE tag ADD COLUMN show_in_jc boolean DEFAULT false NOT NULL;

UPDATE tag set show_in_jc=(title='Staff' or title='Current Student');

ALTER TABLE tag add column show_in_menu boolean default true not null;


# --- !Downs

ALTER TABLE tag DROP show_in_menu;
ALTER TABLE tag DROP show_in_jc;
