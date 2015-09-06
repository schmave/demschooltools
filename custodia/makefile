philly-prod-git := git@heroku.com:shining-overseer.git
web-test-git := git@heroku.com:shining-overseer-test.git

T :
	@echo deploy-test
	@echo deploy-philly
	@echo unit-test
	@echo "sql-philly - connect to philly postgres prod database"
	@echo webdriver-test
	@echo drop-tables
	@echo load-massive-dump
	@echo load-aliased-dump
	@echo sql-backup-local-restore
	@echo "sql-philly-backup - make backup of philly prod database"
	@echo "start - start a running overseer site"
	@echo minify
	@echo js
	@echo watch
	@echo sjs
	@echo sjswatch

# example of ls and echo
hello : 
	ls
	@echo Done

deploy-test : 
	git push $(web-test-git) master

sql-philly : 
	heroku pg:psql --app shining-overseer

sql-philly-backup : 
	heroku pg:backups capture
	curl -o latest.dump `heroku pg:backups public-url -a shining-overseer`

sql-backup-local-restore :
	pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d swipes latest.dump

deploy-philly :
	./prod-deploy.sh $(philly-prod-git) 

start :
	lein run -m overseer.web

debug :
	lein with-profile debug run -m overseer.web

unit-test :
	lein test

webdriver-test :
	lein test :integration

drop-tables :
	psql -d swipes -c "drop table if exists session_store; drop table if exists students; drop table if exists excuses; drop table if exists overrides; drop table if exists years; drop table if exists swipes;"

load-massive-dump : drop-tables
	psql swipes < massive.dump

load-aliased-dump : drop-tables
	psql swipes < dumps/migrated-students-aliased.dump

minify :
	browserify -t reactify -t uglifyify ./src/js/app.jsx -o ./resources/public/js/gen/app.js

js :
	browserify -t reactify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

watch :
	watchify -v -t reactify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

sjs :
	nodejs /usr/local/bin/browserify -t reactify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug

sjswatch :
	nodejs /usr/local/bin/watchify -t reactify ./src/js/app.jsx -o ./resources/public/js/gen/app.js --debug
