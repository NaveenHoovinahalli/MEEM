package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPCloseSession extends MMPCtrlMsg {
    public MMPCloseSession(byte handle) {
        super(MMPConstants.MMP_CODE_CLOSE_SESSION);
        addParam(handle);
    }
}
