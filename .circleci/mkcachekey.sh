#!/bin/bash
set -e

FILELIST=/tmp/compose-cache-files-gradle
KEYFILE=/tmp/compose-cache-key-gradle

rm -f $FILELIST
(
    echo gradle/wrapper/gradle-wrapper.properties
    find . -name '*.gradle' -type f | grep -v /test-project/
    echo gradle.properties
) >> $FILELIST

sha1sum >$KEYFILE $(sort $FILELIST)
