package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPCreateVault extends MMPCtrlMsg {

    public MMPCreateVault(MMPUpid upid, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_CREATE_VAULT);
        addParam(upid.asArray());
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
