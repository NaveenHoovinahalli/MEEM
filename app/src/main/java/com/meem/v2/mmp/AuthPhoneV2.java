package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 10/5/17.
 */

public class AuthPhoneV2 extends MMPV2CtrlMsg {
    String mPin;

    public AuthPhoneV2(String pin, ResponseCallback responseCallback) {
        super("AuthPhoneV2", MMPV2Constants.MMP_CODE_PIN_AUTH, responseCallback);
        mPin = pin;
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        MMPV2 mmpv2 = new MMPV2(MeemCoreV2.getInstance().getPayloadSize());
        ByteBuffer msg = mmpv2.getAuthPhoneMsg(mPin);
        return MeemCoreV2.getInstance().sendUsbMessage(msg, mName);
    }

    @Override
    protected boolean onMMPMessage(MMPV2 msg) {
        boolean result = msg.getResult();

        int errCode = 0;
        byte attemptsSoFar = 0;

        if (!result) {
            ByteBuffer resp = msg.getMessageBuffer(); // the position is right after message code in header.
            errCode = resp.getInt();
            attemptsSoFar = resp.get();
        }

        postResult(result, errCode, Integer.valueOf(errCode), Byte.valueOf(attemptsSoFar));

        return false;
    }
}
