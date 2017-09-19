## Sample

Here's a complete example of how to create and build a project using cordova (you can substitute equivalent commands for phonegap):


```
# pull the plugin and sample application from Github
git clone git@github.com:BlinkID/blinkidmoto-phonegap.git

# create a empty application
cordova create BlinkMotoDemo com.microblink.blinkmoto BlinkMotoDemo

cd BlinkMotoDemo

# add the BlinkMoto plugin
cordova plugin add ../BlinkMoto --variable CAMERA_USAGE_DESCRIPTION="Camera permission is required for automated scanning"

# add android and ios support to the project
cordova platform add ios
cordova platform add android

# copy content of the www folder
cp  -f -r ../www .

# build app
cordova build
```

## Trying

You can also use provided initCordovaDemoApp.sh script that will generate a demo app that uses the plugin:

    ./initCordovaDemoApp.sh
