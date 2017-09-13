//
//  pdf417Plugin.m
//  CDVpdf417
//
//  Created by Jurica Cerovec, Marko Mihovilic on 10/01/13.
//  Copyright (c) 2013 Racuni.hr. All rights reserved.
//

/**
 * Copyright (c)2013 Racuni.hr d.o.o. All rights reserved.
 *
 * ANY UNAUTHORIZED USE OR SALE, DUPLICATION, OR DISTRIBUTION
 * OF THIS PROGRAM OR ANY OF ITS PARTS, IN SOURCE OR BINARY FORMS,
 * WITH OR WITHOUT MODIFICATION, WITH THE PURPOSE OF ACQUIRING
 * UNLAWFUL MATERIAL OR ANY OTHER BENEFIT IS PROHIBITED!
 * THIS PROGRAM IS PROTECTED BY COPYRIGHT LAWS AND YOU MAY NOT
 * REVERSE ENGINEER, DECOMPILE, OR DISASSEMBLE IT.
 */

#import "CDVBlinkIdScanner.h"

#import <MicroBlink/MicroBlink.h>
#import "PPOcrOverlayViewController.h"

@interface CDVBlinkIdScanner () <PPOcrOverlayViewControllerDelegate>

@property (nonatomic) NSMutableArray *scanElements;

@property (nonatomic, retain) CDVInvokedUrlCommand *lastCommand;

@end

@implementation CDVBlinkIdScanner

#pragma mark - Main

- (void)scan:(CDVInvokedUrlCommand *)command {

    [self setLastCommand:command];
    
    PPCameraCoordinator *coordinator = [self createCordinator];
    [self presentFormScannerWithCoordinator:coordinator andOcrParserType:VIN];
}

- (void)scanLicensePlate:(CDVInvokedUrlCommand *)command {
    
    [self setLastCommand:command];
    
    PPCameraCoordinator *coordinator = [self createCordinator];
    [self presentFormScannerWithCoordinator:coordinator andOcrParserType:LicensePlate];
}

#pragma mark - initiate scan

- (PPCameraCoordinator *)createCordinator {

    NSError *error;

    if ([PPCameraCoordinator isScanningUnsupportedForCameraType:PPCameraTypeBack error:&error]) {
        NSString *messageString = [error localizedDescription];
        
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Warning"
                                                                                 message:messageString
                                                                          preferredStyle:UIAlertControllerStyleAlert];
        
        {
            UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"OK"
                                                                   style: UIAlertActionStyleDefault
                                                                 handler:^(UIAlertAction * _Nonnull action) {
            }];
            [alertController addAction:cancelAction];
        }
        
        [[self viewController] presentViewController:alertController animated:YES completion:nil];
        
        return nil;
    }
    
    PPSettings *settings = [[PPSettings alloc] init];
    settings.licenseSettings.licenseKey = [self.lastCommand argumentAtIndex:0];
    settings.metadataSettings.successfulFrame = YES;
    PPCameraCoordinator *coordinator = [[PPCameraCoordinator alloc] initWithSettings:settings];
    return coordinator;
}

- (void)presentFormScannerWithCoordinator:(PPCameraCoordinator *)coordinator andOcrParserType:(OcrRecognizerType)ocrRecognizerType {
    
    if (coordinator == nil) {
        return;
    }
    
    NSDictionary *translation = [self.lastCommand argumentAtIndex:2];
    PPOcrOverlayViewController *overlayVC = [[PPOcrOverlayViewController alloc] initWithOcrRecognizerType:ocrRecognizerType andTranslation:translation];
    overlayVC.coordinator = coordinator;
    overlayVC.delegate = self;
    
    UIViewController<PPScanningViewController> *scanningViewController =
    [PPViewControllerFactory cameraViewControllerWithDelegate:nil
                                        overlayViewController:overlayVC
                                                  coordinator:coordinator
                                                        error:nil];
    scanningViewController.autorotate = YES;
    scanningViewController.supportedOrientations = UIInterfaceOrientationMaskAllButUpsideDown;
    overlayVC.scanningViewController = scanningViewController;
    [[self viewController] presentViewController:scanningViewController animated:YES completion:nil];
    
    
}

#pragma mark - PPOcrOverlayViewControllerDelegate
- (void)ocrOverlayViewControllerWillClose:(PPOcrOverlayViewController *)vc {
    [[self viewController] dismissViewControllerAnimated:YES completion:nil];
    
    [self returnAsCancelled:YES withScanResult:nil];
}

- (void)ocrOverlayViewControllerDidReturnResult:(NSString *)scanResult {
    [[self viewController] dismissViewControllerAnimated:YES completion:nil];
    
    [self returnAsCancelled:NO withScanResult:scanResult];
}

#pragma mark - return results

- (void)returnAsCancelled:(BOOL)cancelled withScanResult:(NSString *)scanResult {

    NSMutableDictionary *resultDict = [[NSMutableDictionary alloc] init];
    [resultDict setObject:[NSNumber numberWithInt:(cancelled ? 1 : 0)] forKey:@"cancelled"];
    
    if (scanResult.length > 0) {
        [resultDict setObject:scanResult forKey:@"result"];
    }

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDict];

    [self.commandDelegate sendPluginResult:result callbackId:self.lastCommand.callbackId];
}

#pragma mark - check scanning supported
- (void)isScanningUnsupportedForCameraType:(CDVInvokedUrlCommand *)command {
    NSError *error;
    CDVPluginResult *result = nil;
    if ([PPCameraCoordinator isScanningUnsupportedForCameraType:PPCameraTypeBack error:&error]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error: %@ Code:%ld", error.localizedDescription, (long)error.code]];
    }
    else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];

}

@end
