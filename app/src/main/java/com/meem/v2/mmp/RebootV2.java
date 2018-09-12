package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 9/6/17.
 */

public class RebootV2 extends MMPV2CtrlMsg {
    public RebootV2(ResponseCallback responseCallback) {
        super("RebootV2", MMPV2Constants.MMP_CODE_REBOOT_CABLE, responseCallback);
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getRebootMsg();
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }
}
