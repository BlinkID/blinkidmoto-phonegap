//
//  pdf417Plugin.h
//  CDVpdf417
//
//  Created by Jurica Cerovec, Marko Mihovilic on 10/01/13.
//  Copyright (c) 2013 Racuni.hr. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <Cordova/CDV.h>

/**
 * pdf417 plugin class.
 * Responds to JS calls
 */
@interface CDVBlinkIdScanner : CDVPlugin

/**
 * Starts VIN scanning process
 */
- (void)scan:(CDVInvokedUrlCommand *)command;

/**
 * Starts License plate scanning process
 */

- (void)scanLicensePlate:(CDVInvokedUrlCommand *)command;

/**
 * Returns successful recognition
 */
- (void)returnAsCancelled:(BOOL)cancelled withScanResult:(NSString *)scanResult;

/**
 * Check if scanning is unsupported
 */
- (void)isScanningUnsupportedForCameraType:(CDVInvokedUrlCommand *)command;

@end
