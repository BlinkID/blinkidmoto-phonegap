//
//  PPOcrOverlayViewController.h
//  BlinkMoto-demo
//
//  Created by Jura Skrlec on 02/06/2017.
//  Copyright © 2017 MicroBlink. All rights reserved.
//

#import <MicroBlink/MicroBlink.h>

#import "PPBlinkIDScanner.h"

@protocol PPOcrOverlayViewControllerDelegate;

@interface PPOcrOverlayViewController : PPBaseOverlayViewController<PPBlinkIDScannerDelegate>

/**
 * Delegate which is notified with important UI events
 */
@property (nonatomic, weak) id<PPOcrOverlayViewControllerDelegate> delegate;

/** Dictionary with translation strings */
@property (nonatomic, strong) NSDictionary *translation;

@end

/**
 * Protocol for observing important events with scanning
 */
@protocol PPOcrOverlayViewControllerDelegate

@required

/**
 * Called when Overlay will close. This can happen if the user pressed close button
 *
 * Perform here your VC dismiss logic.
 *
 *  @param vc View Controller responsible for scanning
 */
- (void)ocrOverlayViewControllerWillClose:(PPOcrOverlayViewController *)vc;

/**
 * Called when Accept button is tapped to return scanning VIN/License plate result
 */
- (void)ocrOverlayViewControllerDidReturnResult:(NSString *)scanResult;

@end
