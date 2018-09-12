package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSendThumbnailDb;

public class SendThumbnailDb extends MMPHandler {
    private MMPFPath mDbPath;

    public SendThumbnailDb(MMPFPath fpath, ResponseCallback callback) {
        super("SendthumbnailDb", MMPConstants.MMP_CODE_SEND_THUMBNAIL_DB, callback);
        mDbPath = fpath;
    }

    @Override
    protected boolean kickStart() {
        MMPSendThumbnailDb sendThumbDb = new MMPSendThumbnailDb(mDbPath);
        return sendThumbDb.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_SEND_THUMBNAIL_DB) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "SendThumbnailDb object got unknown message:");
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
        mUiCtxt.log(UiContext.DEBUG, "SendThumbnailDb TIMEOUT. Ignored.");
        return true;
    }

}
