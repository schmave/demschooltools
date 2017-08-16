ifeq ($(OS),Windows_NT)
    LEIN=lein.bat
else
    LEIN=lein
endif

philly-prod-git := git@heroku.com:shining-overseer.git
web-test-git := git@heroku.com:shining-overseer-test.git

T :
	grep '^[^[:space:]].* :' makefile

# example of ls and echo
hello :
	ls
	@echo Done

deploy-test :
	git push $(web-test-git) master

setup-prod-demo-data :
	heroku pg:psql --app shining-overseer < demo/demo-data.sql

log-philly :
	heroku logs --app shining-overseer

sql-philly :
	heroku pg:psql --app shining-overseer

sql-philly-backup :
	heroku pg:backups capture --app shining-overseer
	curl -o latest.dump `heroku pg:backups public-url -a shining-overseer`

sql-backup-local-restore :
	pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d swipes latest.dump

generate-sequence-reset :
	psql -h localhost -U postgres -d swipes -Atq -f reset.sql -o genreset.sql

run-sequence-reset :
	psql -f genreset.sql

deploy-philly : generate-sequence-reset
	echo 'do you need to run the sequence reset?'
	./prod-deploy.sh $(philly-prod-git)

logs :
	tail -f log/app.log

start :
	${LEIN} run -m overseer.web
    # ${LEIN} ring server-headless 5000

debug :
	${LEIN} with-profile debug run -m overseer.web

unit-test :
	${LEIN} test

webdriver-test :
	${LEIN} test :integration

connect-dst :
	ssh custodia@demschooltools.com -L 5433:localhost:5432

deploy-dst : minify
	${LEIN} uberjar
	scp target/overseer-standalone.jar custodia@demschooltools.com:~/overseer-new.jar
	scp dst_server/* custodia@demschooltools.com:~/
	ssh custodia@demschooltools.com ./run_server.sh

# createuser jack -U postgres
# grant all privileges on database swipes to jack;
# ALTER USER jack WITH SUPERUSER;
# insert into classes_X_students (class_id, student_id) select 1, _id from students;
drop-tables :
	psql -d swipes -c "DROP TABLE IF EXISTS schema_migrations; DROP FUNCTION IF EXISTS phillyfreeschool.school_days(text, bigint); DROP TABLE IF EXISTS phillyfreeschool.users; DROP TABLE IF EXISTS phillyfreeschool.years; DROP TABLE IF EXISTS phillyfreeschool.classes_X_students; DROP TABLE IF EXISTS phillyfreeschool.classes; DROP FUNCTION IF EXISTS phillyfreeschool.school_days(text); DROP VIEW IF EXISTS phillyfreeschool.roundedswipes; DROP TABLE IF EXISTS phillyfreeschool.swipes; DROP TABLE IF EXISTS session_store; DROP TABLE IF EXISTS phillyfreeschool.students; DROP TABLE IF EXISTS phillyfreeschool.excuses; DROP TABLE IF EXISTS phillyfreeschool.overrides; DROP TABLE IF EXISTS years; DROP TABLE IF EXISTS classes_X_students; DROP TABLE IF EXISTS classes; DROP FUNCTION IF EXISTS school_days(text); DROP VIEW IF EXISTS roundedswipes; DROP TABLE IF EXISTS swipes; DROP TABLE IF EXISTS session_store; DROP TABLE IF EXISTS students; DROP TABLE IF EXISTS excuses; DROP TABLE IF EXISTS overrides; "

sql-local :
	psql -d swipes

load-massive-dump : drop-tables
	psql swipes < massive.dump

load-aliased-dump : drop-tables
	psql swipes < dumps/updated-students-aliased.dump

backup-aliased-dump :
	pg_dump swipes > dumps/updated-students-aliased.dump

minify :
	./node_modules/.bin/browserify -t babelify -t reactify -t uglifyify ./src/js/app.jsx -o ./resources/public/js/gen/app.js

js :
	./node_modules/.bin/browserify -t babelify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

watch :
	./node_modules/.bin/watchify -v -t babelify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug
