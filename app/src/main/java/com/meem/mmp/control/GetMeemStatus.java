package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetMSTAT;

/**
 * @author Arun T A
 */

public class GetMeemStatus extends MMPHandler {
    private MMPFPath mMstatPath;

    public GetMeemStatus(MMPFPath fpath, ResponseCallback onCompletion) {
        super("GetMeemStatus", MMPConstants.MMP_CODE_GET_MSTAT, onCompletion);
        mMstatPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPGetMSTAT mstat = new MMPGetMSTAT(mMstatPath);
        return mstat.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_MSTAT) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetMeemStatus object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "GetMeemStatus TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
