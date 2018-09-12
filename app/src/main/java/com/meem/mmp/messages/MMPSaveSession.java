package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSaveSession extends MMPCtrlMsg {

    public MMPSaveSession(byte handle) {
        super(MMPConstants.MMP_CODE_SAVE_SESSION);
        addParam(handle);
    }
}
