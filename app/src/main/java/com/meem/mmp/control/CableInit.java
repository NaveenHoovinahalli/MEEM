package com.meem.mmp.control;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPCableAuth;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPGetAuthMethod;
import com.meem.mmp.messages.MMPGetRandomSeed;
import com.meem.mmp.messages.MMPGetSerialNumber;
import com.meem.mmp.messages.MMPGetTime;

/**
 * @author Arun T A
 */

public class CableInit extends MMPHandler {
    private boolean mGenuineCable = false;
    private String mSerial;

    private ResponseCallback mResponseCallback;

    public CableInit(ResponseCallback responseCallback) {
        super("CableInit", MMPConstants.MMP_CODE_APP_CABLE_INIT, responseCallback);

        if (!ProductSpecs.ENABLE_GENUINE_CABLE_CHECK) {
            mGenuineCable = true;
        }
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        switch (msg.getMessageCode()) {
            case MMPConstants.MMP_CODE_AUTH_CABLE:
                if (msg.isAck()) {
                    break;
                }

                MMPCableAuth cableAuth = new MMPCableAuth(msg);
                if (cableAuth.isSuccess()) {
                    mUiCtxt.log(UiContext.DEBUG, "MMP_CODE_AUTH_CABLE: Succeeded");
                    mGenuineCable = true;
                } else {
                    mUiCtxt.log(UiContext.DEBUG, "MMP_CODE_AUTH_CABLE: Failed!");
                    mGenuineCable = false;
                }

                break;

            case MMPConstants.MMP_INTERNAL_HACK_BYPASS_CABLE_AUTH:
                mUiCtxt.log(UiContext.DEBUG, "Bypassing authentication, directly going to get serial number");

                // proceed to get serial number
                return getSerialNumber();

            case MMPConstants.MMP_CODE_GET_TIME:
                mUiCtxt.log(UiContext.DEBUG, "GET_TIME command");

                if (!mGenuineCable) {
                    mUiCtxt.log(UiContext.DEBUG, "Non-authenticated cable. Rejecting command.");
                    msg.sendNack();
                    return false;
                }

                msg.sendAck();
                return sendGetTimeResponse(msg);

            case MMPConstants.MMP_CODE_GET_RANDOM_SEED:
                mUiCtxt.log(UiContext.DEBUG, "GET_RANDOM_SEED command");
                msg.sendAck();

                MMPGetRandomSeed randSeed = new MMPGetRandomSeed(msg);
                randSeed.prepareResponse();
                randSeed.send();

                // proceed to get serial number
                return getSerialNumber();

            case MMPConstants.MMP_CODE_GET_SERIAL_NUMBER:
                if (msg.isAck()) {
                    break;
                }

                MMPGetSerialNumber getSerial = new MMPGetSerialNumber(msg);
                String dbgMsg = "GET_SERIAL_NUMBER response: " + (getSerial.isSuccess() ? "Success" : "Error");
                mUiCtxt.log(UiContext.DEBUG, dbgMsg);

                // MMPcommand will return some dummy value on error.
                mSerial = getSerial.getSerialNumber();

                // get authentication method etc
                return getAuthDetails();

            case MMPConstants.MMP_CODE_GET_AUTH_DETAILS:
                if (msg.isAck()) {
                    break;
                }

                mUiCtxt.log(UiContext.DEBUG, "GET_AUTH_DETAILS response received. CableInit completed.");

                MMPGetAuthMethod resp = new MMPGetAuthMethod(msg);
                int authMethod = resp.getAuthMethod();

                postResult(true, 0, Integer.valueOf(authMethod), mSerial);
                return false;

            default:
                mUiCtxt.log(UiContext.ERROR, "CableInit object got unknown MMP message");
                msg.dbgDumpBuffer();
                return true;
        }

        return true;
    }

    @Override
    protected boolean kickStart() {
        // Initial sequence will always wait for GET_TIME
        // command. So we don't have to do anything here.
        return true;
    }

    @Override
    protected boolean onMMPTimeout() {
        notifyInitFailure("TIMEOUT");
        return false;
    }

    private boolean sendGetTimeResponse(MMPCtrlMsg msg) {
        boolean res;
        MMPGetTime getTime = new MMPGetTime(msg);
        getTime.prapareResponse();
        res = getTime.send();
        if (!res) {
            notifyInitFailure("GetTime response could not be sent!");
        }
        return res;
    }

    private boolean getAuthDetails() {
        boolean res;
        MMPGetAuthMethod getAuthMethod = new MMPGetAuthMethod();
        res = getAuthMethod.send();
        if (!res) {
            notifyInitFailure("GetAuthMethod could not be sent!");
        }
        return res;
    }

    private boolean getSerialNumber() {
        boolean res;
        MMPGetSerialNumber getSerial = new MMPGetSerialNumber();
        res = getSerial.send();
        if (!res) {
            notifyInitFailure("GetSerialNumber could not be sent!");
        }
        return res;
    }

    private void notifyInitFailure(String msg) {
        mUiCtxt.log(UiContext.ERROR, "CableInit failed: " + msg);
        postResult(false, -1, Integer.valueOf(-1), (mSerial == null ? "Error" : mSerial));
    }
}