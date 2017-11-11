#!/usr/bin/env bash
echo "Executing post clone script in `pwd`"
echo $GOOGLE_SERVICES_JSON | base64 -D > sasquatch/google-services.json
