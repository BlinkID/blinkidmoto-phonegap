/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// implement your decoding as you need it, this just does ASCII decoding
function hex2a(hex) {
    var str = '';
    for (var i = 0; i < hex.length; i += 2) {
        str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    }
    return str;
}

var app = {
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicity call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');
        
        var resultDiv = document.getElementById('resultDiv');
        var resultImgDiv = document.getElementById('imageDiv');
        var resultImg = document.getElementById('documentImage');
    
        resultImgDiv.style.visibility = "hidden"

        /**
         * Use these scanner types
         * You can choose VIN or LicensePlate or both at the same time
         * Available: "VIN", "LicensePlate"
         */
        var types = ["VIN", "LicensePlate"];

        /**
         * Image type defines type of the image that will be returned in scan result (image is returned as Base64 encoded JPEG)
         * available:
         *  "IMAGE_NONE" : do not return image in scan result
         *  "IMAGE_SUCCESSFUL_SCAN" : return full camera frame of successful scan
         *  "IMAGE_CROPPED" : return cropped document image (successful scan)
         *
         */
        var imageType = "IMAGE_CROPPED"

        // Note that each platform requires its own license key

        // This license key allows setting overlay views for this application ID: com.microblink.blinkid
        var licenseiOs = "FJDHBCFC-VPQMNNKV-6EKMUSTE-ZWBQKA2E-66LVGM5U-SAOG7RHA-2G2RLR55-COSW4YTN"; // valid until 2017-07-14

        // This license is only valid for package name "com.microblink.blinkid"
        var licenseAndroid = "NFRZVYWD-MCK7SSO7-TJ7ZWOC4-AT2AYDM7-JDHZQMHY-V3PZU4SX-54PGUFQM-AUX5RGYJ";

        scanButton.addEventListener('click', function() {    
            cordova.plugins.blinkIdScanner.scan(
            
                // Register the callback handler
                function callback(scanningResult) {
                    alert(JSON.stringify(scanningResult))
                },
                
                // Register the error callback
                function errorHandler(err) {
                    alert('Error: ' + err);
                },

                types, imageType, licenseiOs, licenseAndroid
            );
        });

    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        console.log('Received Event: ' + id);
    }
};
