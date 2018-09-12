package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.messages.MPPGetVSTAT;

/**
 * @author Arun T A
 */

public class GetVaultStatus extends MMPHandler {
    private MMPUpid mUpid;
    private MMPFPath mVstatPath;

    public GetVaultStatus(MMPUpid upid, MMPFPath fpath, ResponseCallback onCompletion) {
        super("GetVaultStatus", MMPConstants.MMP_CODE_GET_VSTAT, onCompletion);
        mUpid = upid;
        mVstatPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MPPGetVSTAT vstat = new MPPGetVSTAT(mUpid, mVstatPath);
        return vstat.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_VSTAT) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetVaultStatus object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "GetVaultStatus TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
