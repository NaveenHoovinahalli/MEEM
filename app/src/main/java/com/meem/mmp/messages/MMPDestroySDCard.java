package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPDestroySDCard extends MMPCtrlMsg {

    public MMPDestroySDCard(MMPUpid upid, MMPUmid umid) {
        super(MMPConstants.MMP_CODE_DESTROY_SDCARD);
        addParam(upid.asArray());
        addParam(umid.asArray());
    }
}
