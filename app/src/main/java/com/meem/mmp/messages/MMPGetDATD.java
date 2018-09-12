package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetDATD extends MMPCtrlMsg {

    public MMPGetDATD(byte handle, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_DATD);
        addParam(handle);
        addParam(fpath.asArray());
    }
}
