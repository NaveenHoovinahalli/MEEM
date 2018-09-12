package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

public class PinRecoveryV2 extends MMPV2CtrlMsg {
    LinkedHashMap<Integer, String> mRecoveryAnswers;

    public PinRecoveryV2(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        super("PinRecoveryV2", MMPV2Constants.MMP_CODE_PIN_AUTH, responseCallback);
        mRecoveryAnswers = recoveryAnswers;
    }

    @Override
    protected boolean kickStart() {
        if (!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getPinRecoveryMsg(mRecoveryAnswers);
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }

    @Override
    protected boolean onMMPMessage(MMPV2 msg) {
        boolean result = msg.getResult();

        int errCode = 0;
        byte attemptsSoFar = 0;

        // TODO: Confirm this with Barath.
        if (!result) {
            ByteBuffer resp = msg.getMessageBuffer(); // the position is right after message code in header.
            errCode = resp.getInt();
            attemptsSoFar = resp.get();
        }

        postResult(result, errCode, Integer.valueOf(errCode), Byte.valueOf(attemptsSoFar));

        return false;
    }
}