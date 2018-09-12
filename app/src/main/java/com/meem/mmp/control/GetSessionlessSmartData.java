package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetSingleSmartData;
import com.meem.mmp.messages.MMPSingleSmartDataSpec;

import java.util.ArrayList;

/**
 * Created by arun on 17/11/16.
 */

public class GetSessionlessSmartData extends MMPHandler {
    ArrayList<MMPSingleSmartDataSpec> mSpecList;

    int mIndex;

    public GetSessionlessSmartData(ArrayList<MMPSingleSmartDataSpec> specList, ResponseCallback responseCallback) {
        super("GetSessionlessSmartData", MMPConstants.MMP_CODE_APP_GET_SESSIONLESS_SMARTDATA, responseCallback);
        mSpecList = specList;
    }


    private boolean sendCommand() {
        MMPSingleSmartDataSpec spec = mSpecList.get(mIndex);
        MMPGetSingleSmartData singleSmartData = new MMPGetSingleSmartData(spec);
        return singleSmartData.send();
    }

    @Override
    protected boolean kickStart() {
        if (0 == mSpecList.size()) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessSmartData: Invoked with empty list.");
            postResult(true, 0, null, null);
            return false;
        }

        return sendCommand();
    }

    @Override
    public boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_SINGLE_SMART_DATA) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessSmartData object got an unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        if (msg.isError()) {
            mUiCtxt.log(UiContext.ERROR, "GetSessionlessSmartData got error response. Continuing.");
        }

        mIndex++;

        if (mIndex == mSpecList.size()) {
            // All done. Post success event.
            postResult(true, 0, null, null);
            return false;
        } else {
            return sendCommand();
        }
    }

    @Override
    public boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetSessionlessSmartData TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
