package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFwUpdate;

/**
 * @author Arun T A
 */

public class UpdateFirmware extends MMPHandler {

    String mFwFilePath;

    public UpdateFirmware(String fwFilePath, ResponseCallback responseCallback) {
        super("UpdateFirmware", MMPConstants.MMP_CODE_UPDATE_FIRMWARE, responseCallback);
        mFwFilePath = fwFilePath;
    }

    @Override
    protected boolean kickStart() {
        MMPFwUpdate fwUpdateCmd = new MMPFwUpdate(mFwFilePath);
        return fwUpdateCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_UPDATE_FIRMWARE) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "UpdateFirmware object got unknown message:");
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
        /**
         * Change note: 28Oct2015: FW will take time to finish the update if
         * there is a file system update as a part of FW update.
         */
        mUiCtxt.log(UiContext.DEBUG, "UpdateFirmware TIMEOUT (ignored)");
        return true;
    }
}
