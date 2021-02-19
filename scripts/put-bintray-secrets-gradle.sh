#!/bin/bash
cat >> local.properties << EOL
maven.user=${1:-$MAVEN_USER}
maven.key=${2:-$MAVEN_KEY}
maven.releasesRepoUrl=${3:-$MAVEN_RELEASE_REPO}
maven.signingKeyId=${4:-$GDP_SIGNING_KEY_ID}
maven.publicKeyPassword=${5:-$GDP_KEY_PASSWORD}
maven.secretKeyPath=${6:-$GDP_KEY_SECRET_PATH}
EOL
