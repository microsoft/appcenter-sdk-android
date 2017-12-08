#!/usr/bin/env bash
echo "Executing post clone script in `pwd`"
echo "dimension.dependency=$DIMENSION_DEPENDENCY" >> $APPCENTER_SOURCE_DIRECTORY/local.properties
