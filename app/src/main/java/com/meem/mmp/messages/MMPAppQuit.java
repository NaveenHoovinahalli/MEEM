package com.meem.mmp.messages;

public class MMPAppQuit extends MMPCtrlMsg {
    public MMPAppQuit() {
        super(MMPConstants.MMP_CODE_APP_KILL);
    }
}
