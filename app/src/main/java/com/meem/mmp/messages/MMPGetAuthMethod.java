package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

public class MMPGetAuthMethod extends MMPCtrlMsg {
    private static final String tag = "MMPGetAuthMethod";

    public MMPGetAuthMethod() {
        super(MMPConstants.MMP_CODE_GET_AUTH_DETAILS);
    }

    // For parsing response message
    public MMPGetAuthMethod(MMPCtrlMsg copy) {
        super(copy);
    }

    public int getAuthMethod() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {
            return getIntParam();
        } else {
            dbgTrace("Error response received. Cable is locked");
            return MMPConstants.MMP_ERROR_CABLE_LOCKED;
        }
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMPGetAuthMethod.log", trace);
    }
}
