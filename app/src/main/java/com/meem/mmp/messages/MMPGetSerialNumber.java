package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

public class MMPGetSerialNumber extends MMPCtrlMsg {
    private static final String tag = "MMPGetSerialNumber";

    public MMPGetSerialNumber() {
        super(MMPConstants.MMP_CODE_GET_SERIAL_NUMBER);
    }

    // For parsing response message
    public MMPGetSerialNumber(MMPCtrlMsg copy) {
        super(copy);
    }

    public String getSerialNumber() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {
            byte len = getByteParam();
            dbgTrace("Serial no length: " + len);

            byte[] serialBytes = new byte[len];
            getByteArrayParam(serialBytes);
            String serialString = new String(serialBytes);
            dbgTrace("Serial no (string): " + serialString);
            return serialString;
        } else {
            dbgTrace("Error response received");
            return "EVTXXXXXX";
        }
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMPGetSerialNumber.log", trace);
    }
}
