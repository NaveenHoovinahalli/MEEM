package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPExecuteSession;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSendThumbnailDb;
import com.meem.mmp.messages.MMPSessionStatus;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.messages.MMPSetCopySESD;
import com.meem.mmp.messages.MMPSetSESD;

/**
 * Class that implement a backup/restore/copy session.
 * <p/>
 * See comments in code about differences between current version of MMP specification and firmware implementation.
 * <p/>
 * 21May2015: Removed abort session control command from MMP and changes in here reflects it.
 *
 * @author Arun T A
 */
public class ExecuteSession extends MMPHandler {
    byte mHandle;
    boolean mIsCopy;
    MMPFPath mFPathSesd;
    MMPFPath mFPathThumbDb;

    boolean mAbortPending;

    public ExecuteSession(byte handle, MMPFPath fpathSesd, MMPFPath fpathThumbDb, boolean isCopy, ResponseCallback responseCallback) {
        super("ExecuteSession", MMPConstants.MMP_CODE_APP_EXECUTE_SESSION, responseCallback);

        mHandle = handle;
        mIsCopy = isCopy;
        mFPathSesd = fpathSesd;
        mFPathThumbDb = fpathThumbDb;
        mAbortPending = false;
    }

    @Override
    protected boolean kickStart() {
        if (mIsCopy) {
            MMPSetCopySESD setCopySesd = new MMPSetCopySESD(mHandle, mFPathSesd);
            return setCopySesd.send();
        } else {
            MMPSetSESD setSesd = new MMPSetSESD(mHandle, mFPathSesd);
            return setSesd.send();
        }
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        MeemEvent evt;
        if (msg.isAck()) {
            return true; // to wait further
        }

        if (msg.isError()) {
            int msgCode = msg.getMessageCode();

            if (mAbortPending) {
                mUiCtxt.log(UiContext.INFO, "Abort was requested, got session failed response. Fine. Quitting");
            } else {
                mUiCtxt.log(UiContext.INFO, "Execute session failed! Command failed was: " + msgCode);
            }

            // Post event. We are done.
            postResult(false, msg.getErrorCode(), null, null);
            return false;
        }

        // Note that MMP specification is not updated for latest firmware
        // implementation. As of now, there are no SAVE_SESSION or
        // SESSION_COMPLETE etc.
        switch (msg.getMessageCode()) {
            case MMPConstants.MMP_CODE_SET_SESD:
            case MMPConstants.MMP_CODE_SET_CPY_SESD:
                return sendThumbnailDb();

            case MMPConstants.MMP_CODE_SEND_THUMBNAIL_DB:
                return execSession();

            case MMPConstants.MMP_CODE_EXECUTE_SESSION:
                if (mAbortPending) {
                    mUiCtxt.log(UiContext.INFO, "Abort was requested, but session succeeded! Well...");
                }

                // Post event. We are done.
                postResult(true, 0, null, null);
                return false;

            case MMPConstants.MMP_CODE_SESSION_STATUS:
                // NOTE: 27June2014: Due to FW issue with ack for this, commenting
                // acknowledgment.
                // msg.sendAck();
                mUiCtxt.log(UiContext.DEBUG, "Session status update received.");

                MMPSessionStatus status = new MMPSessionStatus(msg);
                MMPSessionStatusInfo info = status.getInfo();

                evt = new MeemEvent(EventCode.SESSION_PROGRESS_UPDATE);
                evt.setInfo(info);
                mUiCtxt.postEvent(evt);
                return true;

            default:
                mUiCtxt.log(UiContext.ERROR, "ExecuteSession object got unknown message:");
                msg.dbgDumpBuffer();
                return true;
        }
    }

    private boolean sendThumbnailDb() {
        boolean result;

        MMPSendThumbnailDb sendThumbDb = new MMPSendThumbnailDb(mFPathThumbDb);
        result = sendThumbDb.send();
        return result;
    }

    private boolean execSession() {
        boolean result;
        MMPExecuteSession execSess = new MMPExecuteSession(mHandle);
        result = execSess.send();

        if (result) {
            MeemEvent evt = new MeemEvent(EventCode.SESSION_XFR_STARTED);
            mUiCtxt.postEvent(evt);
        }

        return result;
    }

    @Override
    protected boolean onMMPTimeout() {
        return true;
    }

    // Arun: AbortFix 21May2015
    // Completely removed the timer stuff for abort handling.
    @Override
    protected boolean onAbortNotification() {
        mUiCtxt.log(UiContext.DEBUG, "ExecuteSession: New abort implementation in action!");
        mAbortPending = true;

        MeemEvent evt = new MeemEvent(EventCode.SESSION_ABORT_BACKEND_ACK);
        mUiCtxt.postEvent(evt);

        return true;
    }

    // Arun: Abortfix 22May2015
    // See comments in MMPHandler and MeemCoreHandler.
    @Override
    protected boolean onXfrRequest() {
        return !mAbortPending;
    }
}
