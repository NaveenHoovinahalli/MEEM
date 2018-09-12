package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetPassword;

/**
 * @author Arun T A
 */

public class GetPassword extends MMPHandler {
    private UiContext mUiCtxt = UiContext.getInstance();

    public GetPassword(ResponseCallback callback) {
        super("GetPassword", MMPConstants.MMP_CODE_GET_PASSWD, callback);
        mUiCtxt.log(UiContext.WARNING, "WARINIG: Using GetPassword object is an indication of hack either in app or in fw!");
    }

    @Override
    protected boolean kickStart() {
        MMPGetPassword passCmd = new MMPGetPassword();
        return passCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_PASSWD) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetPassword object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        mUiCtxt.log(UiContext.DEBUG, "GET_PASSWD response received");
        MeemEvent evt;

        MMPGetPassword getPasswd = new MMPGetPassword(msg);
        if (getPasswd.isError()) {
            if (MMPConstants.MMP_ERROR_PASSWORD_UNSET == getPasswd.getErrorCode()) {
                postResult(true, 0, null, null);
            } else {
                mUiCtxt.log(UiContext.DEBUG, "BUG! GET_PASSWD response contains an unknown error code (int): " + getPasswd.getErrorCode());
                msg.dbgDumpBuffer();
                return false;
            }
        } else if (getPasswd.isSuccess()) {
            postResult(true, 0, getPasswd.getMeemPassword(), null);
        }

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetPassword TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
