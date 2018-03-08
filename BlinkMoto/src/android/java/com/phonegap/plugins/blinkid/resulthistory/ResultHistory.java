package com.phonegap.plugins.blinkid.resulthistory;

public interface ResultHistory {

    void onNewResult(String result);
    String getResult();
    boolean hasValidResult();
    void clear();

}
