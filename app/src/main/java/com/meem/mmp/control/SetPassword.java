package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPSetPassword;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class SetPassword extends MMPHandler {
    private String mPasswd;
    private MMPUpid mSetterUpid;
    private UiContext mUiCtxt = UiContext.getInstance();

    public SetPassword(String password, MMPUpid setterUpid, ResponseCallback callback) {
        super("SetPassword", MMPConstants.MMP_CODE_SET_PASSWD, callback);

        mPasswd = password;
        mSetterUpid = setterUpid;
    }

    @Override
    protected boolean kickStart() {
        MMPSetPassword passCmd = new MMPSetPassword(mPasswd, mSetterUpid);
        return passCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SET_PASSWD) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SetPassword object got unknown message:");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true; // wait further
        } else if (msg.isError()) {
            postResult(false, msg.getErrorCode(), null, null);
        } else if (msg.isSuccess()) {
            postResult(true, 0, null, null);
        }
        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "SetPassword TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
