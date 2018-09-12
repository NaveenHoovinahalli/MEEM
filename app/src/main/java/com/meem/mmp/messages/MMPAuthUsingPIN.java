package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

public class MMPAuthUsingPIN extends MMPCtrlMsg {
    private static final String tag = "MMPAuthUsingPIN";

    private int mAuthResp;
    private byte mTrialsLeft;

    public MMPAuthUsingPIN(String pin) {
        super(MMPConstants.MMP_CODE_PIN_AUTH);
        addParam(pin.getBytes());
    }

    // For parsing response message
    public MMPAuthUsingPIN(MMPCtrlMsg copy) {
        super(copy);
    }

    public int getResult() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {
            return 0;
        } else {
            mAuthResp = getIntParam();
            mTrialsLeft = getByteParam();

            dbgTrace("Error response received: " + mAuthResp + ", Trials left: " + mTrialsLeft);
            return mAuthResp;
        }
    }

    public int getWrongTrialsCount() {
        return mTrialsLeft;
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMPAuthUsingPIN.log", trace);
    }
}
