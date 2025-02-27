#!/bin/bash

set -ex

npm install
./sbt.sh clean
./sbt.sh dist

(cd custodia && npm install && npm run compile)


git ls-tree -r --name-only head django | xargs tar cfz django

# TODO: Add generated JS files and index.html from Custodia build step

# scp target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/
# scp django.tgz evan@demschooltools.com:/home/evan/
