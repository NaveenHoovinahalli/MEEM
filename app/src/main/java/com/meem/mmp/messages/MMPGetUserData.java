package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPGetUserData extends MMPCtrlMsg {
    public MMPGetUserData(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_USER_DATA);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
