package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFactoryReset;
import com.meem.mmp.messages.MMPUpid;

/**
 * @author Arun T A
 */

public class DoFactoryReset extends MMPHandler {

    MMPUpid mUpid;

    public DoFactoryReset(MMPUpid upid, ResponseCallback responseCallback) {
        super("DoFactoryReset", MMPConstants.MMP_CODE_RESET_CABLE, responseCallback);
        mUpid = upid;
    }

    @Override
    protected boolean kickStart() {
        MMPFactoryReset resetCmd = new MMPFactoryReset(mUpid);
        return resetCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_RESET_CABLE) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "DoFactoryReset object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true; // wait further
        } else if (msg.isError()) {
            postResult(false, msg.getErrorCode(), null, null);
        } else if (msg.isSuccess()) {
            postResult(true, msg.getErrorCode(), null, null);
        }
        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "FactoryReset TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
