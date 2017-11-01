#!/bin/bash
set -e

# Compute new version
gradleVersion=`egrep "versionName = '(.*)'" *.gradle | sed -r "s/^.*versionName = '(.*)'.*$/\1/"`

bintrayRelease=`curl -s https://api.bintray.com/packages/${BINTRAY_USER_ORG}/${BINTRAY_REPO}/appcenter --user $BINTRAY_USER:$BINTRAY_KEY`
bintrayVersion=`sed -r 's/^.*"latest_version":"([^"]+)",.*$/\1/' <<< "$bintrayRelease"`

bintrayBaseVersion=`sed -r 's/^(([0-9]+\.){2}[0-9]+).*$/\1/' <<< "$bintrayVersion"`

bintrayBuildNumber=`sed -r 's/^([0-9]+\.){2}[0-9]+-([0-9]+).*$/\2/' <<< "$bintrayVersion"`

if [[ "$gradleVersion" != "$bintrayBaseVersion" ]] || [[ "$bintrayVersion" == "$bintrayBuildNumber" ]]
then
  buildNumber=0
else
  buildNumber=$((bintrayBuildNumber+1))
fi
newVersion=$gradleVersion-$buildNumber

if [[ "$1" == "--use-hash" ]]
then
  newVersion+="+$(git rev-parse --short HEAD)"
fi

# Replace version in the file
sed -i -b -r "s#(versionName = ')(.*)'#\1$newVersion'#" *.gradle

# Print version on build logs
grep "versionName = '" *.gradle
