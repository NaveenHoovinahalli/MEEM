package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 24/5/17.
 */

public class SendAppQuitV2 extends MMPV2CtrlMsg {
    public SendAppQuitV2(ResponseCallback responseCallback) {
        super("SendAppQuitV2", MMPV2Constants.MMP_CODE_APP_QUIT, responseCallback);
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getAppQuitMsg();
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }
}
