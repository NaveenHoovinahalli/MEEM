package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPCloseSession;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;

/**
 * @author Arun T A
 */

public class CloseSession extends MMPHandler {
    private byte mHandle;

    public CloseSession(byte handle, ResponseCallback responseCallback) {
        super("CloseSession", MMPConstants.MMP_CODE_CLOSE_SESSION, responseCallback);
        mHandle = handle;
    }

    @Override
    protected boolean kickStart() {
        MMPCloseSession abortCmd = new MMPCloseSession(mHandle);
        return abortCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_CLOSE_SESSION) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "CloseSession object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "CloseSession TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
