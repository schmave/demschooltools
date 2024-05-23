ifeq ($(OS),Windows_NT)
    LEIN=lein.bat
else
    LEIN=lein
endif

T :
	grep '^[^[:space:]].* :' makefile

sql-backup-local-restore : drop-tables
	pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d school_crm latest.dump

generate-sequence-reset :
	psql -h localhost -U postgres -d school_crm -Atq -f reset.sql -o genreset.sql

run-sequence-reset :
	psql -f genreset.sql

logs :
	tail -f log/app.log

start :
	LEIN_USE_BOOTCLASSPATH=no ${LEIN} with-profile dev run -m overseer.web

unit-test :
	${LEIN} test

test-refresh :
	${LEIN} test-refresh

webdriver-test :
	${LEIN} test :integration

connect-dst :
	ssh custodia@demschooltools.com -L 5433:localhost:5432

deploy-dst : minify
	${LEIN} uberjar
	scp target/overseer-standalone.jar custodia@demschooltools.com:~/overseer-new.jar
	scp dst_server/* custodia@demschooltools.com:~/
	ssh custodia@demschooltools.com ./run_server.sh

drop-tables :
	psql -d school_crm -c "DROP SCHEMA IF EXISTS phillyfreeschool CASCADE; DROP SCHEMA IF EXISTS overseer CASCADE; DROP TABLE IF EXISTS public.emails; DROP TABLE if EXISTS public.schema_migrations; DROP TABLE IF EXISTS public.session_store;"

BABEL_CMD = -t [ babelify --presets [ es2015 react stage-1 ] ]

minify :
	./node_modules/.bin/browserify $(BABEL_CMD) -t reactify -t uglifyify ./src/js/app.jsx -o ./resources/public/js/gen/app.js

js :
	./node_modules/.bin/browserify $(BABEL_CMD) ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

watch :
	./node_modules/.bin/watchify -v $(BABEL_CMD) ./src/js/app.jsx -o ../django/static/js/gen/app.js --debug
	# ./node_modules/.bin/watchify -v $(BABEL_CMD) ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug
