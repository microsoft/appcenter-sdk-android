#!/bin/bash
set -e

# Command line arguments
while getopts ":hu:p:" opt; do
  case $opt in
    h)
      USE_HASH=true >&2
      ;;
    u)
      BINTRAY_USER=$OPTARG >&2
      ;;
    p)
      BINTRAY_KEY=$OPTARG >&2
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

# Compute new version
gradleVersion=`egrep "versionName = '(.*)'" *.gradle | sed -E "s/^.*versionName = '(.*)'.*$/\1/"`

bintrayRelease=`curl -s https://api.bintray.com/packages/${BINTRAY_USER_ORG}/${BINTRAY_REPO}/appcenter --user $BINTRAY_USER:$BINTRAY_KEY`
bintrayVersion=`sed -E 's/^.*"latest_version":"([^"]+)",.*$/\1/' <<< "$bintrayRelease"`

bintrayBaseVersion=`sed -E 's/^(([0-9]+\.){2}[0-9]+).*$/\1/' <<< "$bintrayVersion"`

bintrayBuildNumber=`sed -E 's/^([0-9]+\.){2}[0-9]+-([0-9]+).*$/\2/' <<< "$bintrayVersion"`

if [[ "$gradleVersion" != "$bintrayBaseVersion" ]] || [[ "$bintrayVersion" == "$bintrayBuildNumber" ]]
then
  buildNumber=0
else
  buildNumber=$((bintrayBuildNumber+1))
fi
newVersion=$gradleVersion-$buildNumber

if [[ "$USE_HASH" == "true" ]]
then
  newVersion+="+$(git rev-parse --short HEAD)"
fi

# Replace version in the file
sed -E -i '' "s#(versionName = ')(.*)'#\1$newVersion'#" *.gradle

# Print version on build logs
grep "versionName = '" *.gradle
