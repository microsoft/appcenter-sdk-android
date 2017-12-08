#!/bin/bash

# We can't run emulator as a daemon
# VSTS will not execute next step until emulator killed
# So we need to run tests in same step...
export DYLD_LIBRARY_PATH="$ANDROID_HOME/emulator/lib64/qt/lib"
$ANDROID_HOME/emulator/emulator64-arm -avd emulator -skin 768x1280 -no-window -gpu off &

# Ensure Android Emulator has booted successfully before continuing
EMU_BOOTED='unknown'
while [[ ${EMU_BOOTED} != *"stopped"* ]]; do
    echo "Waiting emulator to start..."
    sleep 5
    EMU_BOOTED=`adb shell getprop init.svc.bootanim || echo unknown`
done
duration=$(( SECONDS - start ))
echo "Android Emulator started after $duration seconds."

# Run tests now
./gradlew coverageReport
EXIT_CODE=$?

# And kill emulator
adb emu kill

# use gradle exit code, we can't use set -e as we need to kill emulator.
# also if starting emulator fails, gradle will fail, so we can just test gradle.
exit $EXIT_CODE
