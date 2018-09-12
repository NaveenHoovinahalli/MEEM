package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetSingleFile;
import com.meem.mmp.messages.MMPSingleFileSpec;

import java.util.ArrayList;

/**
 * Created by arun on 21/11/16.
 */

public class GetSessionlessGenericData extends MMPHandler {
    ArrayList<MMPSingleFileSpec> mSpecList;
    ArrayList<Integer> mErrorIndexes;

    int mIndex;

    public GetSessionlessGenericData(ArrayList<MMPSingleFileSpec> specList, ResponseCallback responseCallback) {
        super("GetSessionlessGenericData", MMPConstants.MMP_CODE_APP_GET_SESSIONLESS_GENERICDATA, responseCallback);
        mSpecList = specList;
        mErrorIndexes = new ArrayList<>();
    }


    private boolean sendCommand() {
        MMPSingleFileSpec spec = mSpecList.get(mIndex);
        MMPGetSingleFile singleFile = new MMPGetSingleFile(spec);
        return singleFile.send();
    }

    @Override
    protected boolean kickStart() {
        if (0 == mSpecList.size()) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessGenericData: Invoked with empty list.");
            postResult(true, 0, mSpecList, mErrorIndexes);
            return false;
        }

        return sendCommand();
    }

    @Override
    public boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_SINGLE_FILE) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessGenericData object got an unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        if (msg.isError()) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessGenericData got error response. Continuing.");
            mErrorIndexes.add(mIndex);
        }

        mIndex++;

        if (mIndex == mSpecList.size()) {
            // All done
            postResult(mErrorIndexes.size() == 0, 0, mSpecList, mErrorIndexes);
            return false;
        } else {
            return sendCommand();
        }
    }

    @Override
    public boolean onMMPTimeout() {
        // BugFix 6Dec2016: Timeout will cause the whole loop to terminate if large files (esp videos) are transferred.
        mUiCtxt.log(UiContext.DEBUG, "GetSessionlessGenericData TIMEOUT. Ignoring.");
        /*postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, mSpecList, mErrorIndexes);*/
        return true;
    }
}
