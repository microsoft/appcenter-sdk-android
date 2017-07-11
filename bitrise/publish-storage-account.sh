#!/bin/bash
set -e

BINARY_FILE_FILTER="*release.aar"
PUBLISH_VERSION="$(grep "versionName = '" *.gradle | awk -F "[']" '{print $2}')"
ARCHIVE=MobileCenter-SDK-Android-${PUBLISH_VERSION}
ZIP_FILE=$ARCHIVE.zip

# Copy release aar files from sdk
FILES="$(find sdk -name $BINARY_FILE_FILTER)"
for file in $FILES
do
    echo "Found binary" $file
    cp $file $BITRISE_DEPLOY_DIR
done

# Zip them
cd $BITRISE_DEPLOY_DIR
mkdir $ARCHIVE
cp $BINARY_FILE_FILTER $ARCHIVE
zip -r $ZIP_FILE $ARCHIVE

# Upload file
$1azure storage blob upload $ZIP_FILE sdk
