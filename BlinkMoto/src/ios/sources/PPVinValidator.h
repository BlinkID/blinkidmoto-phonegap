//
//  PPVinPostprocessor.h
//  BlinkMotoDemo
//
//  Created by Jura on 04/05/2018.
//

#import <Foundation/Foundation.h>

@protocol PPVinValidator

- (BOOL)isVinValid:(NSString *)vin;

@end

@interface PPDaimlerVinValidator : NSObject<PPVinValidator>

- (BOOL)isVinValid:(NSString *)vin;

@end
