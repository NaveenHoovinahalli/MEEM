package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetSingleFile;
import com.meem.mmp.messages.MMPSingleFileSpec;

/**
 * Created by arun on 21/11/16.
 */

public class DeleteSingleFile extends MMPHandler {
    MMPSingleFileSpec mSpec;

    public DeleteSingleFile(MMPSingleFileSpec fileSpec, ResponseCallback callback) {
        super("DeleteSingleFile", MMPConstants.MMP_CODE_DELETE_SINGLE_FILE, callback);
        mSpec = fileSpec;
    }

    @Override
    protected boolean kickStart() {
        MMPGetSingleFile getSingleFile = new MMPGetSingleFile(mSpec);
        return getSingleFile.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_DELETE_SINGLE_FILE) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "DeleteSingleFile object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true; // wait further
        } else if (msg.isError()) {
            postResult(false, msg.getErrorCode(), mSpec, null);
        } else if (msg.isSuccess()) {
            postResult(true, 0, mSpec, null);
        }
        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.ERROR, "DeleteSingleFile TIMEOUT. Ignored");
        return true;
    }
}
