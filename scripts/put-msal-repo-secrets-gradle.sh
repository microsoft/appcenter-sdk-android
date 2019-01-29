#!/bin/bash
cat >> local.properties << EOL
azure.artifacts.gradle.access.token=${1:-$AZURE_ARTIFACTS_ENV_ACCESS_TOKEN}
EOL
