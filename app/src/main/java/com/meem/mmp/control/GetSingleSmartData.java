package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetSingleSmartData;
import com.meem.mmp.messages.MMPSingleSmartDataSpec;

/**
 * Created by arun on 26/8/16.
 * <p/>
 * Note: There is no need to go for a complex constructor arguments such as we did for GetSmartData - because that complexity happened
 * because of old MMP spec which is very complex
 */
public class GetSingleSmartData extends MMPHandler {
    MMPSingleSmartDataSpec mSpec;

    public GetSingleSmartData(MMPSingleSmartDataSpec fileSpec, ResponseCallback responseCallback) {
        super("GetSingleSmartData", MMPConstants.MMP_CODE_GET_SINGLE_SMART_DATA, responseCallback);
        mSpec = fileSpec;
    }

    @Override
    protected boolean kickStart() {
        MMPGetSingleSmartData mGetSingleSmartDataCmd = new MMPGetSingleSmartData(mSpec);
        return mGetSingleSmartDataCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_SINGLE_SMART_DATA) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "GetSingleSmartData object got unknown message");
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
        mUiCtxt.log(UiContext.DEBUG, "GetSingleSmartData TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, mSpec, null);
        return false;
    }
}
