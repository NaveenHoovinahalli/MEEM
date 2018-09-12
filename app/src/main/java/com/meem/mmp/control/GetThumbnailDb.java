package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetThumbnailDb;

public class GetThumbnailDb extends MMPHandler {
    MMPFPath mFPath;
    int mDbVersion;

    public GetThumbnailDb(MMPFPath fpath, int dbVersion, ResponseCallback onComplete) {
        super("GetThumbnailDb", MMPConstants.MMP_CODE_GET_THUMBNAIL_DB, onComplete);
        mFPath = fpath;
        mDbVersion = dbVersion;
    }

    @Override
    protected boolean kickStart() {
        MMPGetThumbnailDb getThumbDb = new MMPGetThumbnailDb(mFPath, mDbVersion);
        return getThumbDb.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_THUMBNAIL_DB) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetThumbnailDb object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "GetThumbnailDb TIMEOUT. Ignored.");
        return true;
    }
}
