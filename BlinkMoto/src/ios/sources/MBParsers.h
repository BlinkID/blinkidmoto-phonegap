//
//  PPParsers.h
//  SegmentScanDemo
//
//  Created by Dino on 31/03/16.
//  Copyright © 2016 Dino. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PPScanElement;

@interface MBParsers : NSObject

+ (PPScanElement *)getVINParser;
+ (PPScanElement *)getLicensePlateParser;

@end
