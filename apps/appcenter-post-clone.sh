#!/usr/bin/env bash
echo "Executing post clone script in `pwd`"
echo "dimension.dependency=$DIMENSION_DEPENDENCY" >> $APPCENTER_SOURCE_DIRECTORY/local.properties
echo "azure.artifacts.gradle.access.token=$AZURE_ARTIFACTS_GRADLE_ACCESS_TOKEN" >> $APPCENTER_SOURCE_DIRECTORY/local.properties
