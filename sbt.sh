#!/bin/bash
set -e

JAVA_VERSION=11

. set_keys_blank.sh

if [ -z $JAVA_HOME ]
then
    echo
    echo "JAVA_HOME is not set. Please set it to a JDK $JAVA_VERSION directory."
    echo "If you have a JDK downloaded to your home directory, JAVA_HOME might be something like:"
    echo "   export JAVA_HOME=~/jdk-11.0.18.jdk/Contents/Home"
    echo
    exit 1
fi

if [ ! -x $JAVA_HOME/bin/java ]
then
    echo
    echo '$JAVA_HOME/bin/java' "($JAVA_HOME/bin/java)" is not an executable file.
    echo Check your JAVA_HOME setting and try again.
    echo
    exit 1
fi


$JAVA_HOME/bin/java --version | grep "$JAVA_VERSION.0" \
    || (echo "Java $JAVA_VERSION not found in JAVA_HOME!"; exit 1)

HOMEBREW_PREFIX="${HOMEBREW_PREFIX:-/usr/local}"
$HOMEBREW_PREFIX/bin/sbt $@