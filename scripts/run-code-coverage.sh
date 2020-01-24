#!/bin/bash

# We can't run emulator as a daemon
# VSTS will not execute next step until emulator killed
# So we need to run tests in same step...
export DYLD_LIBRARY_PATH="$ANDROID_HOME/emulator/lib64/qt/lib"
$ANDROID_HOME/emulator/emulator -avd emulator -skin 768x1280 -no-window -gpu off &

# Ensure Android Emulator has booted successfully before continuing
EMU_BOOTED='unknown'
while [[ ${EMU_BOOTED} != *"stopped"* ]]; do
    echo "Waiting emulator to start..."
    sleep 5
    EMU_BOOTED=`adb shell getprop init.svc.bootanim || echo unknown`
done
duration=$(( SECONDS - start ))
echo "Android Emulator started after $duration seconds."

# Convert VSTS variables to coveralls for reporting status on github.
if [[ "$LOGNAME" -eq "vsts" ]];
then
    export CI_NAME="vsts"

    # For pull request we need to expose the number to the gradle plugin.
    if [[ $BUILD_SOURCEBRANCH =~ ^refs/pull/[0-9]+/merge$ ]]
    then
        export CI_PULL_REQUEST=`sed -E 's#refs/pull/([0-9]+)/merge#\1#' <<< $BUILD_SOURCEBRANCH`
    fi

    # For branches (after merging), we need to attach to branch name in git
    # VSTS checkouts in detached mode
    # and the branch env variable does not work with the gradle plugin...
    if [[ $BUILD_SOURCEBRANCH =~ ^refs/heads/.*$ ]]
    then
        BRANCH=`sed -E 's#refs/heads/(.*)#\1#' <<< $BUILD_SOURCEBRANCH`
        git checkout -b $BRANCH
    fi
fi

# Run tests with coverage
if [ -z $1 ]
then

    # Using env variable COVERALLS_REPO_TOKEN if set, this will not fail process unset.
    ./gradlew --parallel coveralls
else

    # Expose variable just for this run based on script parameter.
    COVERALLS_REPO_TOKEN=$1 ./gradlew --parallel coveralls
fi
EXIT_CODE=$?

# And kill emulator
adb emu kill

# use gradle exit code, we can't use set -e as we need to kill emulator.
# also if starting emulator fails, gradle will fail, so we can just test gradle.
exit $EXIT_CODE
