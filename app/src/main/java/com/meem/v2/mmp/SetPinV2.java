package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * Created by arun on 10/5/17.
 */

public class SetPinV2 extends MMPV2CtrlMsg {
    private String mPin;
    LinkedHashMap<Integer, String> mRecoveryAnswers;

    public SetPinV2(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        super("SetPinV2", MMPV2Constants.MMP_CODE_SET_PIN, responseCallback);
        mPin = pin;
        mRecoveryAnswers = recoveryAnswers;
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getSetPINMsg(mPin, mRecoveryAnswers);
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }
}
