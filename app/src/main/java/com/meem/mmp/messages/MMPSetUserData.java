package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSetUserData extends MMPCtrlMsg {
    public MMPSetUserData(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SET_USER_DATA);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
