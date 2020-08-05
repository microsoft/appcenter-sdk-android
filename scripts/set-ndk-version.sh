#!/bin/bash

# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

PROJECT_DIR="$(dirname "$0")/.."
PROPERTY_FILE="$ANDROID_HOME/ndk-bundle/source.properties"

echo "Reading ndk version from source.properties file..."
VERSION=$(cat $PROPERTY_FILE | sed 's/ //g' | grep "Pkg.Revision" | cut -d'=' -f2)
echo $VERSION
GRADLE_FILE="$PROJECT_DIR/apps/sasquatch/build.gradle"

if [ -z $VERSION ]; then
  echo "No NDK found in the default location. Proceeding..."
else

  # Insert ndkVersion = 'x.x.x' in the android section.
  NDK_VERSION_LINE="ndkVersion = '$VERSION'"
  echo "$(sed "s/android {/android { \\`echo -e '\n\r\t'` $NDK_VERSION_LINE/g" "$GRADLE_FILE")" > $GRADLE_FILE
fi
