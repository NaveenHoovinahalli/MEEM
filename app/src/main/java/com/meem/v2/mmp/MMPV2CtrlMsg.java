package com.meem.v2.mmp;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;
import com.meem.v2.core.MeemCoreV2Request;

/**
 * Created by arun on 20/4/17.
 */

public class MMPV2CtrlMsg extends MeemCoreV2Request {
    public MMPV2CtrlMsg(String name, int cmdCode, ResponseCallback responseCallback) {
        super(name, cmdCode, responseCallback);
    }

    @Override
    protected boolean kickStart() {
        return super.kickStart();
    }

    @Override
    protected boolean onMMPMessage(MMPV2 msg) {
        super.onMMPMessage(msg);

        int cmdCode = msg.getMsgCode();
        boolean result = msg.getResult();

        if (mCmdCode != cmdCode) {
            postLog(UiContext.ERROR, "Invalid command code: " + cmdCode + " Expected: " + mCmdCode + ", " + mName);
        } else {
            postLog(UiContext.DEBUG, "Got response for: " + mName + ", " + cmdCode + " result: " + result);
        }

        postResult(result, 0, null, null);

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        super.onMMPTimeout();
        return false;
    }
}
