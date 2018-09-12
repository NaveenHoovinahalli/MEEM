package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSetMCFG extends MMPCtrlMsg {

    public MMPSetMCFG(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SET_MCFG);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
