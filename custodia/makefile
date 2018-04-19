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

# setup-prod-demo-data :
# 	heroku pg:psql --app shining-overseer < demo/demo-data.sql

log-philly :
	heroku logs --app shining-overseer

sql-philly :
	heroku pg:psql --app shining-overseer

sql-philly-backup :
	heroku pg:backups capture --app shining-overseer
	curl -o latest.dump `heroku pg:backups public-url -a shining-overseer`

sql-backup-local-restore : drop-tables
	pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d school_crm latest.dump

generate-sequence-reset :
	psql -h localhost -U postgres -d school_crm -Atq -f reset.sql -o genreset.sql

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
# grant all privileges on database school_crm to jack;
# ALTER USER jack WITH SUPERUSER;
# insert into classes_X_students (class_id, student_id) select 1, _id from students;
drop-tables :
	psql -d school_crm -c "DROP SCHEMA IF EXISTS phillyfreeschool CASCADE; DROP SCHEMA IF EXISTS overseer CASCADE; DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"

sql-local :
	psql -d school_crm

load-massive-dump : drop-tables
	psql school_crm < massive.dump

load-aliased-dump : drop-tables
	psql school_crm < dumps/updated-students-aliased.dump

backup-aliased-dump :
	pg_dump school_crm > dumps/updated-students-aliased.dump


BABEL_CMD = -t [ babelify --presets [ es2015 react stage-1 ] ]

minify :
	./node_modules/.bin/browserify $(BABEL_CMD) -t reactify -t uglifyify ./src/js/app.jsx -o ./resources/public/js/gen/app.js

js :
	./node_modules/.bin/browserify $(BABEL_CMD) ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

watch :
	./node_modules/.bin/watchify -v $(BABEL_CMD) ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug
