#!/usr/bin/env bash
echo "Executing post clone script in `pwd`"
echo $GOOGLE_SERVICES_JSON | base64 -D > $APPCENTER_SOURCE_DIRECTORY/apps/sasquatch/google-services.json
