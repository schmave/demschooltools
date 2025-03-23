#!/bin/bash

set -ex

# Make sure that there are no uncommitted changes
git diff-index --quiet HEAD --

### Play Framework stuff
npm install
./sbt.sh clean dist
rsync -v -h --progress target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/


## Custodia & Django stuff
rm -rf custodia/dist
(cd custodia && npm install && npm run compile)

git ls-tree -r --name-only head django | xargs zip django

mkdir -p django/static/js
cp custodia/dist/*.js django/static/js/
cp custodia/dist/index.html django/custodia/templates/
zip django django/static/js/*.js
zip django django/custodia/templates/index.html

scp django.zip evan@demschooltools.com:/home/evan/

rm django.zip
git clean -f django/static/js
git checkout -- django/custodia/templates/index.html