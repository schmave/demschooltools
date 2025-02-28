#!/bin/bash

set -ex

npm install
./sbt.sh clean dist

rm -rf custodia/dist
(cd custodia && npm install && npm run compile)

git ls-tree -r --name-only head django | xargs zip django

cp custodia/dist/*.js django/static/js/
cp custodia/dist/index.html django/custodia/templates/
zip django django/static/js/*.js
zip django django/custodia/templates/index.html

rm django.zip
git clean -f django/static/js
git checkout -- django/custodia/templates/index.html

rsync -v -h --progress target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/
# scp target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/
scp django.zip evan@demschooltools.com:/home/evan/
