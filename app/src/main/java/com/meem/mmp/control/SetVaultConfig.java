package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSetVCFG;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class SetVaultConfig extends MMPHandler {
    private MMPUpid mUpid;
    private MMPFPath mCfgPath;

    public SetVaultConfig(MMPUpid upid, MMPFPath fpath, ResponseCallback callback) {
        super("SetVaultConfig", MMPConstants.MMP_CODE_SET_VCFG, callback);

        mUpid = upid;
        mCfgPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPSetVCFG mstat = new MMPSetVCFG(mUpid, mCfgPath);
        return mstat.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SET_VCFG) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SetVaultConfig object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "SetVaultConfig TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
