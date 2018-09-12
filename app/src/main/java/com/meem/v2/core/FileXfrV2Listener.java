package com.meem.v2.core;

import com.meem.events.ResponseCallback;

public interface FileXfrV2Listener {
    int getPayloadSize();
    void onXfrRecvStateChange(byte xfrId, boolean xfrCodeExpected);
    void onXfrCompletion(byte xfrId, String fileName, ResponseCallback uicb);
    void onXfrError(byte xfrId, String fileName, int result, ResponseCallback uicb);
    boolean sendXfrData(byte[] buffer);
    boolean sendXfrCtrlData(byte[] buffer);
}
