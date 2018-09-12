package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetCopyDATD extends MMPCtrlMsg {
    public MMPGetCopyDATD(byte handle, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_CPY_DATD);
        addParam(handle);
        addParam(fpath.asArray());
    }
}
