package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 24/5/17.
 */

public class DeleteVaultV2 extends MMPV2CtrlMsg {
    String mUpid;

    public DeleteVaultV2(String upid, ResponseCallback responseCallback) {
        super("DeleteVaultV2", MMPV2Constants.MMP_CODE_DELETE, responseCallback);
        mUpid = upid;
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getDeleteVaultMsg(mUpid);
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }

    // Arun: bugfix: 04July2018: This command will take arbitrary time in fw to process.
    // So, we must ignore timeout by returning true.
    @Override
    protected boolean onMMPTimeout() {
        super.onMMPTimeout();
        return true; // Important.
    }
}
