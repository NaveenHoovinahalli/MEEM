package com.meem.mmp.control;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPCloseSession;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCreateSession;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPGetCopyDATD;
import com.meem.mmp.messages.MMPGetDATD;
import com.meem.mmp.messages.MMPUmid;
import com.meem.mmp.messages.MMPUpid;

/**
 * Gets the data descriptor for backup and restore sessions. This design style is a bit ugly. This happened because of the overloaded
 * constructors of MMPCreateSession class which provides common constructor for backup and restore sessions. (BTW, it also provides another
 * two constructors for copy and sync).
 * <p/>
 * Ideally I should have created two objects here - one for backup and one for restore. TODO Later.
 *
 * @author Arun T A
 */
public class GetDataDescriptor extends MMPHandler {
    byte mHandle;
    int mSessionType;
    MMPUpid mUpid;
    MMPUmid mUmid;

    AppLocalData mAppLocalData = AppLocalData.getInstance();
    MMPFPath mDatdPath;

    boolean mAbortRequested = false;

    public GetDataDescriptor(MMPUpid upid, MMPUmid umid, int sessionType, ResponseCallback responseCallback) {
        super("GetDataDescriptor", MMPConstants.MMP_CODE_APP_GET_DATA_DESCRIPTOR, responseCallback);

        mUpid = upid;
        mUmid = umid;
        mSessionType = sessionType;
        mDatdPath = new MMPFPath(mAppLocalData.getDatdPath(), 0);
    }

    @Override
    protected boolean kickStart() {
        if (mSessionType == MMPConstants.MMP_CODE_CREATE_CPY_SESSION) {
            MMPCreateSession createCopySession = new MMPCreateSession(mUpid, mUmid);
            return createCopySession.send();
        } else {
            MMPCreateSession createBackupSession = new MMPCreateSession(mUmid, mSessionType);
            return createBackupSession.send();
        }
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        boolean result = true;

        if (msg.isAck()) {
            // wait further
            return true;
        }

        // take actions
        switch (msg.getMessageCode()) {
            // TODO: Tidy this up by adding another switch case to handle all
            // session types.
            case MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION:
            case MMPConstants.MMP_CODE_CREATE_RESTORE_SESSION:
            case MMPConstants.MMP_CODE_CREATE_CPY_SESSION:
                if (msg.isSuccess()) {
                    MMPCreateSession createSessResp = new MMPCreateSession(msg);
                    mHandle = createSessResp.getSessionHandle();
                    // get data descriptor
                    result = getDataDescriptor();
                } else if (msg.isError()) {
                    postResult(false, msg.getErrorCode(), Byte.valueOf(mHandle), null);
                    return false;
                }
                break;

            case MMPConstants.MMP_CODE_GET_DATD:
            case MMPConstants.MMP_CODE_GET_CPY_DATD:

                if (msg.isSuccess()) {
                    postResult(true, 0, Byte.valueOf(mHandle), null);
                } else if (msg.isError()) {
                    // Arun: 30Dec2015: Bugfix: see below
                    if (!mAbortRequested) {
                        closeSession();
                    }

                    postResult(false, msg.getErrorCode(), Byte.valueOf(mHandle), null);
                }

                return false;

            // 09Sept2015:  see comment at the bottom.
            case MMPConstants.MMP_CODE_CLOSE_SESSION:
                if (msg.isSuccess()) {
                    mUiCtxt.log(UiContext.DEBUG, "GetDataDescriptor got close session success response.");
                    postResult(false, msg.getErrorCode(), Byte.valueOf(mHandle), null);
                    return false;
                } else {
                    mUiCtxt.log(UiContext.ERROR, "GetDataDescriptor got close session failure response!");
                    return true;
                }

            default:
                mUiCtxt.log(UiContext.ERROR, "GetDataDescriptor object got unknown MMP message");
                msg.dbgDumpBuffer();
                result = true;
        }

        return result;
    }

    private boolean getDataDescriptor() {
        if (mSessionType == MMPConstants.MMP_CODE_CREATE_CPY_SESSION) {
            MMPGetCopyDATD getCopyDATD = new MMPGetCopyDATD(mHandle, mDatdPath);
            return getCopyDATD.send();
        } else {
            MMPGetDATD getDatd = new MMPGetDATD(mHandle, mDatdPath);
            return getDatd.send();
        }
    }

    @Override
    protected boolean onMMPTimeout() {
        /**
         * Change note: 28Oct2015: FW will take time to prepare DATD if there
         * are thousands of files in cable.
         */
        mUiCtxt.log(UiContext.DEBUG, "GetDataDescriptor TIMEOUT (ignored)");
        return true;
    }

    @Override
    // 09Sept2015: Arun: AbortFix for FW dsugin lengthy DATD preperation.
    protected boolean onAbortNotification() {
        mUiCtxt.log(UiContext.DEBUG, "GetDataDescriptor: New abort implementation: Forcing close during DATD preperation.");
        // Arun: 30Dec2015: Bugfix: see below
        mAbortRequested = true;
        return closeSession();
    }

    /**
     * Arun: 30Dec2015: Bugfix: Close session is possible only from here as UI components (especially Session will get the handle only on
     * successful return from this object.
     */
    private boolean closeSession() {
        MMPCloseSession closeSession = new MMPCloseSession(mHandle);
        return closeSession.send();
    }
}
