package com.meem.v2.businesslogic;

import android.content.Context;

import com.meem.androidapp.UiContext;
import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.phone.MediaStorage;
import com.meem.phone.Storage;
import com.meem.phone.StorageScanListener;
import com.meem.utils.GenUtils;
import com.meem.v2.cablemodel.SecureDb;
import com.meem.v2.cablemodel.SecureDbProcessor;
import com.meem.v2.mmp.MMPV2Constants;
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
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_PHOTO;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_PHOTO_CAM;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_VIDEO;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_VIDEO_CAM;


/**
 * Created by arun on 15/4/17.
 */

public class BackupV2 implements SecureDbProcessor, StorageScanListener {
    UiContext mUiCtxt = UiContext.getInstance();
    Thread mWorkerThread;
    ContactsV2 contactsV2;
    MessagesV2 messagesV2;
    CalendersV2 calendarsV2;
    private MutableBoolean mAbortFlag = new MutableBoolean(false);
    private SecureDb mSecureDb;
    private Storage mStorage;
    private MediaStorage mMediaStorage;
    private SessionV2 mSessionV2;
    private int mGenMirrCatMask, mGenPlusCatMask;

    public interface BackupV2Listener {
        void saveSecureDb();
    }

    BackupV2Listener mListener;

    public BackupV2(SessionV2 sessionV2, BackupV2Listener listener) {
        dbgTrace();

        mSessionV2 = sessionV2;

        mGenMirrCatMask = mSessionV2.getGenericDataInfo().getGenCatMask();
        mGenPlusCatMask = mSessionV2.getGenericDataInfo().getGenPlusCatMask();

        mListener = listener;
    }

    public void start() {
        dbgTrace();

        mStorage = new Storage(this);
        mSecureDb = new SecureDb(mSessionV2.getVaultInfo().getUpid());
        mMediaStorage = new MediaStorage(this);

        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleSmartData(); // real work!
                handleGenericData(); // real work!
            }
        });

        mWorkerThread.start();
    }

    private boolean handleGenericData() {
        dbgTrace();

        boolean result = true;
        ArrayList<Byte> cats = MMLCategory.getGenericCatCodesArray(mGenMirrCatMask);
        for (Byte cat : cats) {
            result &= mSecureDb.process(cat, this, true);
        }

        // process new items in phone. No need to do this in onSecureDbProcessingComplete
        result &= mMediaStorage.scanForMedia(mGenMirrCatMask);

        return result;
    }

    /**
     * By default, backup_mode field in securedb is [mirror/sync]. When an item is found to be deleted,
     * if its backup mode is [plus/archive] (the cat is in plusmask), then [backup_mode] field shall be
     * set to 1.
     *
     * @param desc Generic data descriptor - some fields are not used. See comments on usage.
     *
     * @return
     */
    @Override
    public Boolean onSecureDbItemForProcessing(MMLGenericDataDesc desc) {
        dbgTrace();

        Boolean result = false;

        if (mAbortFlag.isTrue()) {
            dbgTrace("Abort request noted");
            return null;
        }

        if (!desc.mIsMirror) {
            dbgTrace("Ignoring archive item: " + desc.mPath);
        }

        // conversion for phone specific primary/secondary storage paths
        String path;
        if (desc.onSdCard) {
            path = mStorage.toSecExtAbsPath(desc.mPath);
        } else {
            path = mStorage.toPrimaryExtAbsPath(desc.mPath);
        }

        File file = new File(path);
        if (file.exists()) {
            dbgTrace("Backed up item exists: " + desc);
            // TODO: this is not entirely correct. What to do if item is modified?
            if (file.length() != desc.mSize) {
                desc.mIsDeleted = true;
                result = true;
            }
        } else {
            dbgTrace("Backed up item deleted: " + desc);
            desc.mIsDeleted = true;
            result = true;
        }

        if (desc.mIsDeleted) {
            if (MMLCategory.isGenericCategoryEnabled(desc.mCatCode, mGenPlusCatMask)) {
                dbgTrace("Archive mode is enabled for cat. Marking item to be archived: " + desc);
                desc.mIsMirror = false;
            }
        }

        return result;
    }

    @Override
    public void onSecureDbProcessingComplete() {
        dbgTrace();
    }

    @Override
    public boolean onGenericDataStorageItem(MMLGenericDataDesc genDesc) {
        dbgTrace();

        if (genDesc.mQuirkyDriod) {
            /**
             * Arun: 24Nov2015: So far, only observed this on Huawei Honor
             * H60-L04. See comments in MediaStore.java where this flag is set.
             */
            dbgTrace("Warning: The path: " + genDesc.mPath + "is not supported. Not backing up.");
            return true;
        }

        if (mAbortFlag.booleanValue()) {
            dbgTrace("Abort request noted");
            return false;
        }

        String fPath = genDesc.mPath;

        // Important: checkPresenceAndGetAckStatus is using size and phone_path (and in future, mod_time)
        File file = new File(fPath);

        genDesc.mModTime = file.lastModified();
        genDesc.mSize = file.length();

        String mPath;
        if (genDesc.onSdCard) {
            mPath = mStorage.fromSecondaryExtAbsPath(genDesc.mPath);
        } else {
            mPath = mStorage.fromPrimaryExtAbsPath(genDesc.mPath);
        }
        genDesc.mPath = mPath;

        // lets update the session data size
        long dataSize = (genDesc.mSize) / 1024; // KB

        if (mSecureDb.checkPresenceAndGetAckStatus(genDesc)) {
            dbgTrace("Item already present in securedb: " + genDesc);

            // if it is not backed up yet, account for its size in this session.
            if (!genDesc.mFwAck) {
                mSessionV2.updateTotalSessionDataSize(dataSize, genDesc.onSdCard ? false : true);
            }

            return true;
        }

        dbgTrace("New item to backup: " + genDesc);

        // lets update the session data size and insert this item as new entry in securedb.
        mSessionV2.updateTotalSessionDataSize(dataSize, genDesc.onSdCard ? false : true);

        genDesc.mFwAck = false;
        genDesc.mIsMirror = true;
        genDesc.mMeemInternalPath = GenUtils.genUniqueString(fPath, MMPV2Constants.MMP_FILE_XFR_FNVHASH_LEN);
        genDesc.mCSum = GenUtils.getFileMD5(fPath, mAbortFlag);

        switch (genDesc.mCatCode) {
            case MMP_CATCODE_PHOTO:
            case MMP_CATCODE_PHOTO_CAM:
                genDesc.mThumbNail = GenUtils.getImageThumbnailCustomMethod(fPath);
                break;
            case MMP_CATCODE_VIDEO:
            case MMP_CATCODE_VIDEO_CAM:
                genDesc.mThumbNail = GenUtils.getVideoThumbnailSystemMethod(fPath);
                break;
            default:
                break;
        }

        if (true != mSecureDb.insert(genDesc)) {
            dbgTrace("Securedb insert failed for: " + genDesc);
        } else {
            dbgTrace("Securedb insert succeeded for: " + genDesc);
        }

        return true;
    }

    @Override
    public void onStorageScanCompletionForCat(byte cat, boolean result) {
        dbgTrace();

        // Arun: 06Sep2017: Save the securedb after processing one category.
        if (mListener != null) {
            dbgTrace("Saving securedb after processing cat: " + MMLCategory.toGenericCatString(cat));
            mListener.saveSecureDb();
        }
    }

    @Override
    public void onStorageScanCompletion(boolean result) {
        dbgTrace();

        EventCode resCode = (!mAbortFlag.booleanValue() && result) ? EventCode.BACKUP_PREPARATION_SUCCEEDED : EventCode.BACKUP_PREPARATION_FAILED;

        MeemEvent evt = new MeemEvent(resCode);
        mUiCtxt.postEvent(evt);
    }

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

        SessionSmartDataInfo sesSmartInfo = mSessionV2.getSmartDataInfo();
        HashMap<Byte, MMLSmartDataDesc> sddMap = sesSmartInfo.mDbFilesMap;

        int smartPlusCatMask = mSessionV2.getVaultInfo().getSmartPlusDataCategoryMask();

        for (Byte cat : sddMap.keySet()) {
            MMLSmartDataDesc desc = sddMap.get(cat);

            dbgTrace("Starting backup of smartdata: " + desc);

            Context ctxt = mUiCtxt.getAppContext();
            String upid = mSessionV2.getVaultInfo().getUpid();

            boolean isPlus = MMLCategory.isSmartCategoryEnabled(cat, smartPlusCatMask);
            boolean result = true;

            // TODO: THIS CATCH ALL EXCEPTION HANDLING IS VERY POOR STYLE!
            try {
                switch (cat) {
                    case MMP_CATCODE_CONTACT:
                        contactsV2 = new ContactsV2(ctxt, upid);
                        result &= contactsV2.prepare();
                        result &= contactsV2.backup(isPlus);
                        break;
                    case MMP_CATCODE_MESSAGE:
                        messagesV2 = new MessagesV2(ctxt, upid);
                        result &= messagesV2.prepare();
                        result &= messagesV2.backup(isPlus);
                        break;
                    case MMP_CATCODE_CALENDER:
                        calendarsV2 = new CalendersV2(ctxt, upid);
                        result &= calendarsV2.prepare();
                        result &= calendarsV2.backup(isPlus);
                        break;
                    default:
                        dbgTrace("BUG: Unknown smart cat: " + cat);
                        break;
                }
            } catch (Exception e) {
                dbgTrace("o!o!o Exception during backup of smart data: " + desc + ": " + GenUtils.getStackTrace(e));
                result = false;
            }

            if (result) {
                desc.mCSums[0] = GenUtils.getFileMD5(desc.mPaths[0], null);
                desc.mCSums[1] = GenUtils.getFileMD5(desc.mPaths[1], null);

                desc.mSizes[0] = (new File(desc.mPaths[0])).length();
                desc.mSizes[1] = (new File(desc.mPaths[1])).length();

                dbgTrace("Updating securedb for smartdata: " + desc);
                mSecureDb.updateSmartDataTable(desc);
            } else {
                dbgTrace("Securedb update skipped because of error for smart data: " + desc);
            }

            postStatusInfo(cat, false);

            if (mSessionV2.isAbortPending()) {
                dbgTrace("Abort requested noted");
                break;
            }
        }

        return true;
    }

    private void postStatusInfo(byte cat, boolean start) {
        MMPSessionStatusInfo.Type type = start ? MMPSessionStatusInfo.Type.STARTED : MMPSessionStatusInfo.Type.ENDED;
        MMPSessionStatusInfo statusInfo = new MMPSessionStatusInfo(cat, type);

        MeemEvent evt = new MeemEvent(EventCode.SESSION_PROGRESS_UPDATE);
        evt.setInfo(statusInfo);
        UiContext.getInstance().postEvent(evt);
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logCat("BackupV2", trace);
        GenUtils.logMessageToFile("BackupV2.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
