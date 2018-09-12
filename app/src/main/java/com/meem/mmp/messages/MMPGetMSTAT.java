package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetMSTAT extends MMPCtrlMsg {

    public MMPGetMSTAT(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_MSTAT);
        addParam(fpath.asArray());
    }
}
