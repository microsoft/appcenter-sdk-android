#!/usr/bin/env bash
echo "Executing post clone script in `pwd`"
echo "dimension.dependency=$DIMENSION_DEPENDENCY" >> $APPCENTER_SOURCE_DIRECTORY/local.properties
../scripts/set-ndk-version.sh
cd $APPCENTER_SOURCE_DIRECTORY && ./scripts/put-azure-credentials.sh $AZURE_USERNAME $AZURE_PASSWORD