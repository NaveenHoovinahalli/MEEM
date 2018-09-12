package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

import static com.meem.v2.mmp.MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM;

/**
 * Created by arun on 29/6/17.
 */

public class CableModeSwitchV2 extends MMPV2CtrlMsg {
    private byte mNewMode;

    public CableModeSwitchV2(byte newMode, ResponseCallback responseCallback) {
        super("CableModeSwitchV2", MMPV2Constants.MMP_CODE_CHANGE_MODE, responseCallback);
        mNewMode = newMode;
    }

    @Override
    protected boolean kickStart() {
		if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg;

        if(mNewMode == MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM) {
            msg = mmpv2.getPCMeemModeMsg();
        } else if(mNewMode == MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS) {
            msg = mmpv2.getPCBypassModeMsg();
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mNewMode);
        }

        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }
}
