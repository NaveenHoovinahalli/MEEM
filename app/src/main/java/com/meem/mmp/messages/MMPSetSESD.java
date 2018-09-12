package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSetSESD extends MMPCtrlMsg {

    public MMPSetSESD(byte handle, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SET_SESD);
        addParam(handle);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
