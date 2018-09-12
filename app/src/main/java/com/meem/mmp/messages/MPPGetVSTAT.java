package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MPPGetVSTAT extends MMPCtrlMsg {

    public MPPGetVSTAT(MMPUpid upid, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_VSTAT);
        addParam(upid.asArray());
        addParam(fpath.asArray());
    }
}
