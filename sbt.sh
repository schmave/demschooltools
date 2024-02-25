#!/bin/bash
set -ex

. set_keys_blank.sh
JDK_11_PATH="~/jdk-11.0.2.jdk/Contents/Home/bin/:$PATH"

PATH=$JDK_11_PATH java --version | grep '11.0' || (echo "Java 11 not found! sbt will probably fail"; exit 1)

PATH=$JDK_11_PATH $HOMEBREW_PREFIX/bin/sbt $@
