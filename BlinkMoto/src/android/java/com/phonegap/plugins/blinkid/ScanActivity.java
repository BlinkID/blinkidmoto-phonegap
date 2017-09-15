package com.phonegap.plugins.blinkid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
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

import com.microblink.blinkmoto.R;
import com.microblink.detectors.DetectorResult;
import com.microblink.detectors.points.PointsDetectorResult;
import com.microblink.geometry.Rectangle;
import com.microblink.hardware.camera.CameraType;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.image.Image;
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
import com.microblink.util.RecognizerCompatibility;
import com.microblink.view.BaseCameraView;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.NonLandscapeOrientationNotSupportedException;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.ocrResult.IOcrResultView;
import com.microblink.view.ocrResult.OcrResultHorizontalDotsView;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.PointSetView;

public class ScanActivity extends Activity implements ScanResultListener, CameraEventsListener, MetadataListener {
    public static final String EXTRAS_LICENSE_KEY = "key_license_string";
    public static final String EXTRAS_RECOGNIZER_TYPE = "key_recognizer_type_string";
    public static final String EXTRAS_TITLE_STRING = "key_title_string";
    public static final String EXTRAS_ACCEPT_STRING = "key_accept_string";
    public static final String EXTRAS_CANCEL_STRING = "key_cancel_string";
    public static final String EXTRAS_REPEAT_STRING = "key_repeat_string";

    public static final String EXTRAS_RESULT_STRING = "key_result_string";

    public static enum RecognizerType {
        VIN, LICENCE_PLATES
    }

    private final String OCR_PARSER_NAME = "parser";

    private CameraPermissionManager mCameraPermManager;
    private RecognizerView mRecognizerView;
    private PointSetView mPointSetView;
    private IOcrResultView mOcrDotsView;

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
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.custom_scan_layout);
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

        mRecognizerView = (RecognizerView) findViewById(R.id.recognizerView);

        try {
            // Set license key.
            mRecognizerView.setLicenseKey(extras.getString(EXTRAS_LICENSE_KEY));
        } catch (InvalidLicenceKeyException exc) {
            finish();
            return;
        }

        // Add the camera permissions overlay.
        mCameraPermManager = new CameraPermissionManager(this);
        FrameLayout root = (FrameLayout) findViewById(R.id.recognozerViewRoot);
        View cameraPermissionView;
        if ((cameraPermissionView = mCameraPermManager.getAskPermissionOverlay()) != null) {
            root.addView(cameraPermissionView);
        }

        // Setup array of recognition settings.
        RecognitionSettings settings = new RecognitionSettings();
        if(extras.getSerializable(EXTRAS_RECOGNIZER_TYPE) == null) {
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
        // Set camera aspect mode
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);
        // Set the recognizer view scanning region.
        mRecognizerView.setScanningRegion(new Rectangle(.06f, .42f, .88f, .16f), true);
        // Allow all orientations.
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return true;
            }
        });
//        mRecognizerView.setAnimateRotation(true);

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
        mOcrDotsView = new OcrResultHorizontalDotsView(this, null, mRecognizerView.getHostScreenOrientation());
        mRecognizerView.addChildView(mOcrDotsView.getView(), false);

        // Inflate the overlay view.
        final ViewGroup overlay = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_scan_overlay, null);
        // Bind view elements.
        mScanTitleView = (TextView) overlay.findViewById(R.id.tv_scan_title);
        mScanResultStringView = (TextView) overlay.findViewById(R.id.tv_scan_result);
        mScanResultImageView = (ImageView) overlay.findViewById(R.id.iv_scan_result);
        mAcceptButton = (Button) overlay.findViewById(R.id.btn_accept);
        mCancelButton = (Button) overlay.findViewById(R.id.btn_cancel);
        mRepeatButton = (Button) overlay.findViewById(R.id.btn_repeat);
        // Set user defined titles.
        mScanTitleView.setText(extras.getString(EXTRAS_TITLE_STRING, "Position the data here"));
        mAcceptButton.setText(extras.getString(EXTRAS_ACCEPT_STRING, "Accept"));
        mCancelButton.setText(extras.getString(EXTRAS_CANCEL_STRING, "Cancel"));
        mRepeatButton.setText(extras.getString(EXTRAS_REPEAT_STRING, "Repeat"));
        // Result image invisible at start.
        mScanResultImageView.setVisibility(View.INVISIBLE);
        // Cannot accept or retry when scanning is in progress.
        mRepeatButton.setEnabled(false);
        mAcceptButton.setEnabled(false);
        // Add the overlay to the recognizer view.
        mRecognizerView.addChildView(overlay, true);
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
                if (mOcrDotsView != null) {
                    mOcrDotsView.setOcrResult(null);
                }
            }
        } else if (metadata instanceof OcrMetadata) {
            if (mPointSetView != null) {
                mPointSetView.setPointsDetectionResult(null);
            }
            if (mOcrDotsView != null) {
                mOcrDotsView.setOcrResult(((OcrMetadata) metadata).getOcrResult());
            }
        } else if (metadata instanceof ImageMetadata) {
            Image image = ((ImageMetadata) metadata).getImage();
            if (image != null) {
                mResultImage = image.clone();
            }
            if (mPointSetView != null) {
                mPointSetView.setPointsDetectionResult(null);
            }
            if (mOcrDotsView != null) {
                mOcrDotsView.setOcrResult(null);
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
            if (mScanResultStringView == null) {
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

    public void onButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_accept:
                Intent intent = new Intent();
                intent.putExtra(EXTRAS_RESULT_STRING, mScanResultStringView.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
                break;

            case R.id.btn_cancel:
                setResult(RESULT_CANCELED);
                finish();
                break;

            case R.id.btn_repeat:
                mScanResultImageView.setBackground(null);
                mScanResultImageView.setVisibility(View.INVISIBLE);
                mScanResultStringView.setVisibility(View.INVISIBLE);
                mAcceptButton.setEnabled(false);
                mRepeatButton.setEnabled(false);
                mRecognizerView.resumeScanning(false);
                break;
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

                    String resultString = "Unknown result";

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
                            resultString = "Invalid result, please try again.";
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

    }

    @Override
    public void onCameraPreviewStopped() {

    }

    @Override
    public void onError(Throwable throwable) {
        if (mActivityState == ActivityState.RESUMED || mActivityState == ActivityState.STARTED) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setCancelable(false)
                    .setTitle("Error")
                    .setMessage(throwable.getClass().getSimpleName() + ": " + throwable.getMessage())
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
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
