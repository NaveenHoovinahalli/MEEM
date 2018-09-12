package com.meem.mmp.messages;

/**
 * @author Arun T A
 */
public class MMPAbortSession extends MMPCtrlMsg {
    public MMPAbortSession(byte handle) {
        super(MMPConstants.MMP_CODE_ABORT_SESSION);
        addParam(handle);
    }
}
