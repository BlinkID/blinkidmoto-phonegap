/**
 * cordova is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 */


    var exec = require("cordova/exec");

    /**
     * Constructor.
     *
     * @returns {BlinkIdScanner}
     */
    function BlinkIdScanner() {

    };

/**
 * licenseiOS - iOS license key to enable all features (not required)
 * licenseAndroid - Android license key to enable all features (not required)
 * translation - Dictionary with keys: title_text, cancel_text, repeat_text, accept_text
 */

    BlinkIdScanner.prototype.scan = function (successCallback, errorCallback, licenseiOs, licenseAndroid, translation) {
        if (errorCallback == null) {
            errorCallback = function () {
            };
        }

        if (typeof errorCallback !== "function") {
            console.log("BlinkIdScanner.scan failure: failure parameter not a function");
            return;
        }

        if (typeof successCallback !== "function") {
            console.log("BlinkIdScanner.scan failure: success callback parameter must be a function");
            return;
        }

        exec(successCallback, errorCallback, 'BlinkIdScanner', 'scan', [licenseiOs, licenseAndroid, translation]);
    };
               
   BlinkIdScanner.prototype.scanLicensePlate = function (successCallback, errorCallback, licenseiOs, licenseAndroid, translation) {
       if (errorCallback == null) {
           errorCallback = function () {
           };
       }
       
       if (typeof errorCallback !== "function") {
           console.log("BlinkIdScanner.scanLicensePlate failure: failure parameter not a function");
           return;
       }
       
       if (typeof successCallback !== "function") {
           console.log("BlinkIdScanner.scanLicensePlate failure: success callback parameter must be a function");
           return;
       }
       
       exec(successCallback, errorCallback, 'BlinkIdScanner', 'scanLicensePlate', [licenseiOs, licenseAndroid, translation]);
   };

      BlinkIdScanner.prototype.isScanningUnsupportedForCameraType = function (successCallback, errorCallback) {
       if (errorCallback == null) {
           errorCallback = function () {
           };
       }
       
       if (typeof errorCallback !== "function") {
       console.log("BlinkIdScanner.isScanningUnsupportedForCameraType failure: failure parameter not a function");
       return;
       }
       
       if (typeof successCallback !== "function") {
           console.log("BlinkIdScanner.isScanningUnsupportedForCameraType failure: success callback parameter must be a function");
           return;
       }
       
       exec(successCallback, errorCallback, 'BlinkIdScanner', 'isScanningUnsupportedForCameraType');
   };

    var blinkIdScanner = new BlinkIdScanner();
    module.exports = blinkIdScanner;