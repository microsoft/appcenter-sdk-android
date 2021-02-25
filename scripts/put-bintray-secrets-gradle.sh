#!/bin/bash
cat >> local.properties << EOL
maven.user=${1:-$MAVEN_USER}
maven.key=${2:-$MAVEN_KEY}
maven.repoUrl=${3:-$MAVEN_REPO}
maven.signingKeyId=${5:-$GDP_SIGNING_KEY_ID}
maven.secretKeyPath=${6:-$GDP_KEY_SECRET_PATH}
maven.publicKeyPassword=${7:-$GDP_KEY_PASSWORD}
EOL
