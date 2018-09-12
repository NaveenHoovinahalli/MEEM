package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSetCopySESD extends MMPCtrlMsg {

    public MMPSetCopySESD(byte handle, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SET_CPY_SESD);
        addParam(handle);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
