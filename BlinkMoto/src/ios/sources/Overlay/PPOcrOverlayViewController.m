//
//  PPOcrOverlayViewController.m
//  BlinkMoto-demo
//
//  Created by Jura Skrlec on 02/06/2017.
//  Copyright © 2017 MicroBlink. All rights reserved.
//

#import "PPOcrOverlayViewController.h"
#import "PPScanResultHistory.h"
#import "PPOcrFinderView.h"

@interface PPOcrOverlayViewController () <PPOcrFinderViewDelegate>

@property (strong, nonatomic) PPModernOcrResultOverlaySubview *resultOverlay;

@property (nonatomic, readonly) PPOcrFinderView *viewfinder;

@property (nonatomic) UIInterfaceOrientation interfaceOrientation;

@end

@implementation PPOcrOverlayViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _interfaceOrientation = UIInterfaceOrientationPortrait;
    
    _viewfinder = [[PPOcrFinderView alloc] initWithFrame:self.view.bounds];
    _viewfinder.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    _viewfinder.delegate = self;
    
    [self.view insertSubview:self.viewfinder atIndex:0];
    
    self.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    
    self.resultOverlay = [[PPModernOcrResultOverlaySubview alloc] initWithFrame:self.view.bounds];
    
    // Set overlay subview
    self.resultOverlay.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self.resultOverlay setBackgroundColor:[UIColor clearColor]];
    [self.view insertSubview:self.resultOverlay belowSubview:self.viewfinder];
    [self registerOverlaySubview:self.resultOverlay];

    [self.viewfinder setTranslation:self.translation];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self setupNotifications];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    
    // Set scanning region
    self.scanningRegion = self.viewfinder.scanningRegion;
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [self removeNotifications];
}

- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    [self updateScanningRegion];
}

- (void)updateScanningRegion {
    [self.view layoutIfNeeded];
    self.scanningRegion = self.viewfinder.scanningRegion;
    [UIView animateWithDuration:0.4
                     animations:^{
                         [self.view layoutIfNeeded];
                     }];
}

#pragma mark - notifications

- (void)setupNotifications {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(deviceOrientationChanged:)
                                                 name:UIDeviceOrientationDidChangeNotification
                                               object:nil];
}

- (void)removeNotifications {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceOrientationDidChangeNotification object:nil];
}

#pragma mark - Rotation

- (BOOL)shouldAutorotate {
    return YES;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskAllButUpsideDown;
}

- (void)deviceOrientationChanged:(NSNotification *)notification {
    UIDeviceOrientation deviceOrientation = [[UIDevice currentDevice] orientation];
    UIInterfaceOrientation orientation = self.interfaceOrientation;
    
    if (UIDeviceOrientationIsPortrait(deviceOrientation)) {
        orientation = UIInterfaceOrientationPortrait;
    } else if (UIDeviceOrientationIsLandscape(deviceOrientation)) {
        orientation = deviceOrientation == UIDeviceOrientationLandscapeLeft ? UIInterfaceOrientationLandscapeLeft : UIInterfaceOrientationLandscapeRight;
    }
    
    [self setInterfaceOrientation:orientation animated:YES];
}

- (void)setInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    [self setInterfaceOrientation:interfaceOrientation animated:NO];
}

- (void)setInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation animated:(BOOL)animated {

    if (self.interfaceOrientation == interfaceOrientation) {
        return;
    }
    
    if (UIInterfaceOrientationIsPortrait(interfaceOrientation)) {
        [self.viewfinder initViewfinderForPortrait];
    }
    
    _interfaceOrientation = interfaceOrientation;
}

#pragma mark - PPOcrFinderViewDelegate

- (BOOL)viewfinderViewIsScanningPaused:(PPOcrFinderView *)viewfinderView {
    return [self.containerViewController isScanningPaused];
}

- (void)viewfinderViewDidTapAcceptButton:(UIButton *)sender {
    if ([[self.viewfinder getScanningResult] length] > 0) {
        [self.delegate ocrOverlayViewControllerDidReturnResult:[self.viewfinder getScanningResult]];
    }
}

- (void)viewfinderViewDidTapCancelButton:(UIButton *)sender {
    [self.delegate ocrOverlayViewControllerWillClose:self];
}

- (void)viewfinderViewDidTapRepeatButton:(UIButton *)sender {
    [self.viewfinder resetScanningState];
    [self.containerViewController resumeScanningAndResetState:YES];
}

#pragma mark - PPBlinkIDScannerDelegate

- (void)blinkIdScanner:(PPBlinkIDScanner *)scanner didOutputResult:(NSString *)result fromImage:(UIImage *)image {
    [self.containerViewController pauseScanning];
    [self.viewfinder setOcrResultSucces:YES withResult:result andImage:image];
}

@end
