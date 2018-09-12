package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPToggleRNDIS;

/**
 * @author Arun T A Warning: Use only for testing. Nothing is guaranteed other than command will be sent.
 */

public class ToggleRNDIS extends MMPHandler {
    private byte mMode;

    public ToggleRNDIS(byte mode, ResponseCallback responseCallback) {
        super("ToggleRNDIS", MMPConstants.MMP_CODE_TOGGLE_RNDIS, responseCallback);
        mMode = mode;
    }

    @Override
    protected boolean kickStart() {
        MMPToggleRNDIS toggleRNDIS = new MMPToggleRNDIS(mMode);
        return toggleRNDIS.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_TOGGLE_RNDIS) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "ToggleRNDIS object got unknown message");
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
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}

