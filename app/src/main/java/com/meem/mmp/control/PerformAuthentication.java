package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPAuthUsingPID;
import com.meem.mmp.messages.MMPAuthUsingPIN;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;

public class PerformAuthentication extends MMPHandler {
    private int mAuthMethod;
    private String mCred;
    private ResponseCallback mResponseCallback;

    public PerformAuthentication(int authMethod, String credential, ResponseCallback callback) {
        super("PerformAuthentication", MMPConstants.MMP_CODE_APP_PERFORM_AUTHENTICATION, callback);
        if (authMethod != MMPConstants.MMP_CODE_PIN_AUTH && authMethod != MMPConstants.MMP_CODE_PID_AUTH) {
            throw new IllegalArgumentException("Invalid authentication method specified (neither PIN nor PID): " + authMethod);
        }

        mAuthMethod = authMethod;
        mCred = credential;
        mResponseCallback = callback;
    }

    @Override
    protected boolean kickStart() {
        if (mAuthMethod == MMPConstants.MMP_CODE_PIN_AUTH) {
            MMPAuthUsingPIN authPIN = new MMPAuthUsingPIN(mCred);
            return authPIN.send();
        }

        if (mAuthMethod == MMPConstants.MMP_CODE_PID_AUTH) {
            MMPAuthUsingPID authPID = new MMPAuthUsingPID(mCred);
            return authPID.send();
        }
        // should not happen
        return false;
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != mAuthMethod) {
            mUiCtxt.log(UiContext.ERROR, "PerformAuthentication object got unknown message: " + msgCode);
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        MMPAuthUsingPIN resp = new MMPAuthUsingPIN(msg);

        int authResp = 0;
        int wrongTrials = -1;
        if (msg.isError()) {
            authResp = resp.getResult();
            wrongTrials = resp.getWrongTrialsCount();
        }

        mUiCtxt.log(UiContext.DEBUG, "PerformaAthentication response received: " + authResp + ", Wrong trials " + wrongTrials);
        postResult(true, 0, Integer.valueOf(authResp), Integer.valueOf(wrongTrials));

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.ERROR, "PerformaAthentication TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, Integer.valueOf(-1), null);
        return false;
    }
}
