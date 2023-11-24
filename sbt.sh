#!/bin/bash
set -ex

. set_keys_blank.sh
PATH="~/jdk-11.0.2.jdk/Contents/Home/bin/:$PATH" /usr/local/bin/sbt $@
