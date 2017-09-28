package com.phonegap.plugins.blinkid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.microblink.detectors.DetectorResult;
import com.microblink.detectors.points.PointsDetectorResult;
import com.microblink.geometry.Rectangle;
import com.microblink.hardware.camera.CameraType;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.hardware.orientation.OrientationChangeListener;
import com.microblink.image.Image;
import com.microblink.image.ImageType;
import com.microblink.metadata.DetectionMetadata;
import com.microblink.metadata.ImageMetadata;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.metadata.OcrMetadata;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkbarcode.vin.VinRecognizerSettings;
import com.microblink.recognizers.blinkbarcode.vin.VinScanResult;
import com.microblink.recognizers.blinkinput.BlinkInputRecognitionResult;
import com.microblink.recognizers.blinkinput.BlinkInputRecognizerSettings;
import com.microblink.recognizers.blinkocr.parser.licenseplates.LicensePlatesParserSettings;
import com.microblink.recognizers.blinkocr.parser.vin.VinParserSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.recognizers.settings.RecognizerSettingsUtils;
import com.microblink.util.CameraPermissionManager;
import com.microblink.util.Log;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.view.BaseCameraView;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.NonLandscapeOrientationNotSupportedException;
import com.microblink.view.OnActivityFlipListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.ocrResult.IOcrResultView;
import com.microblink.view.ocrResult.OcrResultDotsView;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.PointSetView;

public class ScanActivity extends Activity implements ScanResultListener, CameraEventsListener, MetadataListener, OnActivityFlipListener {
    public static final String EXTRAS_LICENSE_KEY = "key_license_string";
    public static final String EXTRAS_RECOGNIZER_TYPE = "key_recognizer_type_string";
    public static final String EXTRAS_TITLE_STRING = "key_title_string";
    public static final String EXTRAS_ACCEPT_STRING = "key_accept_string";
    public static final String EXTRAS_CANCEL_STRING = "key_cancel_string";
    public static final String EXTRAS_REPEAT_STRING = "key_repeat_string";

    public static final String EXTRAS_RESULT_STRING = "key_result_string";

    private static final float SCANNING_REGION_ASPECT_RATIO = 1 / 4f;

    public enum RecognizerType {
        VIN, LICENCE_PLATES
    }

    private final String OCR_PARSER_NAME = "parser";

    private FakeR mFakeR;

    private CameraPermissionManager mCameraPermManager;
    private RecognizerView mRecognizerView;
    private PointSetView mPointSetView;
    private IOcrResultView mOcrResultView;

    private FrameLayout mRecognizerViewRoot;
    private FrameLayout mScanViewfinder;
    private TextView mScanTitleView;
    private TextView mScanResultStringView;
    private ImageView mScanResultImageView;
    private Button mAcceptButton;
    private Button mCancelButton;
    private Button mRepeatButton;

    private Image mResultImage;

    private enum ActivityState {
        DESTROYED,
        CREATED,
        STARTED,
        RESUMED
    }

    private ActivityState mActivityState = ActivityState.DESTROYED;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mFakeR = new FakeR(this);
        super.onCreate(savedInstanceState);
        try {
            setContentView(mFakeR.getIdFrom("layout", "custom_scan_layout"));
        } catch (InflateException ie) {
            Throwable cause = ie.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof NonLandscapeOrientationNotSupportedException) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                recreate();
                return;
            } else {
                throw ie;
            }
        }

        mActivityState = ActivityState.CREATED;

        // Set internationalized strings.
        Bundle extras = getIntent().getExtras();

        mRecognizerView = (RecognizerView) findViewById(mFakeR.getId("recognizerView"));

        // Set license key.
        String licenseKey = extras.getString(EXTRAS_LICENSE_KEY);
        try {
            mRecognizerView.setLicenseKey(licenseKey);
        } catch (InvalidLicenceKeyException exc) {
            Log.e(this, exc, "INVALID LICENCE KEY");
        }

        // Add the camera permissions overlay.
        mCameraPermManager = new CameraPermissionManager(this);
        mRecognizerViewRoot = (FrameLayout) findViewById(mFakeR.getId("recognizerViewRoot"));
        View cameraPermissionView = mCameraPermManager.getAskPermissionOverlay();
        if (cameraPermissionView != null) {
            mRecognizerViewRoot.addView(cameraPermissionView);
        }

        // Setup array of recognition settings.
        RecognitionSettings settings = new RecognitionSettings();
        if (extras.getSerializable(EXTRAS_RECOGNIZER_TYPE) == null) {
            throw new NullPointerException("Recognizer type extra missing.");
        }

        RecognizerSettings[] settArray = setupSettingsArray((RecognizerType) extras.getSerializable(EXTRAS_RECOGNIZER_TYPE));
        if (!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, this)) {
            settArray = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(settArray);
        }
        settings.setRecognizerSettingsArray(settArray);
        // Allow only one result on image.
        settings.setAllowMultipleScanResultsOnSingleImage(false);
        mRecognizerView.setRecognitionSettings(settings);

        // Scan result listener will be notified when scan result gets available.
        mRecognizerView.setScanResultListener(this);
        // Camera events listener will be notified about camera lifecycle and errors.
        mRecognizerView.setCameraEventsListener(this);
        mRecognizerView.setOnActivityFlipListener(this);
        // Set camera aspect mode
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);

        // Allow all orientations.
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });

        mRecognizerView.setOrientationChangeListener(new OrientationChangeListener() {
            @Override
            public void onOrientationChange(Orientation orientation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    return;
                }
                resizeScanningRegion();
            }
        });

        // Listen to scanning metadata.
        MetadataSettings metadataSettings = new MetadataSettings();
        metadataSettings.setDetectionMetadataAllowed(true);
        metadataSettings.setOcrMetadataAllowed(true);

        MetadataSettings.ImageMetadataSettings imageMetadata = new MetadataSettings.ImageMetadataSettings();
        imageMetadata.setSuccessfulScanFrameEnabled(true);
        metadataSettings.setImageMetadataSettings(imageMetadata);
        mRecognizerView.setMetadataListener(this, metadataSettings);

        // Create the recognizer view.
        mRecognizerView.create();

        // Add point set view to scanner view as fixed (non-rotatable) view
        mPointSetView = new PointSetView(this, null, mRecognizerView.getHostScreenOrientation());
        mRecognizerView.addChildView(mPointSetView, false);
        // Add point set view to scanner view as fixed (non-rotatable) view
        mOcrResultView = new OcrResultDotsView(this, null, mRecognizerView.getHostScreenOrientation());
        mRecognizerView.addChildView(mOcrResultView.getView(), false);

        // Inflate the overlay view.
        final ViewGroup overlay = (ViewGroup) getLayoutInflater().inflate(mFakeR.getIdFrom("layout", "custom_scan_overlay"), null);
        // Bind view elements.
        mScanViewfinder = (FrameLayout) overlay.findViewById(mFakeR.getId("fl_scan_frame"));
        mScanTitleView = (TextView) overlay.findViewById(mFakeR.getId("tv_scan_title"));
        mScanResultStringView = (TextView) overlay.findViewById(mFakeR.getId("tv_scan_result"));
        mScanResultImageView = (ImageView) overlay.findViewById(mFakeR.getId("iv_scan_result"));
        mAcceptButton = (Button) overlay.findViewById(mFakeR.getId("btn_accept"));
        mCancelButton = (Button) overlay.findViewById(mFakeR.getId("btn_cancel"));
        mRepeatButton = (Button) overlay.findViewById(mFakeR.getId("btn_repeat"));
        // Set user defined titles.
        mScanTitleView.setText(extras.getString(EXTRAS_TITLE_STRING, mFakeR.getString("blinkid_scanning_title")));
        mAcceptButton.setText(extras.getString(EXTRAS_ACCEPT_STRING, mFakeR.getString("blinkid_accept")));
        mCancelButton.setText(extras.getString(EXTRAS_CANCEL_STRING, mFakeR.getString("blinkid_cancel")));
        mRepeatButton.setText(extras.getString(EXTRAS_REPEAT_STRING, mFakeR.getString("blinkid_repeat")));
        // Result image invisible at start.
        mScanResultImageView.setVisibility(View.GONE);
        // Cannot accept or retry when scanning is in progress.
        mRepeatButton.setEnabled(false);
        mAcceptButton.setEnabled(false);
        // Add the overlay to the recognizer view.
        mRecognizerView.addChildView(overlay, true);

        resizeScanningRegion();
    }

    private void resizeScanningRegion() {
        if (mRecognizerView == null || mRecognizerViewRoot == null) {
            return;
        }
        mRecognizerViewRoot.post(new Runnable() {
            @Override
            public void run() {
                Orientation orientation = mRecognizerView.getCurrentOrientation();
                if (mPointSetView != null) {
                    mPointSetView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
                    mPointSetView.setPointsDetectionResult(null);
                }
                if (mOcrResultView != null) {
                    mOcrResultView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
                    mOcrResultView.clearOcrResults();
                }
                // Determine the sizes based on orientation
                int width;
                int cameraWidth;
                int cameraHeight;
                if (orientation.isVertical()) {
                    width = mRecognizerView.getWidth();
                    cameraWidth = width;
                    cameraHeight = mRecognizerView.getHeight();
                } else {
                    width = mRecognizerView.getHeight();
                    cameraWidth = width;
                    cameraHeight = mRecognizerView.getWidth();
                }

                // Subtract the relative horizontal padding
                int horizontalPadding = (int) getResources().getDimension(mFakeR.getIdFrom("dimen", "blinkid_scan_region_padding"));
                width -= horizontalPadding * 2;

                // Calculate the height based on given aspect ratio
                int height = Math.round(width * SCANNING_REGION_ASPECT_RATIO);

                ViewGroup.LayoutParams params = mScanViewfinder.getLayoutParams();
                params.width = width;
                params.height = height;
                mScanViewfinder.setLayoutParams(params);
                mScanViewfinder.invalidate();

                if (cameraWidth != 0 && cameraHeight != 0) {
                    // Set the recognizer view scanning region (must use relative coordinates)
                    float x = horizontalPadding / (float) cameraWidth;
                    float y = (cameraHeight - height) / 2.0f / cameraHeight;
                    float w = width / (float) cameraWidth;
                    float h = height / (float) cameraHeight;
                    Log.i(this, "Scanning region updated x:{}, y:{}, w:{}, h:{}", x, y, w, h);
                    mRecognizerView.setScanningRegion(new Rectangle(x, y, w, h), false);
                } else {
                    Log.e(this, "Camera width or camera height is 0 w:{}, h:{}", cameraWidth, cameraHeight);
                }
            }
        });
    }

    private RecognizerSettings[] setupSettingsArray(RecognizerType type) {
        BlinkInputRecognizerSettings ocrSettings = new BlinkInputRecognizerSettings();

        if (type.equals(RecognizerType.LICENCE_PLATES)) {
            ocrSettings.addParser(OCR_PARSER_NAME, new LicensePlatesParserSettings());
            return new RecognizerSettings[]{ocrSettings};

        } else if (type.equals(RecognizerType.VIN)) {
            ocrSettings.addParser(OCR_PARSER_NAME, new VinParserSettings());

            VinRecognizerSettings vinRecognizerSettings = new VinRecognizerSettings();

            return new RecognizerSettings[]{vinRecognizerSettings, ocrSettings};
        }

        return null;
    }

    @Override
    public void onMetadataAvailable(Metadata metadata) {
        if (metadata instanceof DetectionMetadata) {
            DetectorResult detectionResult = ((DetectionMetadata) metadata).getDetectionResult();
            if (detectionResult == null) {
                if (mPointSetView != null) {
                    mPointSetView.setPointsDetectionResult(null);
                }
            } else if (detectionResult instanceof PointsDetectorResult) {
                if (mPointSetView != null) {
                    mPointSetView.setPointsDetectionResult((PointsDetectorResult) detectionResult);
                }
                if (mOcrResultView != null) {
                    mOcrResultView.clearOcrResults();
                }
            }

        } else if (metadata instanceof OcrMetadata) {
            if (mPointSetView != null) {
                mPointSetView.setPointsDetectionResult(null);
            }
            if (mOcrResultView != null) {
                mOcrResultView.setOcrResult(((OcrMetadata) metadata).getOcrResult());
            }

        } else if (metadata instanceof ImageMetadata) {
            Image image = ((ImageMetadata) metadata).getImage();
            if (image != null && image.getImageType() == ImageType.SUCCESSFUL_SCAN) {
                mResultImage = image.clone();
            }
            if (mPointSetView != null) {
                mPointSetView.setPointsDetectionResult(null);
            }
            if (mOcrResultView != null) {
                mOcrResultView.clearOcrResults();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mActivityState = ActivityState.STARTED;
        if (mRecognizerView != null) {
            mRecognizerView.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mActivityState = ActivityState.RESUMED;
        if (mRecognizerView != null) {
            mRecognizerView.resume();

            // Not displaying results, continue scanning.
            if (mScanResultStringView.getVisibility() != View.VISIBLE) {
                mRecognizerView.resumeScanning(false);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mActivityState = ActivityState.STARTED;
        if (mRecognizerView != null) {
            mRecognizerView.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mActivityState = ActivityState.CREATED;
        if (mRecognizerView != null) {
            mRecognizerView.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mActivityState = ActivityState.DESTROYED;
        if (mRecognizerView != null) {
            mRecognizerView.destroy();
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mRecognizerView != null) {
            mRecognizerView.changeConfiguration(newConfig);
        }
        resizeScanningRegion();
    }

    @Override
    public void onActivityFlip() {
        if (mPointSetView != null) {
            mPointSetView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
        }
        if (mOcrResultView != null) {
            mOcrResultView.setHostActivityOrientation(mRecognizerView.getHostScreenOrientation());
        }
    }

    public void onButtonClicked(View view) {
        int id = view.getId();
        if (id == mFakeR.getId("btn_accept")) {
            Intent intent = new Intent();
            intent.putExtra(EXTRAS_RESULT_STRING, mScanResultStringView.getText().toString());
            setResult(RESULT_OK, intent);
            finish();

        } else if (id == mFakeR.getId("btn_cancel")) {
            setResult(RESULT_CANCELED);
            finish();

        } else if (id == mFakeR.getId("btn_repeat")) {
            mScanResultImageView.setBackground(null);
            mScanResultImageView.setVisibility(View.GONE);
            mScanResultStringView.setVisibility(View.INVISIBLE);
            mAcceptButton.setEnabled(false);
            mRepeatButton.setEnabled(false);
            mRecognizerView.resumeScanning(true);
        }
    }


    @Override
    public void onScanningDone(@Nullable final RecognitionResults recognitionResults) {
        if (mRecognizerView != null) {
            mRecognizerView.pauseScanning();
        }

        // Resume scanning if RecognizerView is not initialized, if no results were found or if empty result is found
        if (mRecognizerView != null && (recognitionResults == null || recognitionResults.getRecognitionResults() == null || recognitionResults.getRecognitionResults().length == 0 || recognitionResults.getRecognitionResults()[0].isEmpty())) {
            mRecognizerView.resumeScanning(true);
            return;
        }

        if (!isFinishing() && mRecognizerView != null && mRecognizerView.getCameraViewState() == BaseCameraView.CameraViewState.RESUMED) {
            final BaseRecognitionResult result = recognitionResults.getRecognitionResults()[0];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAcceptButton.setEnabled(true);
                    mRepeatButton.setEnabled(true);
                    mScanResultImageView.setVisibility(View.VISIBLE);
                    mScanResultStringView.setVisibility(View.VISIBLE);

                    if (mResultImage != null) {

                        Bitmap bitmap = mResultImage.convertToBitmap();
                        if (bitmap == null) {
                            return;
                        }
                        Matrix matrix = new Matrix();
                        switch (mResultImage.getImageOrientation()) {
                            case ORIENTATION_LANDSCAPE_LEFT:
                                matrix.postRotate(180);
                                break;
                            case ORIENTATION_PORTRAIT:
                                matrix.postRotate(90);
                                break;
                            case ORIENTATION_PORTRAIT_UPSIDE:
                                matrix.postRotate(-90);
                                break;
                        }

                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        mScanResultImageView.setImageBitmap(bitmap);
                    }

                    String resultString = mFakeR.getString("blinkid_unknown_result");

                    if (result instanceof VinScanResult) {
                        resultString = ((VinScanResult) result).getVin();

                    } else if (result instanceof BlinkInputRecognitionResult) {
                        BlinkInputRecognitionResult biResult = (BlinkInputRecognitionResult) result;
                        if (biResult.isValid() && !biResult.isEmpty()) {
                            String parsedAmount = biResult.getParsedResult(OCR_PARSER_NAME);
                            if (parsedAmount != null && !parsedAmount.isEmpty()) {
                                resultString = parsedAmount;
                            }
                        } else {
                            resultString = mFakeR.getString("blinkid_invalid_result_message");
                        }
                    }

                    mScanResultStringView.setText(resultString);
                }
            });
        } else {
            mRecognizerView.resumeScanning(false);
        }
    }

    @Override
    public void onCameraPreviewStarted() {
        resizeScanningRegion();
    }

    @Override
    public void onCameraPreviewStopped() {

    }

    @Override
    public void onError(Throwable throwable) {
        if (mActivityState == ActivityState.RESUMED || mActivityState == ActivityState.STARTED) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setCancelable(false)
                    .setTitle(mFakeR.getString("blinkid_error_dialog_title"))
                    .setMessage(throwable.getClass().getSimpleName() + ": " + throwable.getMessage())
                    .setNeutralButton(mFakeR.getString("blinkid_error_dialog_ok"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dialog != null) dialog.dismiss();
                            finish();
                        }
                    }).create().show();
        }
    }

    @Override
    @TargetApi(23)
    public void onCameraPermissionDenied() {
        mCameraPermManager.askForCameraPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCameraPermManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAutofocusFailed() {
    }

    @Override
    public void onAutofocusStarted(Rect[] rects) {

    }

    @Override
    public void onAutofocusStopped(Rect[] rects) {

    }
}
