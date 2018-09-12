package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPAddSDCard extends MMPCtrlMsg {

    public MMPAddSDCard(MMPUpid upid, MMPUmid umid, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_ADD_SDCARD);
        addParam(upid.asArray());
        addParam(umid.asArray());
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
