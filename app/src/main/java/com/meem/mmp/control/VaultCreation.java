package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCreateVault;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class VaultCreation extends MMPHandler {
    private MMPUpid mUpid;
    private MMPFPath mFPath;

    public VaultCreation(MMPUpid upid, MMPFPath fpath, ResponseCallback responseCallback) {
        super("VaultCreation", MMPConstants.MMP_CODE_CREATE_VAULT, responseCallback);

        mUpid = upid;
        mFPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPCreateVault createVault = new MMPCreateVault(mUpid, mFPath);
        return createVault.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_CREATE_VAULT) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "VaultCreation object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "VaultCreation TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
