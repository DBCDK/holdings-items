#!/bin/bash

exec java ${JAVA_OPTS} -jar /usr/local/share/jar/holdings-items-purge-tool-jar-with-dependencies.jar "$@"
