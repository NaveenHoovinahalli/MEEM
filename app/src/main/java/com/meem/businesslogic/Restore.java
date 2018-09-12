package com.meem.businesslogic;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ContactTrackerWrapper;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.messages.MMPSessionStatusInfo.Type;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.phone.Storage;
import com.meem.phone.StorageScanListener;
import com.meem.utils.GenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import meem.org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * You must read the detailed javadoc given for class GenericDataDatabase to understand the backup logic properly. Also, since we are not
 * dealing with plus data in restore, logic is much simpler here.
 *
 * @author Arun T A
 */

public class Restore implements DatdProcessingListener, StorageScanListener, GenericDataDbListener {
    private static final String tag = "Restore";

    Object mCompletionMonitor;
    boolean mIsSmartDataProcessingCompleted;
    boolean mIsGenericDataProcessingCompleted;
    boolean mIsSessionPrepSucceeded;

    boolean mAbortRequested;
    // An object is needed to get this over to checksum algorithm implementation
    MutableBoolean mAbortRequestFlagObject = new MutableBoolean(false);

    DATD mDatd;
    GenericDataDatabase mDatdDb;
    GenericDataCacheDatabase mCacheDb;

    Storage mStorage;
    SESD mSesd;

    Session mSession;
    int mGenCatMask;

    private UiContext mUiCtxt = UiContext.getInstance();
    private AppLocalData mAppData = AppLocalData.getInstance();

    private Thread mGenericDataProcessor = null;
    private Thread mSmartDataProcessor = null;

    private com.meem.phone.Contacts mContacts = null;
    private com.meem.phone.Messages mMessages = null;
    private com.meem.phone.Calenders mCalenders = null;

    public Restore(Session sesInfo) {
        dbgTrace();

        mCompletionMonitor = new Object();
        mSession = sesInfo;

        // we are not dealing with generic plus data in restore.
        mGenCatMask = sesInfo.getGenericDataInfo().getGenCatMask();
        dbgTrace("Generic data category mask for session is: " + mGenCatMask);

        mIsSmartDataProcessingCompleted = false;
        mIsGenericDataProcessingCompleted = false;
        mIsSessionPrepSucceeded = false;

        mAbortRequested = false;
    }

    private void postStatusInfo(byte cat, boolean start) {
        MMPSessionStatusInfo.Type type = start ? Type.STARTED : Type.ENDED;
        MMPSessionStatusInfo statusInfo = new MMPSessionStatusInfo(cat, type);

        MeemEvent evt = new MeemEvent(EventCode.SESSION_PROGRESS_UPDATE);
        evt.setInfo(statusInfo);
        mUiCtxt.postEvent(evt);
    }

    public boolean start() {
        dbgTrace();

        mDatd = new DATD(this);
        mSesd = new SESD();

        mDatdDb = new GenericDataDatabase(this);
        mCacheDb = new GenericDataCacheDatabase();

        mStorage = new Storage(this);

        dbgTrace("Clearing DATD database");
        mDatdDb.deleteAll();

        int items = mDatdDb.getNumRows();
        if (items != 0) {
            dbgTrace("BUG: DATD database is not cleared: " + items + " items still present");
            return false;
        }

        mGenericDataProcessor = new Thread(new Runnable() {
            public void run() {
                // optimization for corner cases
                if (0 != mGenCatMask) {
                    mDatd.process();
                } else {
                    dbgTrace("No generic data specified to process.");
                    onGenericDataProcessingCompletion(true);
                }
            }
        });

        mGenericDataProcessor.start();

        mSmartDataProcessor = new Thread(new Runnable() {
            public void run() {
                processSmartData();
            }
        });

        mSmartDataProcessor.start();

        return true;
    }

    public boolean abort() {
        dbgTrace();

        mAbortRequested = true;
        mAbortRequestFlagObject.setTrue();

        // 27Nov2015: If both generic and smart data processing is not running,
        // then we must ack the abort request here itself.
        if (!mGenericDataProcessor.isAlive() && !mSmartDataProcessor.isAlive()) {
            dbgTrace("Abort requested during idle state. Acking the main thread.");
            MeemEvent evt = new MeemEvent(EventCode.RESTORE_PREPARATION_FAILED);
            mUiCtxt.postEvent(evt);
        }

        // no need to worry about race conditions.
        if (null != mContacts) {
            mContacts.abort();
        }
        if (null != mMessages) {
            mMessages.abort();
        }
        if (null != mCalenders) {
            mCalenders.abort();
        }
        if (null != mStorage) {
            mStorage.abortFileSystemScan();
        }
        return true;
    }

    @Override
    public boolean onNewGenericDataDesc(MMLGenericDataDesc genDataDesc) {
        dbgTrace();

        if (mAbortRequested) {
            // This will make sure that DATD processing is stopped
            return false;
        }

        // XXX: SHOCKING HACK for handling external storage. See comments
        // in Storage.java and here
        if (genDataDesc.mCatCode == MMPConstants.MMP_CATCODE_PHOTO_CAM || genDataDesc.mCatCode == MMPConstants.MMP_CATCODE_VIDEO_CAM || genDataDesc.mCatCode == MMPConstants.MMP_CATCODE_FILE || genDataDesc.mCatCode == MMPConstants.MMP_CATCODE_DOCUMENTS_SD) {
            dbgTrace("SDCARD Item detected: " + genDataDesc.mPath);
            if ('S' != genDataDesc.mPath.charAt(0)) {
                dbgTrace("BUG: SDCARD item path does not start with S: " + genDataDesc.mPath);
                return false;
            }
            genDataDesc.mPath = genDataDesc.mPath.substring(1);
            genDataDesc.onSdCard = true;
        } else {
            if ('I' != genDataDesc.mPath.charAt(0)) {
                dbgTrace("BUG: Internal storage item path does not start with I: " + genDataDesc.mPath);
                return false;
            }
            genDataDesc.mPath = genDataDesc.mPath.substring(1);
            genDataDesc.onSdCard = false;
        }

        // now go and see if the item is present or modified.
        // 28Oct2015: Changes for abort handling during lengthy checksum calculation.
        mStorage.updateItemStatusForRestore(genDataDesc, mAbortRequestFlagObject);
        if (mAbortRequested) {
            // This will make sure that DATD processing is stopped & invalid BEEFDEAD csums will be properly ignored
            return false;
        }

        // update estimated data size
        if (genDataDesc.mStatus != Storage.ITEM_UNCHANGED) {
            // update session information for the data size
            long dataSize = (genDataDesc.mSize) / 1024; // KB
            mSession.updateEstimatedTotalSessionDataSize(dataSize, genDataDesc.onSdCard ? false : true);
        }

        dbgTrace("Adding item to db: " + genDataDesc.mPath);
        return mDatdDb.add(genDataDesc);
    }

    @Override
    public void onDatdProcessingCompletion(boolean result) {
        onGenericDataProcessingCompletion(result);
    }

    @Override
    public boolean onGenericDataStorageItem(MMLGenericDataDesc genDataDesc) {
        // will never be used.
        return true;
    }

    // Added: Arun: 06Sep2017: Not used for V1.
    @Override
    public void onStorageScanCompletionForCat(byte cat, boolean result) {
        // will never be used.
    }

    @Override
    public void onStorageScanCompletion(boolean result) {
        // will never be used.
    }

    private void onGenericDataProcessingCompletion(boolean result) {
        dbgTrace();

        // take lock
        synchronized (mCompletionMonitor) {
            mIsGenericDataProcessingCompleted = true;

            // if smart data processing is in progress
            if (!mIsSmartDataProcessingCompleted) {
                try {
                    // wait for it to complete
                    dbgTrace("Waiting for smart data processing to complete");

                    // FindBugs fix.
                    while (!mIsSmartDataProcessingCompleted) {
                        mCompletionMonitor.wait();
                    }
                } catch (InterruptedException ex) {
                    result = false;
                }

                // smart data processing is complete. consider its result and
                // go for SESD creation
                boolean prepResult = mIsSessionPrepSucceeded & result;
                if (prepResult) {
                    createSESD();
                } else {
                    MeemEvent evt = new MeemEvent(EventCode.BACKUP_PREPARATION_FAILED);
                    mUiCtxt.postEvent(evt);
                    return;
                }
            } else {
                // smart data processing completed first and is waiting for this
                // to finish. update result and notify it that we are done.
                mIsSessionPrepSucceeded = result;
                mCompletionMonitor.notifyAll();
            }
        }
    }

    private void processSmartData() {
        dbgTrace();
        @SuppressWarnings("unused") boolean result = true;

        if (mSession == null) {
            onSmartDataProcessingCompletion(false);
            return;
        }

        SessionSmartDataInfo sesSmartInfo = mSession.getSmartDataInfo();
        HashMap<Byte, ArrayList<MMPFPath>> catsFilesMap = sesSmartInfo.getCatsFilesMap();
        for (Entry<Byte, ArrayList<MMPFPath>> catMap : catsFilesMap.entrySet()) {
            Byte cat = catMap.getKey();
            ArrayList<MMPFPath> files = catMap.getValue();

            String upidStr = mSession.getVaultInfo().getUpid();
            String catStr = MMLCategory.toSmartCatString(cat.intValue());

            String mirrorFile = files.get(0).toString();
            String mirrorPlusFile = files.get(1).toString();

            String outMirr = mAppData.getSmartCatOutFilePath(upidStr, catStr, true);
            // Design change 23July2014: MirrorPlus file and Mirror file are to
            // be same.
            // String outMirrPlus = mAppData.getSmartCatOutFilePath(upidStr,
            // catStr, false);

            // mirrorPlusFile can be null if there is only mirror.
            String inMirrPlus, outMirrPlus;
            if (mirrorPlusFile != null) {
                inMirrPlus = mirrorPlusFile;
                outMirrPlus = mirrorPlusFile;
            } else {
                String mirrorPlusEmptyFile = mAppData.getSmartCatEmptyFilePath(upidStr, catStr, false);
                inMirrPlus = mirrorPlusEmptyFile;
                outMirrPlus = mirrorPlusEmptyFile;
            }

            // The following case (mirror file null) will not happen.
            // Keeping it for the time being.
            String mirrorEmptyFile = mAppData.getSmartCatEmptyFilePath(upidStr, catStr, true);
            String inMirr = (null != mirrorFile) ? mirrorFile : mirrorEmptyFile;

            try {
                byte catByte = cat.byteValue();

                postStatusInfo(catByte, true);

                if (catByte == MMPConstants.MMP_CATCODE_CONTACT) {
                    dbgTrace("Restoring contacts with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");

                    mContacts = new com.meem.phone.Contacts(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    result &= mContacts.prepare();
                    result &= mContacts.restore();

                    ContactTrackerWrapper contactTrackWrap = mContacts.getContactTrackingWrapper();
                    if (contactTrackWrap != null) {
                        dbgTrace("Updating contact tracker");
                        if (mSession.getType() != MMPConstants.MMP_CODE_CREATE_CPY_SESSION) {
                            contactTrackWrap.onContactRestoreCompleted();
                        }
                    } else {
                        dbgTrace("BUG: Contact tracking wrapper is null white restore or copy!");
                    }

                    dbgTrace("Finished restoring contacts.");
                } else if (catByte == MMPConstants.MMP_CATCODE_MESSAGE) {
                    dbgTrace("Restoring messages with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");

                    mMessages = new com.meem.phone.Messages(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    result &= mMessages.prepare();
                    result &= mMessages.restore();

                    dbgTrace("Finished restoring messages.");
                } else if (catByte == MMPConstants.MMP_CATCODE_CALENDER) {
                    dbgTrace("Restoring calenders with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");

                    mCalenders = new com.meem.phone.Calenders(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    result &= mCalenders.prepare();
                    result &= mCalenders.restore();

                    dbgTrace("Finished restoring calenders.");
                }

                postStatusInfo(catByte, false);

                if (null != mSession && mSession.isAbortPending()) {
                    dbgTrace("Smart data processing aborted");
                    result = false;
                    break;
                }

                // update session info with FPaths output files
                File outMirrFile = new File(outMirr);
                MMPFPath outMirrFPath = new MMPFPath(outMirr, outMirrFile.length());

                File outMirrPlusFile = new File(outMirrPlus);
                MMPFPath outMirrPlusFPath = new MMPFPath(outMirrPlus, outMirrPlusFile.length());

                if (null != mirrorFile) {
                    files.remove(0);
                    files.add(0, outMirrFPath);
                }

                if (null != mirrorPlusFile) {
                    files.remove(1);
                    files.add(1, outMirrPlusFPath);
                }
            } catch (Exception ex) {
                dbgTrace("Exception while processing smart data info: " + GenUtils.getStackTrace(ex));
                result = false;
                break;
            }
        }

        onSmartDataProcessingCompletion(/*result*/true); // TODO: We are proceeding with true.
    }

    // This will be called from smart data processor thread
    private void onSmartDataProcessingCompletion(boolean result) {
        dbgTrace();

        // take lock
        synchronized (mCompletionMonitor) {
            mIsSmartDataProcessingCompleted = true;

            // if generic data processing is in progress
            if (!mIsGenericDataProcessingCompleted) {
                try {
                    // wait for it to complete
                    dbgTrace("Waiting for generic data processing to complete");

                    // findBugs fix.
                    while (!mIsGenericDataProcessingCompleted) {
                        mCompletionMonitor.wait();
                    }
                } catch (InterruptedException ex) {
                    result = false;
                }

                // generic data processing is complete. consider its result and
                // go for SESD creation
                boolean prepResult = mIsSessionPrepSucceeded & result;
                if (prepResult) {
                    createSESD();
                } else {
                    MeemEvent evt = new MeemEvent(EventCode.BACKUP_PREPARATION_FAILED);
                    mUiCtxt.postEvent(evt);
                    return;
                }
            } else {
                // generic data processing completed first and is waiting for
                // this to finish. update result and notify it that we are done.
                mIsSessionPrepSucceeded = result;
                mCompletionMonitor.notifyAll();
            }
        }
    }

    private void createSESD() {
        dbgTrace();
        boolean result = true;

        // Now scan the Generic Data DB and create SESD
        mSesd.begin();
        mSesd.addSmartDataSection(mSession.getSmartDataInfo());

        mSesd.beginGenericData();

        byte[] genCats = {MMPConstants.MMP_CATCODE_PHOTO, MMPConstants.MMP_CATCODE_MUSIC, MMPConstants.MMP_CATCODE_VIDEO, MMPConstants.MMP_CATCODE_DOCUMENTS,
                // Note: This is music on external storage :(
                MMPConstants.MMP_CATCODE_FILE, MMPConstants.MMP_CATCODE_PHOTO_CAM, MMPConstants.MMP_CATCODE_VIDEO_CAM, MMPConstants.MMP_CATCODE_DOCUMENTS_SD};
        for (int i = 0; i < genCats.length; i++) {
            byte cat = genCats[i];

            if (MMLCategory.isGenericCategoryEnabled(cat, mGenCatMask)) {
                mSesd.beginGenCategory(cat);

                mSesd.beginMirror();

                // NOTE: Don't be smart and call scan 2 times for MODIFIED and
                // DELETED. It will mess up with SESD creation logic.
                // Note beginMirror and endMirror.
                result = mDatdDb.scanForCategoryWithoutStatus(cat, Storage.ITEM_UNCHANGED);

                mSesd.endMirror();

                mSesd.endGenCategory(cat);
            }

            // ABORT check point
            if (mAbortRequested) {
                dbgTrace("Aborting SESD preperation on request");
                result = false;
                break;
            }
        }

        mSesd.endGenericData();
        mSesd.end();

        MeemEvent evt = new MeemEvent(result ? EventCode.RESTORE_PREPARATION_SUCCEEDED : EventCode.RESTORE_PREPARATION_FAILED);
        mUiCtxt.postEvent(evt);
    }

    @Override
    public boolean onDatabaseItemRetrieved(MMLGenericDataDesc desc) {
        dbgTrace();

        if (mAbortRequested) {
            return false;
        }

        // final processing. one generic data item of a category is retrieved
        // from db with required status
        dbgTrace("Adding to SESD: " + desc);

        // XXX: SHOCKING HACK: if file is on external SDCARD, prepend the path
        // with 'S'
        // else prepend with 'I'
        String pathHack;
        if (desc.onSdCard) {
            pathHack = "S";
        } else {
            pathHack = "I";
        }

        desc.mPath = pathHack + desc.mPath;

        mSesd.addGenericDataFile(desc);

        // update session information for the data size
        long dataSize = (desc.mSize) / 1024; // KB
        mSession.updateTotalSessionDataSize(dataSize, desc.onSdCard ? false : true);

        return true;
    }

    @Override
    public void onDatabaseScanCompleted(boolean result) {
        dbgTrace();

        if (!result) {
            MeemEvent evt = new MeemEvent(EventCode.RESTORE_PREPARATION_FAILED);
            mUiCtxt.postEvent(evt);
        }
    }

    public void updateModTime(String filePath) {
        if (null != mDatdDb && null != mStorage) {
            String datdPath = mStorage.fromPrimaryExtAbsPath(filePath);
            long modTime = mDatdDb.getModTime(datdPath);
            if (modTime > 0) {
                File file = new File(filePath);
                if (!file.setLastModified(modTime)) {
                    dbgTrace("Error: Unable to set last modified time for file: " + filePath);
                }
            }
        }
    }

    // debugging
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("Restore.log", trace);
    }

    // debugging
    private void dbgTrace() {
        GenUtils.logMethodToFile("Restore.log");
    }
}
