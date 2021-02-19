#!/bin/bash
set -e

# Command line arguments
while getopts ":hu:p:" opt; do
  case $opt in
    h)
      USE_HASH=true >&2
      ;;
    u)
      MAVEN_USER=$OPTARG >&2
      ;;
    p)
      MAVEN_KEY=$OPTARG >&2
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

# Download metadata.xml file.
# TODO use $MAVEN_RELEASE_REPO instead.
mavenRelease=`curl -s "http://localhost:8081/repository/maven-releases/com/microsoft/appcenter/appcenter/maven-metadata.xml"`

# Get version of the lates release.
mavenVersion=`echo $mavenRelease | sed -e 's/.*<release>\(.*\)<\/release>.*/\1/'`
mavenBaseVersion=$(echo $mavenVersion| cut -d'-' -f 1)

# Get build version.
mavenBuildNumber=$(echo $mavenVersion| cut -d'-' -f 2)

# Get base gradle version.
baseGradleVersion=$(echo $gradleVersion| cut -d'-' -f 1)
if [[ "$gradleVersion" != "$mavenVersion" ]] || [[ "$mavenBaseVersion" == "$mavenBuildNumber" ]]
then
  buildNumber=0
else
  buildNumber=$((mavenBuildNumber+1))
fi
newVersion=$baseGradleVersion-$buildNumber

if [[ "$USE_HASH" == "true" ]]
then
  newVersion+="+$(git rev-parse --short HEAD)"
fi

# Replace version in the file
sed -E -i '' "s#(versionName = ')(.*)'#\1$newVersion'#" *.gradle

# Print version on build logs
grep "versionName = '" *.gradle
