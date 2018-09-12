package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetFwLog;

public class GetFirmwareLog extends MMPHandler {

    private MMPFPath mFwLogPath;

    public GetFirmwareLog(MMPFPath fpath, ResponseCallback responseCallback) {
        super("GetFirmwareLog", MMPConstants.MMP_CODE_GET_FW_LOG, responseCallback);
        mFwLogPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPGetFwLog fwLog = new MMPGetFwLog(mFwLogPath);
        return fwLog.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_FW_LOG) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetFirmwareLog object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "GetFwLog TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }

}
