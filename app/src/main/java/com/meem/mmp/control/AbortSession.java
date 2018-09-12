package com.meem.mmp.control;

import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPAbortSession;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;

/**
 * Abort handling is done using a minor hack: there wont be any waiting for its ack or response (because FW wont be able to send response
 * during a session. This is a problem in FW implementation). It will be handled by the ExecuteSession handler under progress. Right way to
 * implement requires multiple sequence handling with a bit more advanced event processing. Postponed to future releases.
 * <p/>
 * Note that for this to work, once a session is in progress, UI shall not allow user to send any commands other than abort.
 *
 * @author Arun T A
 */

public class AbortSession extends MMPHandler {
    private byte mHandle;

    public AbortSession(byte handle, ResponseCallback responseCallback) {
        super("AbortSession", MMPConstants.MMP_CODE_ABORT_SESSION, responseCallback);

        if (null != responseCallback) {
            throw new IllegalArgumentException("AbortSession object should NOT use response callback (must pass null)." + "This is a FW related protocol constraint. See comments in AbortSession.java / contact arun@meemmemory.com");
        }

        mHandle = handle;
    }

    @Override
    protected boolean kickStart() {
        MMPAbortSession abortCmd = new MMPAbortSession(mHandle);
        return abortCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        return false;
    }

}
