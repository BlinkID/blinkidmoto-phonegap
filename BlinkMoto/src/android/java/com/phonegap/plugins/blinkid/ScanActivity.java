package com.phonegap.plugins.blinkid;

import android.app.Activity;
import android.os.Bundle;

import com.microblink.blinkmoto.R;
import com.microblink.util.Log;
import android.view.View;

public class ScanActivity extends Activity {

    public static final String EXTRAS_TITLE_STRING = "key_title_string";

    public static final String EXTRAS_ACCEPT_STRING = "key_success_string";
    public static final String EXTRAS_CANCEL_STRING = "key_cancel_string";
    public static final String EXTRAS_REPEAT_STRING = "key_retry_string";

    public static final String EXTRAS_RECOGNIZER_TYPE = "key_recognizer_type_string";

    public static final String EXTRAS_RESULT_STRING = "key_result_string";

    public static final String EXTRAS_LICENSE_KEY = "EXTRAS_LICENSE_KEY";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new View(this));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.w(this, "INTEXTR {}: {}", EXTRAS_TITLE_STRING, extras.getString(EXTRAS_TITLE_STRING));
            Log.w(this, "INTEXTR {}: {}", EXTRAS_ACCEPT_STRING, extras.getString(EXTRAS_ACCEPT_STRING));
            Log.w(this, "INTEXTR {}: {}", EXTRAS_CANCEL_STRING, extras.getString(EXTRAS_CANCEL_STRING));
            Log.w(this, "INTEXTR {}: {}", EXTRAS_REPEAT_STRING, extras.getString(EXTRAS_REPEAT_STRING));

            Log.w(this, "INTEXTR {}: {}", EXTRAS_RECOGNIZER_TYPE, ((RecognizerType)extras.getSerializable(EXTRAS_RECOGNIZER_TYPE)).name());
            Log.w(this, "INTEXTR {}: {}", EXTRAS_LICENSE_KEY, extras.getString(EXTRAS_LICENSE_KEY));

        }
    }

    public static enum RecognizerType {
        VIN, LICENCE_PLATES
    }
}
