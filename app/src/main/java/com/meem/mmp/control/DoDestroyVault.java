package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPDestroyVault;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class DoDestroyVault extends MMPHandler {

    MMPUpid mUpid;

    public DoDestroyVault(MMPUpid upid, ResponseCallback responseCallback) {
        super("DoDestroyVault", MMPConstants.MMP_CODE_DESTROY_VAULT, responseCallback);
        mUpid = upid;
    }

    @Override
    protected boolean kickStart() {
        MMPDestroyVault resetCmd = new MMPDestroyVault(mUpid);
        return resetCmd.send();
    }

    @Override
    public boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_DESTROY_VAULT) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "DoDestroyVault object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true; // wait further
        } else if (msg.isError()) {
            postResult(false, msg.getErrorCode(), mUpid.toString(), null);
        } else if (msg.isSuccess()) {
            postResult(true, 0, mUpid.toString(), null);
        }
        return false;
    }

    @Override
    public boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "DestroyVault TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, mUpid.toString(), null);
        return false;
    }
}
