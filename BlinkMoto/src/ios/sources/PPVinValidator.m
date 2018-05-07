//
//  PPVinPostprocessor.m
//  BlinkMotoDemo
//
//  Created by Jura on 04/05/2018.
//

#import "PPVinValidator.h"

@implementation PPDaimlerVinValidator

- (BOOL)string:(NSString *)string hasDigitsOnIndexes:(NSArray<NSNumber *> *)indexes {
    for (NSNumber *index in indexes) {
        if (isdigit([string characterAtIndex:[index intValue]])) {
            return true;
        }
    }
    return false;
}

- (BOOL)isVinValid:(NSString *)vin {
    unichar c = [vin characterAtIndex:0];

    if (c == 'I') {
        NSLog(@"Vin starts with I");
        return NO;
    }

    if ([self string:vin hasDigitsOnIndexes:@[@(0), @(1), @(2)]]) {
        NSLog(@"VIN has digits in first three places!");
        return NO;
    }

    return YES;
}

@end
