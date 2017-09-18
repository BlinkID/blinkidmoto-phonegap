/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.phonegap.plugins.blinkid;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.microblink.activity.ScanCard;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BlinkIdScanner extends CordovaPlugin {

    private static final int REQUEST_CODE = 1337;

    /** Index of the license key for Android in the exec() arguments. */
    private static final int ARG_LICENSE_KEY_INDEX = 1;
    /** Index of the translations JSON in the exec() arguments. */
    private static final int ARG_TRANSLATIONS_INDEX = 2;


    /** Name of the scan VIN method from Javascript */
    private static final String SCAN = "scan";
    /** Name of the scan License Plate method from Javascript */
    private static final String SCAN_LICENSE_PLATE = "scanLicensePlate";
    private static final String IS_SCANNING_UNSUPPORTED = "isScanningUnsupportedForCameraType";

    // Result JSON keys
    /** Result JSON object key for boolean value which indicates whether scanning was canceled */
    private static final String RES_KEY_CANCELLED = "cancelled";
    /** Result JSON object key for scanning result string value */
    private static final String RES_KEY_RESULT = "result";


    private static final String LOG_TAG = "BlinkIdScanner";

    private CallbackContext mCallbackContext;

    /**
     * Constructor.
     */
    public BlinkIdScanner() {
    }

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount
     * of work, use: cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     * cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action
     *            The action to execute.
     * @param args
     *            The exec() arguments.
     * @param callbackContext
     *            The callback context used when calling back into JavaScript.
     * @return Whether the action was valid.
     *
     * @sa
     *     https://github.com/apache/cordova-android/blob/master/framework/src/org
     *     /apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        mCallbackContext = callbackContext;

        ScanActivity.RecognizerType recognizerType;
        if (action.equals(SCAN)) {
            Log.e(LOG_TAG, "SCAN");
            recognizerType = ScanActivity.RecognizerType.VIN;
        } else if (action.equals(SCAN_LICENSE_PLATE)) {
            Log.e(LOG_TAG, "SCAN LICENSE PLATE");
            recognizerType = ScanActivity.RecognizerType.LICENCE_PLATES;
        } else if (action.equals(IS_SCANNING_UNSUPPORTED)) {
            Log.e(LOG_TAG, "IS SCANNING UNSUPPORTED");
            return true;
        } else {
            return false;
        }

        String licenseKey;
        if (!args.isNull(ARG_LICENSE_KEY_INDEX)) {
            licenseKey = args.optString(ARG_LICENSE_KEY_INDEX);
        } else {
            return false;
        }

        TranslationSettings translationSettings;
        if (!args.isNull(ARG_TRANSLATIONS_INDEX)) {
            try {
                translationSettings = TranslationSettings.readFromJSON(args.getJSONObject(ARG_TRANSLATIONS_INDEX));
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }

        scan(recognizerType, licenseKey, translationSettings);
        return true;
    }


    /**
     * Starts an intent from provided activity to scan and return result.
     */
    public void scan(ScanActivity.RecognizerType recognizerType, String license, TranslationSettings translationSettings) {

        Context context = this.cordova.getActivity().getApplicationContext();

        Intent intent = new Intent(context, ScanActivity.class);

        intent.putExtra(ScanActivity.EXTRAS_LICENSE_KEY, license);
        intent.putExtra(ScanActivity.EXTRAS_RECOGNIZER_TYPE, recognizerType);

        intent.putExtra(ScanActivity.EXTRAS_TITLE_STRING, translationSettings.getTitleText());
        intent.putExtra(ScanActivity.EXTRAS_ACCEPT_STRING, translationSettings.getAcceptText());
        intent.putExtra(ScanActivity.EXTRAS_REPEAT_STRING, translationSettings.getRepeatText());
        intent.putExtra(ScanActivity.EXTRAS_CANCEL_STRING, translationSettings.getCancelText());

        this.cordova.startActivityForResult(this, intent, REQUEST_CODE);
    }

    /**
     * Called when the scanner intent completes.
     *
     * @param requestCode
     *            The request code originally supplied to
     *            startActivityForResult(), allowing you to identify who this
     *            result came from.
     * @param resultCode
     *            The integer result code returned by the child activity through
     *            its setResult().
     * @param data
     *            An Intent, which can return result data to the caller (various
     *            data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE) {

            if (resultCode == ScanCard.RESULT_OK) {
                String resultString = data.getStringExtra(ScanActivity.EXTRAS_RESULT_STRING);
                try {
                    JSONObject root = new JSONObject();
                    root.put(RES_KEY_CANCELLED, 0);
                    root.put(RES_KEY_RESULT, resultString);
                    if (mCallbackContext != null) {
                        mCallbackContext.success(root);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "This should never happen");
                }
            } else if (resultCode == ScanCard.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(RES_KEY_CANCELLED, 1);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "This should never happen");
                }
                if (mCallbackContext != null) {
                    mCallbackContext.success(obj);
                }
            } else {
                if (mCallbackContext != null) {
                    mCallbackContext.error("Unexpected error");
                }
            }
        }
    }

    /**
     * Scan activity strings.
     */
    private static class TranslationSettings {

        /** Javascript key for title text. */
        private static final String JSON_KEY_TITLE_TEXT = "title_text";
        /** Javascript key for cancle button text. */
        private static final String JSON_KEY_CANCEL_TEXT = "cancel_text";
        /** Javascript key for repeat button text. */
        private static final String JSON_KEY_REPEAT_TEXT = "repeat_text";
        /** Javascript key for accept button text. */
        private static final String JSON_KEY_ACCEPT_TEXT = "accept_text";

        private String mTitleText;
        private String mCancelText;
        private String mRepeatText;
        private String mAcceptText;

        /**
         * Reads translations from JSONObject. If some translation does not exist, sets its value to
         * empty string.
         * @param translationJson translations JSON object.
         * @return built translation settings object from the given JSON.
         */
        public static TranslationSettings readFromJSON(JSONObject translationJson) {
            TranslationSettings translationSett = new TranslationSettings();
            translationSett.mTitleText = translationJson.optString(JSON_KEY_TITLE_TEXT, null);
            translationSett.mCancelText = translationJson.optString(JSON_KEY_CANCEL_TEXT, null);
            translationSett.mRepeatText = translationJson.optString(JSON_KEY_REPEAT_TEXT, null);
            translationSett.mAcceptText = translationJson.optString(JSON_KEY_ACCEPT_TEXT, null);
            return translationSett;
        }

        /**
         * Returns text for the title.
         * @return text for the title or {@code null} if it does not exist.
         */
        public String getTitleText() {
            return mTitleText;
        }

        /**
         * Returns cancel button text.
         * @return cancel button text or {@code null} if it does not exist.
         */
        public String getCancelText() {
            return mCancelText;
        }

        /**
         * Returns repeat button text.
         * @return repeat button text or {@code null} if it does not exist.
         */
        public String getRepeatText() {
            return mRepeatText;
        }

        /**
         * Returns accept button text.
         * @return accept button text or {@code null} if it does not exist.
         */
        public String getAcceptText() {
            return mAcceptText;
        }
    }

}
