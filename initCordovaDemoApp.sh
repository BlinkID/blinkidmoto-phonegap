#!/bin/bash

# position to a relative path
HERE="$(dirname "$(test -L "$0" && readlink "$0" || echo "$0")")"
pushd $HERE >> /dev/null

# remove any existing code
rm -rf BlinkMotoDemo

# create a sample application
cordova create BlinkMotoDemo com.microblink.blinkmoto BlinkMotoDemo

# enter into demo project folder
cd BlinkMotoDemo

# add the BlinkMoto plugin
cordova plugin add ../BlinkMoto --variable CAMERA_USAGE_DESCRIPTION="Camera permission is required for automated scanning"

# add android and ios support to the project
cordova platform add ios
cordova platform add android@6

# copy content of the www folder
cp  -f -r ../www .

# build app
cordova build

# how to run
echo "To run iOS demo application open Xcode project BlinkMotoDemo.xcodeproj"
echo "To run Android demo application, position to BlinkMotoDemo folder and type: cordova run android"
