#!/bin/bash
cd ~/.gradle && cat >> gradle.properties << EOL
android.useAndroidX=true
EOL