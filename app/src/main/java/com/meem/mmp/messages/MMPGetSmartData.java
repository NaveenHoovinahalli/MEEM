package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetSmartData extends MMPCtrlMsg {

    public MMPGetSmartData(byte handle, byte category, MMPFPath mirror, MMPFPath mirrorPlus) {
        super(MMPConstants.MMP_CODE_GET_SMART_DATA);
        addParam(handle);
        addParam(category);
        addParam(mirror.asArray());
        addParam(mirrorPlus.asArray());
    }
}
