package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPModifyPINF extends MMPCtrlMsg {

    public MMPModifyPINF(MMPUpid upid, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_MODIFY_PINF);
        addParam(upid.asArray());
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
