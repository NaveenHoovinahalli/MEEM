package com.meem.mmp.messages;

public class MMPCableAuth extends MMPCtrlMsg {
    public MMPCableAuth() {
        super(MMPConstants.MMP_CODE_AUTH_CABLE);
    }

    // For parsing response message
    public MMPCableAuth(MMPCtrlMsg copy) {
        super(copy);
    }
}
