//
//  PPScanner.m
//  BlinkMotoDemo
//
//  Created by Jura on 30/04/2018.
//

#import "PPBlinkIDScanner.h"
#import "PPScanResultHistory.h"

static NSString * const kVinOcrParser = @"VIN OCR Parser";
static NSString * const kLicensePlateOcrParser = @"License Plate OCR Parser";

@interface PPBlinkIDScanner () <PPCoordinatorDelegate>

@property (nonatomic) PPCameraCoordinator *coordinator;

@property (nonatomic) id<PPVinValidator> validator;

@property (nonatomic) PPImageMetadata *successImageMetadata;

@property (nonatomic) PPParserType parserType;

@property (nonatomic) PPScanResultHistory *history;

@end

@implementation PPBlinkIDScanner

- (instancetype)initWithLicenseKey:(NSString *)licenseKey
                        parserType:(PPParserType)parserType
                      vinValidator:(id<PPVinValidator>)validator
                          delegate:(id<PPBlinkIDScannerDelegate>)delegate {

    self = [super init];
    if (self) {
        _parserType = parserType;
        _validator = validator;
        _delegate = delegate;
        _history = [[PPScanResultHistory alloc] initWithThreshold:3];

        PPSettings *settings = [[PPSettings alloc] init];
        settings.licenseSettings.licenseKey = licenseKey;
        settings.metadataSettings.successfulFrame = YES;
        settings.metadataSettings.currentFrame = YES;

        if (parserType == PPParserTypeVin) {
            PPVinRecognizerSettings *vinRecognizerSettings = [[PPVinRecognizerSettings alloc] init];
            [settings.scanSettings addRecognizerSettings:vinRecognizerSettings];

            PPBlinkInputRecognizerSettings *blinkInputRecognizerSettings = [[PPBlinkInputRecognizerSettings alloc] init];
            [blinkInputRecognizerSettings addOcrParser:[[PPVinOcrParserFactory alloc] init] name:kVinOcrParser];
            [settings.scanSettings addRecognizerSettings:blinkInputRecognizerSettings];
        } else if (parserType == PPParserTypeLicensePlate) {
            PPBlinkInputRecognizerSettings *blinkInputRecognizerSettings = [[PPBlinkInputRecognizerSettings alloc] init];
            [blinkInputRecognizerSettings addOcrParser:[[PPLicensePlatesParserFactory alloc] init] name:kLicensePlateOcrParser];
            [settings.scanSettings addRecognizerSettings:blinkInputRecognizerSettings];
        }

        _coordinator = [[PPCameraCoordinator alloc] initWithSettings:settings delegate:self];
    }
    return self;
}

- (UIViewController *)createScanningViewControllerWithOverlayViewController:(PPOverlayViewController *)overlayVC {

    UIViewController<PPScanningViewController> *scanningViewController =
        [PPViewControllerFactory cameraViewControllerWithDelegate:nil
                                            overlayViewController:overlayVC
                                                      coordinator:self.coordinator
                                                            error:nil];
    scanningViewController.autorotate = YES;
    scanningViewController.supportedOrientations = UIInterfaceOrientationMaskPortrait | UIInterfaceOrientationMaskPortraitUpsideDown;

    return scanningViewController;
}

+ (BOOL)isScanningUnsupportedWithError:(NSError *_Nullable *_Nullable)error {
    return ([PPCameraCoordinator isScanningUnsupportedForCameraType:PPCameraTypeBack error:error]);
}

#pragma mark - PPCoordinatorDelegate

- (void)coordinator:(PPCoordinator *)coordinator didOutputMetadata:(PPMetadata *)metadata {
    if ([metadata isKindOfClass:[PPImageMetadata class]]) {
        PPImageMetadata *imageMetadata = (PPImageMetadata *)metadata;
        if (imageMetadata.imageType == PPImageMetadataTypeSuccessfulFrame) {
            self.successImageMetadata = imageMetadata;
        }
    }
}

- (void)coordinator:(PPCoordinator *)coordinator didOutputResults:(NSArray<PPRecognizerResult *> *)results {

    // First we check that we received a valid result!
    if (results == nil || results.count == 0) {
        return;
    }

    for (PPRecognizerResult *result in results) {
        NSString *stringResult;
        BOOL success = NO;
        if ([result isKindOfClass:[PPBlinkInputRecognizerResult class]]) {
            PPBlinkInputRecognizerResult *inputResult = (PPBlinkInputRecognizerResult *)result;

            switch (self.parserType) {
                case PPParserTypeVin:
                    stringResult = [inputResult parsedResultForName:kVinOcrParser];
                    if ([self.validator isVinValid:stringResult]) {
                        success = [self.history pushResult:stringResult];
                    }
                    break;
                case PPParserTypeLicensePlate:
                    stringResult =  [inputResult parsedResultForName:kLicensePlateOcrParser];
                    success = [self.history pushResult:stringResult];
                    break;
                default:
                    stringResult =  @"";
                    success = NO;
                    break;
            }
        }
        if ([result isKindOfClass:[PPVinRecognizerResult class]]) {
            PPVinRecognizerResult *vinRecognizerResult = (PPVinRecognizerResult *)result;
            stringResult = vinRecognizerResult.vinNumber;
            success = YES;
        }
        if (success) {
            [self.history resetState];
            [self.delegate blinkIdScanner:self didOutputResult:stringResult fromImage:self.successImageMetadata.image];
        }
    }
}

@end
