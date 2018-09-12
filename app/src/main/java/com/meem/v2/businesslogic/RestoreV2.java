package com.meem.v2.businesslogic;

import android.content.Context;

import com.meem.androidapp.UiContext;
import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.phone.Storage;
import com.meem.utils.GenUtils;
import com.meem.v2.cablemodel.SecureDb;
import com.meem.v2.cablemodel.SecureDbScanner;
import com.meem.v2.phone.CalendersV2;
import com.meem.v2.phone.ContactsV2;
import com.meem.v2.phone.MessagesV2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import meem.org.apache.commons.lang3.mutable.MutableBoolean;

import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_CALENDER;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_CONTACT;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_MESSAGE;

/**
 * Created by arun on 15/4/17.
 * Takes care of the simple restore logic of V2 product.
 */

public class RestoreV2 implements SecureDbScanner {
    UiContext mUiCtxt = UiContext.getInstance();
    private MutableBoolean mAbortFlag = new MutableBoolean(false);
    private ResponseCallback mResponseCallback;
    private SecureDb mSecureDb;
    private Storage mStorage;
    private SessionV2 mSessionV2;
    private Thread mWorkerThread;

    private ContactsV2 contactsV2;
    private MessagesV2 messagesV2;
    private CalendersV2 calendarsV2;

    private long mSessionIntMemSize = 0, mSessionExtMemSize = 0;

    public RestoreV2(SessionV2 sessionV2, ResponseCallback responseCallback) {
        dbgTrace();

        mSessionV2 = sessionV2;
        mResponseCallback = responseCallback;
    }

    public void start() {
        dbgTrace();

        mStorage = new Storage();

        mSecureDb = new SecureDb(mSessionV2.getVaultInfo().getUpid());

        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleSmartData(); // real work!
                handleGenericData(); // just size calculation for restore!
            }
        });

        mWorkerThread.start();
    }

    @Override
    public Boolean onSecureDbItemWhileScanning(MMLGenericDataDesc desc) {
        // calculate size
        if (mSessionV2.isAbortPending()) return null;

        String path;
        if (desc.onSdCard) {
            path = mStorage.toSecExtAbsPath(desc.mPath);
        } else {
            path = mStorage.toPrimaryExtAbsPath(desc.mPath);
        }

        File file = new File(path);
        if (!file.exists()) {
            if (desc.onSdCard) {
                mSessionExtMemSize += desc.mSize;
            } else {
                mSessionIntMemSize += desc.mSize;
            }
        }

        return false;
    }

    @Override
    public void onSecureDbScanningComplete(byte cat) {
        dbgTrace("securedb scanning complete for cat: " + cat);

        // update the size and result in relevant objects
        // Legacy code expects sizes in kb.
        mSessionV2.updateTotalSessionDataSize(mSessionIntMemSize / 1024, true);
        mSessionV2.updateEstimatedTotalSessionDataSize(mSessionIntMemSize / 1024, true);

        mSessionV2.updateTotalSessionDataSize(mSessionExtMemSize / 1024, false);
        mSessionV2.updateEstimatedTotalSessionDataSize(mSessionExtMemSize / 1024, false);

        // Arun: Bugfix: 221June2017
        mSessionIntMemSize = 0;
        mSessionExtMemSize = 0;
    }

    @Override
    public void onTotalItemCountForScannedCat(byte cat, int itemCount) {
        // Not used
    }

    // ===================================================================
    // Methods
    // ===================================================================
    public void abort() {
        dbgTrace();

        mAbortFlag.setTrue();

        if (contactsV2 != null) {
            contactsV2.abort();
        }

        if (calendarsV2 != null) {
            calendarsV2.abort();
        }

        if (messagesV2 != null) {
            messagesV2.abort();
        }
    }

    private boolean handleSmartData() {
        dbgTrace();

        boolean result = true;

        SessionSmartDataInfo sesSmartInfo = mSessionV2.getSmartDataInfo();
        HashMap<Byte, MMLSmartDataDesc> sddMap = sesSmartInfo.mDbFilesMap;

        for (Byte cat : sddMap.keySet()) {
            MMLSmartDataDesc desc = sddMap.get(cat);

            Context ctxt = mUiCtxt.getAppContext();
            String upid = mSessionV2.getVaultInfo().getUpid();

            // TODO: THIS CATCH ALL EXCEPTION HANDLING IS VERY POOR STYLE!
            try {
                switch (cat) {
                    case MMP_CATCODE_CONTACT:
                        contactsV2 = new ContactsV2(ctxt, upid);
                        result &= contactsV2.prepare();
                        result &= contactsV2.restore();
                        break;
                    case MMP_CATCODE_MESSAGE:
                        messagesV2 = new MessagesV2(ctxt, upid);
                        result &= messagesV2.prepare();
                        result &= messagesV2.restore();
                        break;
                    case MMP_CATCODE_CALENDER:
                        calendarsV2 = new CalendersV2(ctxt, upid);
                        result &= calendarsV2.prepare();
                        result &= calendarsV2.restore();
                        break;
                    default:
                        dbgTrace("BUG: Unknown smart cat: " + cat);
                        break;
                }
            } catch (Exception e) {
                dbgTrace("o!o!o Exception during restore/copy of smart data: " + desc + ": " + GenUtils.getStackTrace(e));
                result = false;
            }

            dbgTrace("Restoring smart cat: " + desc + ", result: " + result);

            if (mSessionV2.isAbortPending()) {
                dbgTrace("Abort requested noted");
                break;
            }

            postStatusInfo(cat, false);
        }

        return result;
    }

    private void postStatusInfo(byte cat, boolean start) {
        dbgTrace();

        MMPSessionStatusInfo.Type type = start ? MMPSessionStatusInfo.Type.STARTED : MMPSessionStatusInfo.Type.ENDED;
        MMPSessionStatusInfo statusInfo = new MMPSessionStatusInfo(cat, type);

        MeemEvent evt = new MeemEvent(EventCode.SESSION_PROGRESS_UPDATE);
        evt.setInfo(statusInfo);
        UiContext.getInstance().postEvent(evt);
    }

    /**
     * For restore, this is only generic data size calculation for the session.
     *
     * @return
     */
    private boolean handleGenericData() {
        dbgTrace();
        boolean result = true;

        String upid = mSessionV2.getVaultInfo().getUpid();
        int genCatCode = mSessionV2.getGenericDataInfo().getGenCatMask();
        ArrayList<Byte> cats = MMLCategory.getGenericCatCodesArray(genCatCode);

        if (cats.isEmpty()) {
            // No generic data to handle
            postResult(result);
            return result;
        }

        dbgTrace("Starting securedb scanning for generic data (primarily to calculate size)");
        SecureDb secDb = new SecureDb(upid);

        for (Byte cat : cats) {
            result &= secDb.scan(cat, this, true);

            if (mSessionV2.isAbortPending()) {
                dbgTrace("Aborting on request");
                result = false;
                break;
            }
        }

        postResult(result);
        return result;
    }

    private void postResult(boolean result) {
        dbgTrace();

        EventCode resCode = (result && (!mSessionV2.isAbortPending())) ? EventCode.RESTORE_PREPARATION_SUCCEEDED : EventCode.RESTORE_PREPARATION_FAILED;
        MeemEvent evt = new MeemEvent(resCode);
        UiContext.getInstance().postEvent(evt);
    }


    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logCat("RestoreV2", trace);
        GenUtils.logMessageToFile("RestoreV2.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
