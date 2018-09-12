package com.meem.mmp.messages;

public class MMPSetChargingMode extends MMPCtrlMsg {
    public MMPSetChargingMode(byte code) {
        super(MMPConstants.MMP_CODE_SET_CHARGING_MODE);
        addParam(code);
    }
}
