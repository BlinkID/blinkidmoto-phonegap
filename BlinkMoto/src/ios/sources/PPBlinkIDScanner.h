//
//  PPScanner.h
//  BlinkMotoDemo
//
//  Created by Jura on 30/04/2018.
//

#import <Foundation/Foundation.h>

#import <MicroBlink/MicroBlink.h>
#import "PPVinValidator.h"

typedef NS_ENUM(NSInteger, PPParserType) {
    PPParserTypeVin,
    PPParserTypeLicensePlate
};

@protocol PPBlinkIDScannerDelegate;

@interface PPBlinkIDScanner : NSObject

@property (nonatomic, weak) id<PPBlinkIDScannerDelegate> delegate;

- (instancetype)initWithLicenseKey:(NSString *)licenseKey
                        parserType:(PPParserType)parserType
                      vinValidator:(id<PPVinValidator>)validator
                          delegate:(id<PPBlinkIDScannerDelegate>)delegate;

- (UIViewController *)createScanningViewControllerWithOverlayViewController:(PPOverlayViewController *)overlayVC;

+ (BOOL)isScanningUnsupportedWithError:(NSError *_Nullable *_Nullable)error;

@end


@protocol PPBlinkIDScannerDelegate

- (void)blinkIdScanner:(PPBlinkIDScanner *)scanner didOutputResult:(NSString *)result fromImage:(UIImage *)image;

@end
