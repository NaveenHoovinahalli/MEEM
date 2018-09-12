package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetCopySmartData extends MMPCtrlMsg {

    public MMPGetCopySmartData(byte handle, byte category, MMPFPath mirror, MMPFPath mirrorPlus) {
        super(MMPConstants.MMP_CODE_GET_CPY_SMART_DATA);
        addParam(handle);
        addParam(category);
        addParam(mirror.asArray());
        addParam(mirrorPlus.asArray());
    }
}
