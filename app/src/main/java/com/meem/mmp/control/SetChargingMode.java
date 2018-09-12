package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPSetChargingMode;

/**
 * @author Arun T A Warning: Use only for testing. Nothing is guaranteed other than command will be sent.
 */

public class SetChargingMode extends MMPHandler {
    private byte mMode;

    public SetChargingMode(byte mode, ResponseCallback responseCallback) {
        super("SetChargingMode", MMPConstants.MMP_CODE_SET_CHARGING_MODE, responseCallback);
        mMode = mode;
    }

    @Override
    protected boolean kickStart() {
        MMPSetChargingMode chargeModeCmd = new MMPSetChargingMode(mMode);
        return chargeModeCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SET_CHARGING_MODE) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SetChargingMode object got unknown message");
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
        return false;
    }
}
