package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

public class MMPAuthUsingPID extends MMPCtrlMsg {
    private static final String tag = "MMPAuthUsingPID";

    public MMPAuthUsingPID(String pid) {
        super(MMPConstants.MMP_CODE_PID_AUTH);
        addParam(pid.getBytes());
    }

    // For parsing response message
    public MMPAuthUsingPID(MMPCtrlMsg copy) {
        super(copy);
    }

    public int getResult() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {
            return 0;
        } else {
            int authResp = getIntParam();
            dbgTrace("Error response received: " + authResp);
            return authResp;
        }
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMPAuthUsingPID.log", trace);
    }
}
