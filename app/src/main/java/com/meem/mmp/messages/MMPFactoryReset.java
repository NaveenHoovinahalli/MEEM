package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPFactoryReset extends MMPCtrlMsg {
    public MMPFactoryReset(MMPUpid upid) {
        super(MMPConstants.MMP_CODE_RESET_CABLE);
        addParam(upid.asArray());
    }
}
