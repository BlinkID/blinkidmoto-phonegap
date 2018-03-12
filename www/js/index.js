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


        // Note that each platform requires its own license key

        // This license key allows setting overlay views for this application ID: com.microblink.blinkid
        var licenseiOs = "R23ALVMH-2UTECJBQ-3L25IWY5-R5FKXY5K-I6PCROGY-MJXP7R54-YGAZ5CBH-AR6YVLRA"; // Valid until 2018-06-07

        // This license is only valid for package name "com.microblink.blinkid"
        var licenseAndroid = "PDSEMRTP-ACSCUVIA-UBIBQNDG-D7CPIENL-WHW2TLUQ-T6G73F3U-GEM3F7HQ-MVGDHQLY";
        
        // Translation dictionary
        var translation = {
            title_text: "Bitte die FIN / VIN oder Barcode in diesem Bereich erfassen",
            cancel_text: "Abbrechen",
            repeat_text: "Wiederholen",
            accept_text: "Übernehmen"
        };
        
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
                
                licenseiOs, licenseAndroid, translation
            );
        });
    
        scanButtonLicensePlate.addEventListener('click', function() {
            cordova.plugins.blinkIdScanner.scanLicensePlate(

                // Register the callback handler
                function callback(scanningResult) {
                    alert(JSON.stringify(scanningResult))
                },
                
                // Register the error callback
                function errorHandler(err) {
                alert('Error: ' + err);
                },
                
                licenseiOs, licenseAndroid, translation
            );
        });

    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        console.log('Received Event: ' + id);
    }
};
