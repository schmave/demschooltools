philly-prod-git := git@heroku.com:shining-overseer.git
web-test-git := git@heroku.com:shining-overseer-test.git

T : 
	@echo deploy-test
	@echo deploy-philly
	@echo unit-test
	@echo webdriver-test

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
