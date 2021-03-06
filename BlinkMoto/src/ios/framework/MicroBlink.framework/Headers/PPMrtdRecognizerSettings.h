//
//  PPMrtdRecognizerSettings.h
//  PhotoPayFramework
//
//  Created by Jura on 26/02/15.
//  Copyright (c) 2015 MicroBlink Ltd. All rights reserved.
//

#import "PPTemplatingRecognizerSettings.h"
#import "PPMrzFilter.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Settings class for configuring MRTD Recognizer.
 *
 * MRTD Recognizer recognizer is used for scanning and parsing Machine readable travel documents.
 * Typical MRTDs are passports, visas, ID cards. They can be recognized by two or three lines of monospace text, which contains all personal
 * information.
 *
 * @see https://en.wikipedia.org/wiki/Machine-readable_passport
 */
PP_CLASS_AVAILABLE_IOS(6.0)
@interface PPMrtdRecognizerSettings : PPTemplatingRecognizerSettings

/**
 * Name of the image sent to didOutputMetadata method of scanDelegate object that contains full document.
 * This image will be sent to scan delegate during recognition process if displaying of full document image
 * is enabled via dewarpFullDocument property and receiving of dewarpedImage in MetadataSettings is enabled.
 */
+ (NSString *)FULL_DOCUMENT_IMAGE;

/**
 * Name of the image sent to didOutputMetadata method of scanDelegate object that contains machine readable zone.
 * This image will be sent to scan delegate during recognition process if displaying of full document image
 * is disabled via dewarpFullDocument property and receiving of dewarpedImage in MetadataSettings is enabled.
 */
+ (NSString *)MRZ_IMAGE;

/**
 * If YES, MrtdRecognizer will return MRTD results even if they are not parsed.
 *
 * Default NO.
 *
 * Setting this to YES will give you the chance to parse MRZ result, if Mrtd recognizer wasn't
 * successful in parsing (this can happen since MRZ isn't always formatted accoring to ICAO Document 9303 standard.
 * @see http://www.icao.int/Security/mrtd/pages/Document9303.aspx
 *
 * When YES, MrtdRecognizerResult will be returned with isParsed property set to NO, and with rawOcrLayout property set
 * to the PPOcrLayout object which was the result of the OCR process.
 *
 * However, you should be careful when this property is set to YES, since obtained OcrLayout can contain OCR errors (for example
 * (0 <-> O, 2 <-> Z, etc.). If you set this to YES, then you need to perform your own parsing and error correction.
 *
 * If you set this to YES, we suggest the following approach in your result callback
 *
 *    - obtain mrtdResult
 *    - if [mrtdResult isParsed]
 *        - present result and return
 *    - else if mrtdResult can be parsed with your custom parsing algorithm
 *        - present your custom results and return
 *    - else continue scanning since MRTD result cannot be parsed at all
 */
@property (nonatomic, assign) BOOL allowUnparsedResults;

/**
 * Set this to YES to allow obtaining of results with incorrect check digits.
 * This flag will be taken into account only if Machine Readable Zone has been successfully parsed because only in that case check digits can be examined.
 * 
 * Default: NO.
 */
@property (nonatomic, assign) BOOL allowUnverifiedResults;

/**
 * Set this to YES to allow OCR of non-standard characters in MRZ (e.g. '-')
 *
 * Default: NO.
 */
@property (nonatomic, assign) BOOL allowSpecialCharacters;

/**
 * If YES, MRTD recognizer will determine the position of the whole
 * MRTD document, based on the position of the machine readable zone.
 *
 * Also, MRTD recognizer will dewarp and crop the image around the MRTD.
 *
 * This is useful if you're at the same time obtaining Dewarped image metadata, since it allows you to obtain dewarped and cropped
 * images of MRTD documents. Dewarped images are returned to scanningViewController:didOutputMetadata: callback,
 * as PPImageMetadata objects with name @"MRTD"
 *
 * If NO, this logic is not performed.
 *
 * Default: NO.
 */
@property (nonatomic, assign) BOOL dewarpFullDocument;

/**
 * Delegate for mrz filter.
 *
 * Default: nil
 */
@property (nonatomic, weak) id<PPMrzFilter> mrzFilter;

/**
 * Property got setting DPI for full document images
 * Valid ranges are [100,400]. Setting DPI out of valid ranges throws an exception
 *
 * Default: 250.0
 */
@property (nonatomic, assign) NSUInteger fullDocumentImageDPI;

@end

NS_ASSUME_NONNULL_END
