//
//  PPScanResultHistory.m
//  BlinkMotoDemo
//
//  Created by Dino Gustin on 12/03/2018.
//

#import "PPScanResultHistory.h"

@interface PPScanResultHistory ()

@end

@implementation PPScanResultHistory

- (instancetype)initWithThreshold:(NSUInteger)repetitionThreshold {
    self = [super init];
    if (self) {
        _repetitionThreshold = repetitionThreshold;
    }
    return self;
}

- (BOOL)pushResult:(NSString *)result {
    if ([_lastResult isEqualToString:result]) {
        _repetitionCount++;
    } else {
        _lastResult = result;
        _repetitionCount = 1;
    }
    if (_repetitionCount >= _repetitionThreshold) {
        return YES;
    } else {
        return NO;
    }
}

- (void)resetState {
    _repetitionCount = 0;
    _lastResult = @"";
}

@end

