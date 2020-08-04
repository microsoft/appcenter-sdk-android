#!/bin/bash
set -e

function getProperty {
   PROP_KEY=$1
   PROP_VALUE=`cat $PROPERTY_FILE | sed 's/ //g' | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}
PROJECT_DIR="$(dirname "$0")/.."
PROPERTY_FILE="$HOME/Library/Android/sdk/ndk-bundle/source.properties"

echo "Reading ndk version from source.properties file..."
VERSION=$(getProperty "Pkg.Revision")
echo $VERSION
GRADLE_FILE="$PROJECT_DIR/apps/sasquatch/build.gradle"

if [ -z $VERSION ]; then
  echo "No NDK found in the default location. Proceeding..."
else
  NDK_VERSION_LINE="ndkVersion = '$VERSION'"
  #sed -a '' 's/\(android { \).*/\1'$NDK_VERSION_LINE'/g' $file
  echo "$(sed "s/android {/android { \\`echo -e '\n\r\t'` $NDK_VERSION_LINE/g" "$GRADLE_FILE")" > $GRADLE_FILE
fi
