package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSetPassword extends MMPCtrlMsg {
    public MMPSetPassword(String passwd, MMPUpid upid) {
        super(MMPConstants.MMP_CODE_SET_PASSWD);

        if (passwd.length() < MMPConstants.MMP_MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password is too short");
        }

        this.addParam(passwd.getBytes());
        this.addParam(upid.asArray());
    }
}
