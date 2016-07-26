#!/bin/sh
# This file is Public Domain. Use it as you wish.

DSMP_HOME=/tmp/dsmp

java -classpath ${DSMP_HOME}/conf:target/dsmp-1.1-jar-with-dependencies.jar de.pdark.dsmp.Main ${DSMP_HOME}
