#!/bin/bash
set -ex

. set_keys_blank.sh
export JAVA_HOME=~/jdk-11.0.18.jdk/Contents/Home
JDK_11_PATH="$JAVA_HOME/bin:$PATH"

PATH=$JDK_11_PATH java --version | grep '11.0' || (echo "Java 11 not found! sbt will probably fail"; exit 1)

HOMEBREW_PREFIX="${HOMEBREW_PREFIX:-/usr/local}"
$HOMEBREW_PREFIX/bin/sbt $@