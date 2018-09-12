package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetUserData;

public class GetUserData extends MMPHandler {
    private MMPFPath mFPath;

    public GetUserData(MMPFPath fpath, ResponseCallback responseCallback) {
        super("GetUserData", MMPConstants.MMP_CODE_GET_USER_DATA, responseCallback);
        mFPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPGetUserData setUserData = new MMPGetUserData(mFPath);
        return setUserData.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_USER_DATA) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetUserData object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "GetUSerData TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
