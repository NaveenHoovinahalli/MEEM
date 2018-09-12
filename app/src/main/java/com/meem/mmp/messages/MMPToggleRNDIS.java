package com.meem.mmp.messages;

public class MMPToggleRNDIS extends MMPCtrlMsg {
    public MMPToggleRNDIS(byte mMode) {
        super(MMPConstants.MMP_CODE_TOGGLE_RNDIS);
    }
}
