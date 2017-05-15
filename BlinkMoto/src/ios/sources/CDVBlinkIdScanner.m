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

#import <MicroBlinkDynamic/MicroBlinkDynamic.h>


#import "PPFormOcrOverlayViewController.h"
#import "MBParsers.h"

@interface CDVBlinkIdScanner () <PPFormOcrOverlayViewControllerDelegate>

@property (nonatomic) NSMutableArray *scanElements;

@property (nonatomic, retain) CDVInvokedUrlCommand *lastCommand;

@end

@implementation CDVBlinkIdScanner

#pragma mark - Main

- (void)scan:(CDVInvokedUrlCommand *)command {

    [self setLastCommand:command];
    
    PPCameraCoordinator *coordinator = [self createCordinator];
    [self initializeScanElements];
    [self presentFormScannerWithCoordinator:coordinator];
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
    settings.licenseSettings.licenseKey = [self.lastCommand argumentAtIndex:2];
    
    PPCameraCoordinator *coordinator = [[PPCameraCoordinator alloc] initWithSettings:settings];
    return coordinator;
}

- (void)initializeScanElements {
    self.scanElements = [[NSMutableArray alloc] init];
    NSArray *recognizerTypes = [self.lastCommand argumentAtIndex:0];

    if ([self shouldUseVINRecognizerForTypes:recognizerTypes]) {
        [self.scanElements addObject:[MBParsers getVINParser]];
    }
    
    if ([self shouldUseLicensePlateRecognizerForTypes:recognizerTypes]) {
        [self.scanElements addObject:[MBParsers getLicensePlateParser]];
    }
}


- (void)presentFormScannerWithCoordinator:(PPCameraCoordinator *)coordinator {

    if (coordinator == nil) {
        return;
    }

    if (self.scanElements.count > 0) {
        PPFormOcrOverlayViewController *overlayViewController =
        [PPFormOcrOverlayViewController allocFromNibName:@"PPFormOcrOverlayViewController"];
        
        overlayViewController.scanElements = self.scanElements;
        overlayViewController.coordinator = coordinator;
        overlayViewController.delegate = self;

        UIViewController<PPScanningViewController> *scanningViewController =
        [PPViewControllerFactory cameraViewControllerWithDelegate:nil
                                            overlayViewController:overlayViewController
                                                      coordinator:coordinator
                                                            error:nil];

        [[self viewController] presentViewController:scanningViewController animated:YES completion:nil];

    } else {
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"No Scan Elements Present"
                                                                                 message:@"Tap Settings to add Scan Elements"
                                                                          preferredStyle:UIAlertControllerStyleAlert];
        
        {
            UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"OK"
                                                                   style: UIAlertActionStyleDefault
                                                                 handler:^(UIAlertAction * _Nonnull action) {
                                                                 }];
            [alertController addAction:cancelAction];
        }
        
        [[self viewController] presentViewController:alertController animated:YES completion:nil];
    }
}

#pragma mark - PPFormOcrOverlayViewControllerDelegate

- (void)formOcrOverlayViewControllerWillClose:(PPFormOcrOverlayViewController *)vc {
    [[self viewController] dismissViewControllerAnimated:YES completion:nil];

    [self returnAsCancelled:YES];
}

- (void)formOcrOverlayViewController:(PPFormOcrOverlayViewController *)vc didFinishScanningWithElements:(NSArray *)scanElements {

    [[self viewController] dismissViewControllerAnimated:YES
                                              completion:^(void) {
                                                  ;
                                              }];

    [self returnAsCancelled:NO];
}

#pragma mark - return results

- (void)returnAsCancelled:(BOOL)cancelled {

    NSMutableDictionary *resultDict = [[NSMutableDictionary alloc] init];
    [resultDict setObject:[NSNumber numberWithInt:(cancelled ? 1 : 0)] forKey:@"cancelled"];

    for (PPScanElement *element in self.scanElements) {
        if (element.value != nil) {
            [resultDict setObject:element.value forKey:element.identifier];
        }
    }

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDict];

    [self.commandDelegate sendPluginResult:result callbackId:self.lastCommand.callbackId];
}

- (BOOL)shouldUseVINRecognizerForTypes:(NSArray *)recognizerTypes {
    return [recognizerTypes containsObject:@"VIN"];
}

- (BOOL)shouldUseLicensePlateRecognizerForTypes:(NSArray *)recognizerTypes {
    return [recognizerTypes containsObject:@"LicensePlate"];
}

@end
