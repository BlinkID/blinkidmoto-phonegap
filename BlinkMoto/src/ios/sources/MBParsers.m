//
//  PPParsers.m
//  SegmentScanDemo
//
//  Created by Dino on 31/03/16.
//  Copyright © 2016 Dino. All rights reserved.
//

#import "MBParsers.h"
#import "PPScanElement.h"
#import <MicroBlinkDynamic/MicroBlinkDynamic.h>

@interface MBParsers ()

@end

@implementation MBParsers

+ (PPScanElement *)getVINParser {
    PPVinOcrParserFactory *vinFactory = [[PPVinOcrParserFactory alloc] init];
    vinFactory.isRequired = NO;
    PPScanElement *vinElement = [[PPScanElement alloc] initWithIdentifier:@"VIN number" parserFactory:vinFactory];
    vinElement.localizedTitle = @"VIN";
    vinElement.localizedTooltip = @"Position VIN number in this window";
    vinElement.scanningRegionHeight = 0.20;
    vinElement.scanningRegionWidth = 0.86;
    
    return vinElement;
}

+ (PPScanElement *)getLicensePlateParser {
    PPLicensePlatesParserFactory *licensePlatesFactory = [[PPLicensePlatesParserFactory alloc] init];
    licensePlatesFactory.isRequired = NO;
    PPScanElement *licensePlatesElement = [[PPScanElement alloc] initWithIdentifier:@"License plate" parserFactory:licensePlatesFactory];
    licensePlatesElement.localizedTitle = @"License plate";
    licensePlatesElement.localizedTooltip = @"Position License plate in this window";
    licensePlatesElement.scanningRegionHeight = 0.20;
    licensePlatesElement.scanningRegionWidth = 0.86;
    
    return licensePlatesElement;
}

@end
