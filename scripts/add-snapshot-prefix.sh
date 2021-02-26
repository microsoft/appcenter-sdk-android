#!/bin/bash

# Description: Add a `SNAPSHOT` prefix for `versionName` in `versions.gradle`.
# Usage: ./scripts/add-snapshot-prefix.sh

# Compute new version
 gradleVersion=`egrep "versionName = '(.*)'" *.gradle | sed -E "s/^.*versionName = '(.*)'.*$/\1/"`
baseGradleVersion=$(echo $gradleVersion| cut -d'-' -f 1)
newVersion=$baseGradleVersion-"SNAPSHOT"

 if [[ "$USE_HASH" == "true" ]]
 then
  newVersion+="+$(git rev-parse --short HEAD)"
fi
# Replace version in the file
sed -E -i '' "s#(versionName = ')(.*)'#\1$newVersion'#" *.gradle
# Print version on build logs
grep "versionName = '" *.gradle
