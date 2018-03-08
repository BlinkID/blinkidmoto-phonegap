package com.phonegap.plugins.blinkid.resulthistory;

//accept result as valid only if we got it 3 times in a row
public class VinResultHistory implements ResultHistory {

    private static final int SAME_RESULT_COUNT_REQUIRED_FOR_SUCCESS = 3;
    private int mSameScannedVinCount = 0;
    private String mLastScannedVin;

    @Override
    public void onNewResult(String result) {
        if(result != null && result.equals(mLastScannedVin)) {
            mSameScannedVinCount++;
        } else {
            mSameScannedVinCount = 0;
            mLastScannedVin = result;
        }
    }

    @Override
    public String getResult() {
        return mLastScannedVin;
    }

    @Override
    public boolean hasValidResult() {
        return mSameScannedVinCount >= SAME_RESULT_COUNT_REQUIRED_FOR_SUCCESS;
    }

    @Override
    public void clear() {
        mSameScannedVinCount = 0;
        mLastScannedVin = null;
    }

}
