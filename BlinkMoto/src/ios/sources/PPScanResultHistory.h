//
//  PPScanResultHistory.h
//  BlinkMotoDemo
//
//  Created by Dino Gustin on 12/03/2018.
//

#import <Foundation/Foundation.h>

@interface PPScanResultHistory : NSObject

- (instancetype)initWithThreshold:(NSUInteger)repetitionThreshold;

@property (nonatomic, readonly) NSString *lastResult;

@property (nonatomic, readonly, assign) NSUInteger repetitionCount;

@property (nonatomic, assign) NSUInteger repetitionThreshold;

- (BOOL)pushResult:(NSString *)result;

- (void)resetState;

@end
