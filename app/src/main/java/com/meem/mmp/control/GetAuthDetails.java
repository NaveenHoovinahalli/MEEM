package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetAuthMethod;

public class GetAuthDetails extends MMPHandler {

    public GetAuthDetails(ResponseCallback callback) {
        super("GetAuthDetails", MMPConstants.MMP_CODE_GET_AUTH_DETAILS, callback);
    }

    @Override
    protected boolean kickStart() {
        MMPGetAuthMethod getAuthMethod = new MMPGetAuthMethod();
        return getAuthMethod.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_AUTH_DETAILS) {
            mUiCtxt.log(UiContext.ERROR, "GetAuthDetails object got unknown message");
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        MMPGetAuthMethod resp = new MMPGetAuthMethod(msg);
        int authMethod = resp.getAuthMethod();

        mUiCtxt.log(UiContext.DEBUG, "GetAuthDetails response received: " + authMethod);

        /**
         * Note: We do not care about the response being success or failed.
         */
        postResult(true, msg.getErrorCode(), Integer.valueOf(authMethod), null);

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetAuthDetails TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
