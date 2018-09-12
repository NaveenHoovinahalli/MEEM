package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetSmartData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Arun T A
 */

public class GetSmartData extends MMPHandler {
    byte mHandle;
    HashMap<Byte, ArrayList<MMPFPath>> mFileList;
    ArrayList<Byte> mCatCodes;
    int mCatsIndex;
    int mCatsCount;

    public GetSmartData(byte handle, ArrayList<Byte> cats, HashMap<Byte, ArrayList<MMPFPath>> fileList, ResponseCallback responseCallback) {
        super("GetSmartData", MMPConstants.MMP_CODE_GET_SMART_DATA, responseCallback);

        if (0 == cats.size()) {
            mUiCtxt.log(UiContext.ERROR, "GetSmartData: No smart data specified in this handler is a harmless controller layer bug!");
        }

		/*
         * Note: Arun: Removed after implementation of category filtering of
		 * individual category backup. if(cats.size() != fileList.size()) {
		 * throw new IllegalArgumentException(
		 * "There should be as many files as there are categories"); }
		 */

        mHandle = handle;
        mFileList = fileList;
        mCatCodes = cats;
        mCatsIndex = 0;
        mCatsCount = cats.size();
    }

    private boolean sendCommand() {
        // May happen if there is no smart data after filtering. (See note
        // above)
        // This is a safety measure only as this will never get called if there
        // is no smart data cats after filtering.
        if (mCatsCount == 0) {
            postResult(true, 0, null, null);
            return false;
        }

        Byte catCode = mCatCodes.get(mCatsIndex);
        ArrayList<MMPFPath> fpathList = mFileList.get(catCode);
        if (fpathList.size() != 2) { // mirror and mirror plus must be there
            throw new IllegalArgumentException("You must provide mirror and mirror plus FPATH, even if its FPATH_NULL");
        }
        MMPGetSmartData getSmart = new MMPGetSmartData(mHandle, catCode, fpathList.get(0), fpathList.get(1));
        return getSmart.send();
    }

    @Override
    protected boolean kickStart() {
        return sendCommand();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_SMART_DATA) {
            if (msgCode == MMPConstants.MMP_CODE_ABORT_SESSION) {
                mUiCtxt.log(UiContext.ERROR, "GetSmartData object got abort response");
                postResult(false, MMPConstants.MMP_CODE_ABORT_SESSION, null, null);
                return false;
            } else {
                // ignore and continue.
                mUiCtxt.log(UiContext.ERROR, "GetCopySmartData object got unknown message");
                msg.dbgDumpBuffer();
                return true;
            }
        }

        if (msg.isAck()) {
            return true;
        }

        if (msg.isError()) {
            mUiCtxt.log(UiContext.INFO, "Get smart data got error response from cable!");
            postResult(false, msg.getErrorCode(), null, null);
            return false;
        }

        mCatsIndex++;

        if (mCatsIndex == mCatsCount) {
            // All done. Post success event.
            postResult(true, 0, null, null);
            return false;
        } else {
            return sendCommand();
        }
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetSmartData TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
