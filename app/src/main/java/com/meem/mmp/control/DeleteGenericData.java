package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPDeleteSingleFile;
import com.meem.mmp.messages.MMPSingleFileSpec;

import java.util.ArrayList;

/**
 * Created by arun on 21/11/16.
 */

public class DeleteGenericData extends MMPHandler {

    ArrayList<MMPSingleFileSpec> mSpecList;
    ArrayList<Integer> mErrorIndexes;

    int mIndex;

    public DeleteGenericData(ArrayList<MMPSingleFileSpec> specList, ResponseCallback responseCallback) {
        super("DeleteGenericData", MMPConstants.MMP_CODE_APP_DELETE_GENERICDATA, responseCallback);
        mSpecList = specList;
        mErrorIndexes = new ArrayList<Integer>();
    }

    private boolean sendCommand() {
        MMPSingleFileSpec spec = mSpecList.get(mIndex);
        MMPDeleteSingleFile delFile = new MMPDeleteSingleFile(spec);
        return delFile.send();
    }

    @Override
    protected boolean kickStart() {
        if (0 == mSpecList.size()) {
            mUiCtxt.log(UiContext.ERROR, "DeleteGenericData: Invoked with empty list.");
            postResult(true, 0, mSpecList, mErrorIndexes);
            return false;
        }

        return sendCommand();
    }

    @Override
    public boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_DELETE_SINGLE_FILE) {
            mUiCtxt.log(UiContext.ERROR, "DeleteGenericData object got an unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        if (msg.isError()) {
            mUiCtxt.log(UiContext.ERROR, "DeleteGenericData got error response.");
            mErrorIndexes.add(mIndex);
        }

        mIndex++;

        if (mIndex == mSpecList.size()) {
            // All done.
            postResult(mErrorIndexes.size() == 0, 0, mSpecList, mErrorIndexes);
            return false;
        } else {
            return sendCommand();
        }
    }

    @Override
    public boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetSessionlessGenericData TIMEOUT");

        // All done. Remove all error entries from spec list and Post success event.
        for (Integer i : mErrorIndexes) {
            mSpecList.remove(i);
        }

        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, mSpecList, mErrorIndexes);
        return false;
    }
}
