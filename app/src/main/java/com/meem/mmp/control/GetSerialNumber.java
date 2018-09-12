package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetSerialNumber;

public class GetSerialNumber extends MMPHandler {

    public GetSerialNumber(ResponseCallback responseCallback) {
        super("GetSerialNumber", MMPConstants.MMP_CODE_GET_SERIAL_NUMBER, responseCallback);
    }

    @Override
    protected boolean kickStart() {
        MMPGetSerialNumber getSerial = new MMPGetSerialNumber();
        return getSerial.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_SERIAL_NUMBER) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetSerialNumber object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        MeemEvent evt;

        MMPGetSerialNumber getSerial = new MMPGetSerialNumber(msg);
        if (getSerial.isError()) {
            mUiCtxt.log(UiContext.DEBUG, "GET_GET_SERIAL_NUMBER error response");
            postResult(false, msg.getErrorCode(), "Error", null);

        } else if (getSerial.isSuccess()) {
            mUiCtxt.log(UiContext.DEBUG, "GET_GET_SERIAL_NUMBER success response");
            postResult(true, 0, getSerial.getSerialNumber(), null);
        }

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetSerialNumber TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, "Error", null);
        return false;
    }
}
