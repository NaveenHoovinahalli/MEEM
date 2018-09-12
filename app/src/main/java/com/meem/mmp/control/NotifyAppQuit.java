package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPAppQuit;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;

/**
 * This is special. The problem is, during a forced app kill, the usb reader must wake up and exit from the pending usb read. Due to Android
 * driver bug, the read never gets an exception if we close the usb input stream. So, a command must be sent to cable during onDestroy() of
 * main activity and hope (pray!) that a response will come from cable that will wake up the reader thread and whole app can terminate
 * cleanly. So, here, no wait for response is done. Just send the command and quit the handler.
 *
 * @author Arun T A
 */

public class NotifyAppQuit extends MMPHandler {
    public NotifyAppQuit(String name, ResponseCallback responseCallback) {
        super(name, MMPConstants.MMP_CODE_APP_KILL, responseCallback);

        if (responseCallback != null) {
            throw new IllegalArgumentException("NotifyAppQuit should not pass response callback (must pass null). " + "See comments in NotifyAppQuit.java / contact arun@meeemmemory.com");
        }
    }

    @Override
    protected boolean kickStart() {
        MMPAppQuit quitMsg = new MMPAppQuit();
        quitMsg.send();

        // we do not want a response for this here.
        // remember, this whole arrangement is a hack to get the usb reader
        // thread get out of that blocking read.
        return false;
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_APP_KILL) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "NotifyAppQuit object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        mUiCtxt.log(UiContext.DEBUG, "Response for NotifyAppQuit received. Ignoring.");

        return false;
    }

    @Override
    protected boolean onMMPTimeout() {
        return false;
    }
}
