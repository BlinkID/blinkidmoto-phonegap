package com.phonegap.plugins.blinkid.resulthistory;

public class AcceptFirstResultHistory implements ResultHistory {

    private String mResult;

    @Override
    public void onNewResult(String result) {
        mResult = result;
    }

    @Override
    public String getResult() {
        return mResult;
    }

    @Override
    public boolean hasValidResult() {
        return true;
    }

    @Override
    public void clear() {
        mResult = null;
    }

}
