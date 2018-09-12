package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPPhoneId;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class RegistrationCheck extends MMPHandler {
    private MMPUpid mUpid;

    public RegistrationCheck(MMPUpid upid, ResponseCallback responseCallback) {
        super("RegistrationCheck", MMPConstants.MMP_CODE_PHONE_ID, responseCallback);
        mUpid = upid;
    }

    @Override
    protected boolean kickStart() {
        MMPPhoneId phoneId = new MMPPhoneId(mUpid);
        return phoneId.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_PHONE_ID) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "RegistrationCheck object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "RegistrationCheck TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
