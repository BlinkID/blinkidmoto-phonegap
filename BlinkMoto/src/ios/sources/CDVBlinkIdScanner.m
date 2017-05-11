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

    self.scanElements = [[MBParsers getParsers] mutableCopy];

    PPCameraCoordinator *coordinator = [self createCordinator];
    [self presentFormScannerWithCoordinator:coordinator];
}

#pragma mark - initiate scan

- (PPCameraCoordinator *)createCordinator {

    NSError *error;

    if ([PPCameraCoordinator isScanningUnsupportedForCameraType:PPCameraTypeBack error:&error]) {
        NSString *messageString = [error localizedDescription];
        [[[UIAlertView alloc] initWithTitle:@"Warning"
                                    message:messageString
                                   delegate:nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil, nil] show];
        return nil;
    }

    PPSettings *settings = [[PPSettings alloc] init];
    settings.licenseSettings.licenseKey = @"5MX4D3AJ-WJIJ7RFA-6W34264A-LPJKKGLM-F2JREAQ3-HS2RKZZU-4DI3LNIU-XS6WZN5Q";
    PPCameraCoordinator *coordinator = [[PPCameraCoordinator alloc] initWithSettings:settings];
    return coordinator;
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
        UIAlertView *theAlert = [[UIAlertView alloc] initWithTitle:@"No Scan Elements Present"
                                                           message:@"Tap Settings to add Scan Elements"
                                                          delegate:self
                                                 cancelButtonTitle:@"OK"
                                                 otherButtonTitles:nil];
        [theAlert show];
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
        [resultDict setObject:element.value forKey:element.identifier];
    }

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDict];

    [self.commandDelegate sendPluginResult:result callbackId:self.lastCommand.callbackId];
}

@end
