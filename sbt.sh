#!/bin/bash
set -ex

. set_keys_blank.sh
PATH="~/jdk-11.0.2.jdk/Contents/Home/bin/:$PATH" /usr/local/Cellar/sbt/1.8.2/libexec/bin/sbt $@
