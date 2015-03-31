philly-prod-git := git@heroku.com:shining-overseer.git
web-test-git := git@heroku.com:shining-overseer-test.git

T : 
	@echo deploy-test
	@echo deploy-philly
	@echo unit-test
	@echo webdriver-test
	@echo drop-tables
	@echo load-massive-dump
	@echo load-aliased-dump

# example of ls and echo
hello : 
	ls
	@echo Done

deploy-test :
	git push $(web-test-git) master

deploy-philly :
	./prod-deploy.sh $(philly-prod-git) 

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
