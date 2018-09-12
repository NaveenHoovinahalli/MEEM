package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSetMCFG;

/**
 * @author Arun T A
 */

public class SetMeemConfig extends MMPHandler {
    private MMPFPath mCfgPath;

    public SetMeemConfig(MMPFPath fpath, ResponseCallback callback) {
        super("SetMeemConfig", MMPConstants.MMP_CODE_SET_MCFG, callback);
        mCfgPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPSetMCFG mstat = new MMPSetMCFG(mCfgPath);
        return mstat.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SET_MCFG) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SetMeemConfig object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "SetMeemconfig TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
