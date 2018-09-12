package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPPhoneId extends MMPCtrlMsg {

    public MMPPhoneId(MMPUpid upid) {
        super(MMPConstants.MMP_CODE_PHONE_ID);
        this.addParam(upid.asArray());
    }
}
