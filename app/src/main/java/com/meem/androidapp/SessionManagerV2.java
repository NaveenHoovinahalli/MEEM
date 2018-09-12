package com.meem.androidapp;

import com.meem.businesslogic.Session;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.phone.Storage;
import com.meem.utils.DebugTracer;
import com.meem.v2.businesslogic.BackupV2;
import com.meem.v2.businesslogic.RestoreV2;
import com.meem.v2.businesslogic.SessionV2;
import com.meem.v2.cablemodel.SecureDb;
import com.meem.v2.cablemodel.SecureDbScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Important: Session manager class uses BAckupV2 for backup logic and implements whiole retsore logic (for generic data) in here itself.
 * MMP V2 has made restore logic so simple!
 * <p>
 * Created by arun on 25/5/17.
 */

public class SessionManagerV2 extends SessionManager implements BackupV2.BackupV2Listener, SecureDbScanner {
    private static final String TAG = "SessionManagerV2";
    protected DebugTracer mDbg = new DebugTracer(TAG, "SessionManagerV2.log");

    SessionV2 mSessionV2;
    Thread mWorkerThread;

    Storage mStorage;

    BackupV2 mBackupV2;
    RestoreV2 mRestoreV2;

    int mItemCount, mTotalItems;

    public SessionManagerV2(String phoneUpid, SessionManagementHelper helper) {
        super(phoneUpid, helper);
    }

    @Override
    protected Session createSession(int sessionType, Session.SessionStatUpdateHelper statHelper) {
        mDbg.trace();

        mStorage = new Storage();
        mSessionV2 = new SessionV2(sessionType, statHelper);

        return mSessionV2;
    }

    @Override
    protected boolean startBackupSessionAfterInitialCableSpaceCheck() {
        mDbg.trace();

        mSessionV2.prepareGenericDataInfoForBackup();
        mSessionV2.prepareSmartDataInfo();

        mBackupV2 = new BackupV2(mSessionV2, this);
        mBackupV2.start();

        // we will continue from onSessionPreperationSucceeded
        return true;
    }

    @Override
    protected boolean startRestoreOrCopySessionAfterStorageOptions(String upid) {
        mDbg.trace();

        // TODO: Handle plus items for restore (not required as per old product plan).

        // Arun: 20June2017: Already done in checkPhoneStorageAvailabilityForSession in the parent class.
        // Doing this again here will nullify the effect of category filter.
        /*mSessionV2.prepareGenericDataInfoForRestoreOrCopy(mSessionV2.getVaultInfo().mGenMirrorCatMask, 0);*/
        mSessionV2.prepareSmartDataInfo();

        // if sms category is involved in the session, we must ask the user permission
        if (mSessionV2.getSmartDataInfo().getCatCodes().contains(MMPConstants.MMP_CATCODE_MESSAGE)) {
            mDbg.trace("Going for user permission for messages");
            mHelper.requestSmsManagementPermission();
        } else {
            accessForSMSManagement(false);
        }

        return true;
    }

    public void accessForSMSManagement(boolean granted) {
        mDbg.trace();

        if (!granted) {
            // remove messages from session's cat list
            mDbg.trace("Permission denied to access sms. We wont do sms restore.");
            mSessionV2.getSmartDataInfo().getCatCodes().remove(Byte.valueOf(MMPConstants.MMP_CATCODE_MESSAGE));
            mSessionV2.getSmartDataInfo().getCatsFilesMap().remove(MMPConstants.MMP_CATCODE_MESSAGE);
        } else {
            mHasMsgMgmtPermission = true;
        }

        mSession.startStats();

        mRestoreV2 = new RestoreV2(mSessionV2, null);
        mRestoreV2.start();
    }

    /**
     * This will be called by UI thread when it gets the preparation completion event from backup/restore logic threads.
     *
     * @return
     */
    @Override
    protected boolean onSessionPreperationSucceeded() {
        mDbg.trace();

        if (mHasMsgMgmtPermission) {
            mHasMsgMgmtPermission = false;
            mHelper.dropSmsManagementPermission();
        }

        if (!checkForStorageAfterSessionPreperation()) {
            return false;
        }

        if (mSessionType == SessionType.BACKUP) {
            // send the securedb to cable
            mDbg.trace("Sending securedb to cable");
            return mDriverInstance.executeSession((byte) 0, null, null, false, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mDbg.trace("Securedb sent, result: " + result);

                    MeemEvent evt = new MeemEvent(EventCode.SESSION_XFR_STARTED);
                    UiContext.getInstance().postEvent(evt);

                    mWorkerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            doBackupXfr();
                        }
                    });

                    mDbg.trace("Starting backup scan+xfr thread...");
                    mWorkerThread.start();

                    return result;
                }
            });
        } else {

            MeemEvent evt = new MeemEvent(EventCode.SESSION_XFR_STARTED);
            UiContext.getInstance().postEvent(evt);

            // no need to send securedb for restore.
            mWorkerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    doRestoreXfr();
                }
            });

            mDbg.trace("Starting restore scan+xfr thread...");
            mWorkerThread.start();

            return true;
        }
    }

    @Override
    protected boolean abort() {
        mDbg.trace();

        boolean result = false;

        if (null == mSessionV2) {
            mDbg.trace("SessionV2 is null during abort!");
            return result;
        }

        if (mSessionV2.isAbortPending()) {
            mDbg.trace("Note: sessionV2 is already marked for aborting.");
        }

        mSessionV2.setAbortPending(true); //  This will be checked by XFR thread below

        if (null != mBackupV2) mBackupV2.abort();
        if (null != mRestoreV2) mRestoreV2.abort();

        mDriverInstance.abortSession();

        return result;
    }

    // ----------------------------------------------------------------------------
    // -------------------------------- XFR stuff ---------------------------------
    // ----------------------------------------------------------------------------
    @Override
    public Boolean onSecureDbItemWhileScanning(MMLGenericDataDesc desc) {
        mDbg.trace();

        postSessionCommentary(desc);

        String upid = mSessionV2.getVaultInfo().getUpid();

        if (mSessionType == SessionType.BACKUP) {
            mDbg.trace("Backing up item: " + desc);
            mDriverInstance.sendGenericData(upid, desc, null);
        } else {
            String path;

            if (desc.onSdCard) {
                path = mStorage.toPrimaryExtAbsPath(desc.mPath);
            } else {
                path = mStorage.toSecExtAbsPath(desc.mPath);
            }

            // Note: this is not fully correct (we have to consider checksum to be 100% sure - but for a product
            // linke MEEM, this is enough unless and until someone specifically asks for it.
            File file = new File(path);
            boolean fileNotFound = !file.exists();
            boolean sizeMisMatch = (file.length() != desc.mSize);
            if (fileNotFound || sizeMisMatch) {
                mDbg.trace("Fetching item: " + desc + " {fnf = " + fileNotFound + ", currSize = " + file.length() + "}");
                mDriverInstance.fetchGenericData(upid, desc, null);
            }
        }

        if (mSessionV2.isAbortPending()) {
            return null;
        } else {
            return false;
        }
    }

    @Override
    public void onSecureDbScanningComplete(byte cat) {
        // unused
    }

    @Override
    public void onTotalItemCountForScannedCat(byte cat, int itemCount) {
        mDbg.trace("We have total: " + itemCount + "for cat: " + cat);
        mTotalItems = itemCount;
    }

    // ----------------------------------------------------------------------------
    // -------------------------------- XFR stuff ---------------------------------
    // ----------------------------------------------------------------------------

    /**
     * Thread function.
     *
     * @return
     */
    private boolean doBackupXfr() {
        mDbg.trace();
        boolean result = true;

        result &= doSmartDataBackupXfr();
        result &= doGenDataBackupXfr();

        notifySessionResult(result);

        return result;
    }

    private boolean doSmartDataBackupXfr() {
        mDbg.trace();
        boolean result = true;

        String upid = mSessionV2.getVaultInfo().getUpid();
        HashMap<Byte, MMLSmartDataDesc> sddMap = mSessionV2.getSmartDataInfo().mDbFilesMap;

        for (Byte cat : sddMap.keySet()) {
            MMLSmartDataDesc desc = sddMap.get(cat);

            mDbg.trace("Sending smartdata item: " + desc);
            result &= mDriverInstance.sendSmartData(upid, desc, null);
        }

        return result;
    }

    private boolean doGenDataBackupXfr() {
        mDbg.trace();
        boolean result = true;

        String upid = mSessionV2.getVaultInfo().getUpid();

        int genCatCode = mSessionV2.getGenericDataInfo().getGenCatMask();
        ArrayList<Byte> cats = MMLCategory.getGenericCatCodesArray(genCatCode);

        mDbg.trace("Cleaning up cable before xfr");
        mDriverInstance.cleanupCable(upid, cats);

        mDbg.trace("Starting securedb scanning for backup");
        SecureDb secDb = new SecureDb(upid);

        for (Byte cat : cats) {
            mDbg.trace("Securedb scan starts for cat: " + MMLCategory.toGenericCatString(cat));
            result &= secDb.scan(cat, this, false);

            if (mSessionV2.isAbortPending()) {
                mDbg.trace("Aborting on request");
                break;
            }

            postStatusInfo(cat, false);
            mItemCount = 0;
            mTotalItems = 0;
        }

        return result;
    }

    private void postStatusInfo(byte cat, boolean start) {
        MMPSessionStatusInfo.Type type = start ? MMPSessionStatusInfo.Type.STARTED : MMPSessionStatusInfo.Type.ENDED;
        MMPSessionStatusInfo statusInfo = new MMPSessionStatusInfo(cat, type);

        MeemEvent evt = new MeemEvent(EventCode.SESSION_PROGRESS_UPDATE);
        evt.setInfo(statusInfo);
        UiContext.getInstance().postEvent(evt);
    }

    /**
     * Thread function.
     * Note: No XFR is to be done for smart data on restore.
     *
     * @return
     */
    private boolean doRestoreXfr() {
        mDbg.trace();
        boolean result = true;

        String upid = mSessionV2.getVaultInfo().getUpid();
        int genCatCode = mSessionV2.getGenericDataInfo().getGenCatMask();
        ArrayList<Byte> cats = MMLCategory.getGenericCatCodesArray(genCatCode);

        mDbg.trace("Starting securedb scanning for restore");
        SecureDb secDb = new SecureDb(upid);

        for (Byte cat : cats) {
            result &= secDb.scan(cat, this, true);

            if (mSessionV2.isAbortPending()) {
                mDbg.trace("Aborting on request");
                break;
            }

            postStatusInfo(cat, false);
            mItemCount = 0;
            mTotalItems = 0;
        }

        notifySessionResult(result);
        return result;
    }

    private void notifySessionResult(final boolean sessionResult) {
        // Post the result to ui thread. TODO: This is ugly! mixing the ui and non-ui code like this...
        UiContext.getInstance().postEvent(new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, null, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                String msg = "" + mSessionType + " session (v2) ended";

                // legacy call to session manager (us)
                onSessionEnd(sessionResult, msg);

                // Arun: 07July2017: Bugfix: Removed calling Mainactivity's notifySessionResult
                // because it is already called in onSessionEnd just above.
                return true;
            }
        }));
    }

    private void postSessionCommentary(MMLGenericDataDesc desc) {
        mItemCount++;
        SessionCommentary comment = new SessionCommentary(EventCode.SESSION_XFR_COMMENTARY, mItemCount, mTotalItems, desc.mCatCode, SessionCommentary.OPMODE_DONTCARE);
        comment.post();
    }

    // ------------------------------------------------------------
    // Interface implementation for BAckupV2 - mainly to save secureDb
    // to cable whenever a category processing is done.
    // ------------------------------------------------------------

    @Override
    public void saveSecureDb() {
        mDbg.trace();

        if(mDriverInstance != null && mDriverInstance.isCableConnected()) {
            // note: for v2, execute session is just sending securedb to cable.
            mDriverInstance.executeSession((byte) 0, null, null, false, null);
        }
    }
}
