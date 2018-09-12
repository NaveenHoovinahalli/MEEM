package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPDestroyVault extends MMPCtrlMsg {
    public MMPDestroyVault(MMPUpid upid) {
        super(MMPConstants.MMP_CODE_DESTROY_VAULT);
        addParam(upid.asArray());
    }
}
