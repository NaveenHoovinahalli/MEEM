package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPExecuteSession extends MMPCtrlMsg {

    public MMPExecuteSession(byte handle) {
        super(MMPConstants.MMP_CODE_EXECUTE_SESSION);
        addParam(handle);
    }
}
