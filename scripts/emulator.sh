export DYLD_LIBRARY_PATH="$ANDROID_HOME/emulator/lib64/qt/lib"
$ANDROID_HOME/emulator/emulator64-arm -avd emulator -skin 768x1280 -no-boot-anim -no-window -gpu off
