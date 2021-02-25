#!/bin/bash
cat >> local.properties << EOL
maven.user=${1:-$MAVEN_USER}
maven.key=${2:-$MAVEN_KEY}
maven.releasesRepoUrl=${3:-$MAVEN_RELEASE_REPO}
maven.snapshotRepoUrl=${4:-$MAVEN_SNAPSHOT_REPO}
maven.signingKeyId=${5:-$GDP_SIGNING_KEY_ID}
maven.secretKeyPath=${6:-$GDP_KEY_SECRET_PATH}
maven.publicKeyPassword=${7:-$GDP_KEY_PASSWORD}
EOL
