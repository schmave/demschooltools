philly-prod-git := git@heroku.com:shining-overseer.git
web-test-git := git@heroku.com:shining-overseer-test.git

T : 
	@echo deploy-test
	@echo deploy-philly
	@echo unit-test
	@echo sql-philly
	@echo webdriver-test
	@echo drop-tables
	@echo load-massive-dump
	@echo load-aliased-dump
	@echo sql-backup-local-restore
	@echo sql-philly-backup
	@echo start

# example of ls and echo
hello : 
	ls
	@echo Done

deploy-test : unit-test
	git push $(web-test-git) master

sql-philly : 
	heroku pg:psql --app shining-overseer

sql-philly-backup : 
	heroku pg:backups capture
	curl -o latest.dump `heroku pg:backups public-url -a shining-overseer`

sql-backup-local-restore :
	pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d swipes latest.dump

deploy-philly : unit-test
	./prod-deploy.sh $(philly-prod-git) 

start :
	lein run -m overseer.web

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
