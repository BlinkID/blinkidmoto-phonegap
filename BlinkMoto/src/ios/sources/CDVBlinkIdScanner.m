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

#import "PPOcrOverlayViewController.h"
#import "PPBlinkIDScanner.h"

@interface CDVBlinkIdScanner () <PPOcrOverlayViewControllerDelegate>

@property (nonatomic) PPBlinkIDScanner *scanner;

@property (nonatomic, retain) CDVInvokedUrlCommand *lastCommand;

@end

@implementation CDVBlinkIdScanner

#pragma mark - Main

- (void)scanVin:(CDVInvokedUrlCommand *)command {
    [self setLastCommand:command];
    [self presentFormScannerWithParserType:PPParserTypeVin];
}

- (void)scanLicensePlate:(CDVInvokedUrlCommand *)command {
    [self setLastCommand:command];
    [self presentFormScannerWithParserType:PPParserTypeLicensePlate];
}

- (void)isScanningUnsupportedForCameraType:(CDVInvokedUrlCommand *)command {
    NSError *error;
    CDVPluginResult *result = nil;

    if ([PPCameraCoordinator isScanningUnsupportedForCameraType:PPCameraTypeBack error:&error]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error: %@ Code:%ld", error.localizedDescription, (long)error.code]];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

#pragma mark - initiate scan

- (void)presentFormScannerWithParserType:(PPParserType)parserType {

    NSError *error;
    if ([PPBlinkIDScanner isScanningUnsupportedWithError:&error]) {
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Warning"
                                                                                 message:[error localizedDescription]
                                                                          preferredStyle:UIAlertControllerStyleAlert];

        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"OK"
                                                               style: UIAlertActionStyleDefault
                                                             handler:^(UIAlertAction * _Nonnull action) {
                                                                 // nothing here
                                                             }];
        [alertController addAction:cancelAction];

        [[self viewController] presentViewController:alertController animated:YES completion:nil];
    }

    PPOcrOverlayViewController *overlayVC = [[PPOcrOverlayViewController alloc] init];
    overlayVC.translation = [self.lastCommand argumentAtIndex:2];
    overlayVC.delegate = self;

    self.scanner = [[PPBlinkIDScanner alloc] initWithLicenseKey:[self.lastCommand argumentAtIndex:0]
                                                     parserType:parserType
                                                   vinValidator:[[PPDaimlerVinValidator alloc] init]
                                                       delegate:overlayVC];
    
    UIViewController *scanningViewController = [self.scanner createScanningViewControllerWithOverlayViewController:overlayVC];

    [[self viewController] presentViewController:scanningViewController animated:YES completion:nil];
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

#pragma mark - PPOcrOverlayViewControllerDelegate

- (void)ocrOverlayViewControllerWillClose:(PPOcrOverlayViewController *)vc {
    [[self viewController] dismissViewControllerAnimated:YES completion:nil];
    [self returnAsCancelled:YES withScanResult:nil];
}

- (void)ocrOverlayViewControllerDidReturnResult:(NSString *)scanResult {
    [[self viewController] dismissViewControllerAnimated:YES completion:nil];
    [self returnAsCancelled:NO withScanResult:scanResult];
}

@end
