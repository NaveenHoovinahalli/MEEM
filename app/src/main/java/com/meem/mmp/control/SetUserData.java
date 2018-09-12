package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSetUserData;

public class SetUserData extends MMPHandler {
    private MMPFPath mFPath;

    public SetUserData(MMPFPath fpath, ResponseCallback responseCallback) {
        super("SetUserData", MMPConstants.MMP_CODE_SET_USER_DATA, responseCallback);
        mFPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPSetUserData setUserData = new MMPSetUserData(mFPath);
        return setUserData.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SET_USER_DATA) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SetUserData object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "SetUserData TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
