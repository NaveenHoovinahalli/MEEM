package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

/**
 * @author Arun T A
 */

public class MMPGetPassword extends MMPCtrlMsg {
    private String tag = "MMPGetPassword";

    private String mSetterUpid;

    // For creating command
    public MMPGetPassword() {
        super(MMPConstants.MMP_CODE_GET_PASSWD);
    }

    // For parsing response message
    public MMPGetPassword(MMPCtrlMsg copy) {
        super(copy);
    }

    /**
     * Get the MEEM password from GET_PASSWORD response.
     *
     * @return If the password is set, returns that password in clear text. Returns null otherwise or on any other error.
     */
    public String getMeemPassword() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {

            int hlen = getHeaderLength();
            int tlen = getMessageLength();
            int plen = tlen - hlen - 2;

            // TODO: This is a hack for 4 byte password limitation,
            // as supported & restricted by main stream MEEM GUI.
            // This is to be fixed by a firmware update where header
            // is not properly updated to reflect the message length
            // by device manager module for this command.
            plen = 4;

            byte[] baPasswd = new byte[plen];
            getByteArrayParam(baPasswd);
            String strPasswd = new String(baPasswd);

            int upidLen = getByteParam();
            dbgTrace("upid length: " + upidLen);
            byte[] baUpid = new byte[upidLen];
            getByteArrayParam(baUpid);
            mSetterUpid = new String(baUpid);
            dbgTrace("Password was set by: " + mSetterUpid);

            return strPasswd;
        } else {
            // ACK management is done by MMP transport layer in MMPHandler
            // object. We don't care about ACK/NACK in MMP objects.
            dbgTrace("Meem password is not set");
            return null;
        }
    }

    public String getPasswordSetter() {
        return mSetterUpid;
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMPGetPassword.log", trace);
    }

    // debug support
    @SuppressWarnings("unused")
    private void dbgTrace() {
        GenUtils.logMethodToFile("MMPGetPassword.log");
    }
}
