#!/bin/bash
set -e

AZURE_STORAGE_ACCESS_KEY=${1:-$AZURE_STORAGE_ACCESS_KEY}

BINARY_FILE_FILTER="*.aar"
FULL_VERSION="$(grep "versionName = '" *.gradle | awk -F "[']" '{print $2}')"
PUBLISH_VERSION=`echo $FULL_VERSION | cut -d'-' -f 1`
COMMIT_VERSION=`git show -s --format=%h`
ARCHIVE=AppCenter-SDK-Android-${PUBLISH_VERSION}-${COMMIT_VERSION}
ZIP_FILE=$ARCHIVE.zip

# Copy release aar files from sdk
FILES="$(find sdk -name $BINARY_FILE_FILTER)"
for file in $FILES
do
    echo "Found binary" $file
    cp $file $BUILD_ARTIFACTSTAGINGDIRECTORY
done

# Zip them
cd $BUILD_ARTIFACTSTAGINGDIRECTORY
mkdir $ARCHIVE
cp $BINARY_FILE_FILTER $ARCHIVE
zip -r $ZIP_FILE $ARCHIVE
cd -

# Upload file
AZURE_STORAGE_ACCESS_KEY=$AZURE_STORAGE_ACCESS_KEY \
azure storage blob upload $BUILD_ARTIFACTSTAGINGDIRECTORY/$ZIP_FILE sdk
