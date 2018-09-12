package com.meem.businesslogic;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ContactTrackerWrapper;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.messages.MMPSessionStatusInfo.Type;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.phone.MediaStorage;
import com.meem.phone.Storage;
import com.meem.phone.StorageScanListener;
import com.meem.utils.GenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import meem.org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * You must read the detailed javadoc given for class GenericDataDatabase to understand the backup logic properly.
 *
 * @author Arun T A
 */

public class Backup implements DatdProcessingListener, StorageScanListener, GenericDataDbListener {
    private static final String tag = "Backup";
    final Session mSession;
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
    GenericDataThumbnailDatabase mThumbDb;
    Storage mStorage;
    MediaStorage mMediaStorage;
    SESD mSesd;
    int mGenCatMask, mGenPlusCatMask;
    private UiContext mUiCtxt = UiContext.getInstance();
    private AppLocalData mAppData = AppLocalData.getInstance();

    private Thread mGenericDataProcessor = null;
    private Thread mSmartDataProcessor = null;

    private com.meem.phone.Contacts mContacts = null;
    private com.meem.phone.Messages mMessages = null;

    private com.meem.phone.Calenders mCalenders = null;

    public Backup(Session sesInfo) {
        dbgTrace();

        mCompletionMonitor = new Object();

        mSession = sesInfo;

        mGenCatMask = sesInfo.getGenericDataInfo().getGenCatMask();
        mGenPlusCatMask = sesInfo.getGenericDataInfo().getGenPlusCatMask();

        dbgTrace("Generic data category mirror mask for session is: " + mGenCatMask + " Mirror+ mask: " + mGenPlusCatMask);

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
        mThumbDb = new GenericDataThumbnailDatabase(mSession.getVaultInfo().getUpid());

        mStorage = new Storage(this);
        mMediaStorage = new MediaStorage(this);

        dbgTrace("Clearing DATD database");
        mDatdDb.deleteAll();

        int items = mDatdDb.getNumRows();
        if (items != 0) {
            dbgTrace("BUG: DATD database is not cleared: " + items + " items still present");
            return false;
        }

        items = mCacheDb.getNumRows();
        dbgTrace("Checksum cache database has: " + items + " items.");

        mGenericDataProcessor = new Thread(new Runnable() {
            public void run() {
                // optimization for corner cases
                if (0 != mGenCatMask || 0 != mGenPlusCatMask) {
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
            MeemEvent evt = new MeemEvent(EventCode.BACKUP_PREPARATION_FAILED);
            mUiCtxt.postEvent(evt);
        }

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
        if (null != mMediaStorage) {
            mMediaStorage.abortMediaStorageScan();
        }

        return true;
    }

    @Override
    public boolean onNewGenericDataDesc(MMLGenericDataDesc genDataDesc) {
        dbgTrace();
        boolean result = true;

        if (mAbortRequested) {
            // This will make sure that DATD processing is stopped
            return false;
        }

        // called by DATD during parsing.
        if (null == genDataDesc.mPath) {
            // Minor hack for empty item (will happen on very first
            // backup).
            return true;
        }

        // XXX: SHOCKING HACK for handling external storage. See comments
        // in Storage.java and here (Arun: 03Jan2017: Bugfix: Forgot to add doc_sd category here).
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

        // try to get checksum from database
        String cachedChecksum = mCacheDb.getChecksum(genDataDesc);
        if (null == cachedChecksum) {
            dbgTrace("Warning: Checksum was not found in cache for DATD item (app uninstalled?): " + genDataDesc.mPath);
        }
        // 28Oct2015: Changes for abort handling during lengthy checksum calculation.
        mStorage.updateItemStatusForBackup(genDataDesc, cachedChecksum, mAbortRequestFlagObject);
        if (mAbortRequested) {
            // This will make sure that DATD processing is stopped & invalid BEEFDEAD csums wont get into cache
            return false;
        }

        if (genDataDesc.mCSum == null) {
            // This is taken care of below by null checks.
            dbgTrace("WARNING: checksum is null after DATD item processing (taken care of)");
        }

        // if item has modified after last backup, we must update cache
        if ((genDataDesc.mStatus != Storage.ITEM_DELETED) && (null != genDataDesc.mCSum)) {
            if (null == cachedChecksum) {
                dbgTrace("Adding item cache db (app uninstalled?): " + genDataDesc.mPath);
                result = mCacheDb.add(genDataDesc);
            } else {
                if (genDataDesc.mStatus == Storage.ITEM_MODIFIED) {
                    dbgTrace("Updating cache db: " + genDataDesc.mPath);
                    result = mCacheDb.update(genDataDesc);
                }
            }
            if (!result) {
                dbgTrace("Cache add or update failed for DATD item: " + genDataDesc.mPath);
            }
        } else {
            // deleting deleted item from cache.
            dbgTrace("Removing deleted item from cache db: " + genDataDesc.mPath);
            result = mCacheDb.delete(genDataDesc);
        }
        // add it to DATD database for session processing - as a precaution against bugs
        // never add items without checksum
        if (genDataDesc.mCSum != null) {
            dbgTrace("Adding " + genDataDesc.mPath + " to DATD db. Status: " + genDataDesc.mStatus + " Size: " + ((genDataDesc.mSize) / 1024) + "KB");
            result = mDatdDb.add(genDataDesc);
            // update session information for the data size (Arun: 25July2015)
            if (genDataDesc.mStatus != Storage.ITEM_DELETED) {
                if (genDataDesc.mStatus == Storage.ITEM_MODIFIED) {
                    long dataSize = (genDataDesc.mSize) / 1024; // KB
                    mSession.updateEstimatedTotalSessionDataSize(dataSize, genDataDesc.onSdCard ? false : true);
                }

                // Add it to thumbnail db (Arun: 01Dec2016)
                String phoneSpecificPath;
                if (genDataDesc.onSdCard) {
                    phoneSpecificPath = mStorage.toSecExtAbsPath(genDataDesc.mPath);
                } else {
                    phoneSpecificPath = mStorage.toPrimaryExtAbsPath(genDataDesc.mPath);
                }

                genDataDesc.mPathForThumb = phoneSpecificPath;
                // Important: this is from cable. So, scenarios where old version of fw (no thumbnail) is involved, we must set the fw ack.
                result = addToThumbnailDb(genDataDesc, true);

                if (!result) {
                    dbgTrace("Error while processing for thumbnail for DATD item: " + genDataDesc);
                    result = true;
                }
            }
        } else {
            dbgTrace("BUG: NOT adding " + genDataDesc.mPath + " to DATD db. Status: " + genDataDesc.mStatus + " as csum is null");
        }
        if (!result) {
            dbgTrace("Unable to process DATD item: " + genDataDesc);
        }

        return result;
    }

    @Override
    public void onDatdProcessingCompletion(boolean result) {
        if (result == false) {
            dbgTrace("DATD processing returns error: " + (mAbortRequested ? "abort requested" : "see logs"));
            onGenericDataProcessingCompletion(false);
        } else {
            dbgTrace("Begining generic data scanning: mask: " + String.valueOf(mGenCatMask));

            if (ProductSpecs.USE_SYSTEM_MEDIA_STORE) {
                mMediaStorage.scanForMedia(mGenCatMask);
            } else {
                mStorage.scanFileSystem(mGenCatMask, mAbortRequestFlagObject);
            }
        }
    }

    /**
     * IMPORTANT: This interface is used now as a hook for cached checksum optimization and also for updating the db with new files created
     * after previous backup. Since this comes from media store, we need to modify the path to a meem specific path - without root path of
     * storage. See comments below.
     * <p/>
     * XXX: If there is a bug, check this function. Also revisit while adding support for secondary external storage.
     */
    @Override
    public boolean onGenericDataStorageItem(MMLGenericDataDesc genDataDesc) {
        dbgTrace();
        boolean result = true;

        if (genDataDesc.mQuirkyDriod) {
            /**
             * Arun: 24Nov2015: So far, only observed this on Huawei Honor
             * H60-L04. See comments in MediaStore.java where this flag is set.
             */
            dbgTrace("Warning: The path: " + genDataDesc.mPath + "is not supported. Not backing up.");
            return true;
        }


        String meemSpecificPath;
        if (genDataDesc.onSdCard) {
            meemSpecificPath = mStorage.fromSecondaryExtAbsPath(genDataDesc.mPath);
        } else {
            meemSpecificPath = mStorage.fromPrimaryExtAbsPath(genDataDesc.mPath);
        }

        // Keep phone specific path for thumbnail generation.
        genDataDesc.mPathForThumb = genDataDesc.mPath;
        genDataDesc.mPath = meemSpecificPath;

        // try to get checksum from database
        String cachedChecksum = mCacheDb.getChecksum(genDataDesc);

        // 28Oct2015: Changes for abort handling during lengthy checksum calculation.
        mStorage.updateItemStatusForBackup(genDataDesc, cachedChecksum, mAbortRequestFlagObject);
        if (mAbortRequested) {
            // This will make sure that DATD processing is stopped & invalid BEEFDEAD csums wont get into cache
            return false;
        }

        // if item has changed, and if we had a checksum from cache for it, it
        // means item has changed after last backup. Else it is a new item
        // created after last backup, so we must add it to cache.
        if (genDataDesc.mStatus != Storage.ITEM_UNCHANGED) {
            if (genDataDesc.mStatus == Storage.ITEM_DELETED) {
                dbgTrace("Removing deleted item from cache: " + genDataDesc);
                result = mCacheDb.delete(genDataDesc);
            } else {
                if (null != cachedChecksum) {
                    dbgTrace("Updating item in cache: " + genDataDesc);
                    result = mCacheDb.update(genDataDesc);
                } else {
                    dbgTrace("Adding new item to cache: " + genDataDesc);
                    result = mCacheDb.add(genDataDesc);
                }
            }
        }

        if (!result) {
            dbgTrace("Error dealing with cache for item: " + genDataDesc);
            return result;
        }

        // add it to DATD database for session processing - if it is not
        // already there in DATD
        if (mDatdDb.isPresent(genDataDesc.mCSum, genDataDesc.mPath)) {
            dbgTrace("NOT adding " + genDataDesc.mPath + ": it is already present in db");
        } else {
            genDataDesc.mStatus = Storage.ITEM_MODIFIED;
            dbgTrace("Adding " + genDataDesc.mPath + " to DATD db");
            // a precautionary check against any unseen scenarios - like app uninstalled etc.
            if (genDataDesc.mCSum != null) {
                result = mDatdDb.add(genDataDesc);

                // update session information for the data size (Arun: 25July2015)
                long dataSize = (genDataDesc.mSize) / 1024; // KB
                mSession.updateEstimatedTotalSessionDataSize(dataSize, genDataDesc.onSdCard ? false : true);
            } else {
                dbgTrace("BUG: checksum is null after storage item processing");
            }
        }

        // Arun: 10Aug2016: added for app v2.
        // For new/modified photos and videos, add/update item in thumbnail db
        if (genDataDesc.mStatus != Storage.ITEM_DELETED) {
            result = addToThumbnailDb(genDataDesc, false);
        }

        if (!result) {
            dbgTrace("Error while processing storage item: " + genDataDesc);
        }

        return true;
    }

    // Added: Arun: 06Sep2017: Not used for V1.
    @Override
    public void onStorageScanCompletionForCat(byte cat, boolean result) {
        // will never be used.
    }

    @Override
    public void onStorageScanCompletion(boolean result) {
        dbgTrace();
        onGenericDataProcessingCompletion(result);
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

    private boolean deleteOutMirrFileNRenameInToOut(String inMirr, String outMirr) {
        File filedelete = new File(outMirr);
        boolean res = true;
        if (filedelete.exists()) {
            if (filedelete.delete()) {
                System.out.println("File deleted :" + outMirr);
            } else {
                System.out.println("File not deleted :" + outMirr);
                res = false;
            }
        }
        File inMirrFile = new File(inMirr);
        File outMirrFile = new File(outMirr);
        res = inMirrFile.renameTo(outMirrFile);
        if (res) {
            dbgTrace("Renamed successfully");
        }
        return res;
    }

    private void processSmartData() {
        dbgTrace();
        @SuppressWarnings("unused") boolean result = true;

        // FindBugs fix.
        if (null == mSession) {
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
                    dbgTrace("Backing up contacts with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");
                    mContacts = new com.meem.phone.Contacts(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    mSession.setContactTrackingWrapper(mContacts.getContactTrackingWrapper());

                    ContactTrackerWrapper contactTrackerWrapper = mContacts.getContactTrackingWrapper();

                    File inMirrFile = new File(inMirr);
                    if (inMirrFile.length() == 0) {
                        // TODO: Remove this stuff! More importantly, we are checking for mirror size only
                        if (0 != mSession.mVaultInfo.getSmartDataMirrorOnlyUsage(MMPConstants.MMP_CATCODE_CONTACT)) {
                            dbgTrace("BUG: Mirror file size does not match with vault usage info");
                        }

                        // This may happen either because a. first backup b. delete contact category
                        // c. delete vault d. cable reset etc.
                        dbgTrace("Resetting local contact tracker database on empty mirror info");
                        contactTrackerWrapper.resetLocalContactDatabase();
                    }

                    while (true == contactTrackerWrapper.isSyncPending()) {
                        dbgTrace("Waiting for contact sync to complete...");
                        try {
                            Thread.sleep(1000);
                            // This is gonna be lengthy. So check this here and get out.
                            if (mSession.isAbortPending()) {
                                break;
                            }
                        } catch (Exception ex) {
                            dbgTrace("Exception while waiting for contact sync to complete");
                        }
                    }

                    dbgTrace("Meem contact tracker service has synced local contact changes. We are ok to go.");

                    switch (contactTrackerWrapper.getContactDbStatus()) {
                        case ContactTrackerWrapper.CONTACT_SKIP:
                            dbgTrace("CONTACT_SKIP");

                            if (deleteOutMirrFileNRenameInToOut(inMirr, outMirr)) {
                                dbgTrace("deleteNRename is success");
                            } else {
                                dbgTrace("deleteNRename is failed");
                            }
                            break;
                        case ContactTrackerWrapper.CONTACT_PROCESS:
                            dbgTrace("CONTACT_PROCESS");
                            result &= mContacts.prepare();
                            result &= mContacts.backup();
                            break;

                        case ContactTrackerWrapper.CONTACT_APPEND_PROCESS:
                            dbgTrace("CONTACT_APPEND_PROCESS");
                        /*
                         * Mirr file as input in append mode and add new
						 * contacts ,and then go for process to remove the
						 * deleted from the mirr and move to mirrPlus file
						 */
                        /*result &= mContacts.append(inMirr, outMirr);
                        result &= mContacts.backup();

						if (deleteOutMirrFileNRenameInToOut(inMirr, outMirr)) {
							dbgTrace("deleteNRename is success");
						} else {
							dbgTrace("deleteNRename is failed");
						}*/
                        /*this is change done by @keyur As per it will alaways add new  contact to mirror
						 * and will not remove deleted one from mirror file;*/
                            result &= mContacts.prepare();
                            result &= mContacts.backup();

                            break;
                        case ContactTrackerWrapper.CONTACT_APPEND_NEW:
                            dbgTrace("CONTACT_APPEND_NEW");
						/*
						 * Mirr file as input in append mode and add new
						 * contacts
						 */

                            result &= mContacts.append(inMirr, outMirr);

                            if (deleteOutMirrFileNRenameInToOut(inMirr, outMirr)) {
                                dbgTrace("deleteNRename is success");
                            } else {
                                dbgTrace("deleteNRename is failed");
                            }

                            break;
                        case ContactTrackerWrapper.CONTACT_BOTH_PROCESS_PREPARE:
                            dbgTrace("CONTACT_BOTH_PROCESS_PREPARE");
                            result &= mContacts.prepare();
                            result &= mContacts.backup();
                            break;
                        case ContactTrackerWrapper.CONTACT_PREPARE:
                            dbgTrace("CONTACT_PREPARE");
                            result &= mContacts.prepare();
                            break;
                    }
                    dbgTrace("Finished backing up contacts.");
                } else if (catByte == MMPConstants.MMP_CATCODE_MESSAGE) {
                    dbgTrace("Backing up messages with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");

                    mMessages = new com.meem.phone.Messages(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    result &= mMessages.prepare();
                    result &= mMessages.backup();

                    dbgTrace("Finished processing messages.");
                } else if (catByte == MMPConstants.MMP_CATCODE_CALENDER) {
                    dbgTrace("Backing up calenders with: [" + inMirr + ", " + inMirrPlus + "] and [" + outMirr + ", " + outMirrPlus + "]");

                    mCalenders = new com.meem.phone.Calenders(mUiCtxt.getAppContext(), inMirr, inMirrPlus, outMirr, outMirrPlus);

                    result &= mCalenders.prepare();
                    result &= mCalenders.backup();

                    dbgTrace("Finished backing up calenders.");
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

                    // FindBugs fix.
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
                // this
                // to finish. update result and notify it that we are done.
                mIsSessionPrepSucceeded = result;
                mCompletionMonitor.notifyAll();
            }
        }
    }

    /*
     * No return code because all results will be posted as events. This is
     * needed because this function may be called from 3 threads (including UI
     * thread).
     */
    private void createSESD() {
        dbgTrace();

        // Now scan the Generic Data DB and create SESD
        mSesd.begin();

        // Pass file details from Session.
        if (false == mSesd.addSmartDataSection(mSession.getSmartDataInfo())) {
            MeemEvent evt = new MeemEvent(EventCode.BACKUP_PREPARATION_FAILED);
            mUiCtxt.postEvent(evt);
            return;
        }

        // Now process generic data categories involved in this session
        mSesd.beginGenericData();

        boolean doMirrCat = false, doPlusCat = false, result = true;

        byte[] genCats = {MMPConstants.MMP_CATCODE_PHOTO, MMPConstants.MMP_CATCODE_MUSIC, MMPConstants.MMP_CATCODE_VIDEO, MMPConstants.MMP_CATCODE_DOCUMENTS,
                // Note: This is music on external storage :(
                MMPConstants.MMP_CATCODE_FILE, MMPConstants.MMP_CATCODE_PHOTO_CAM, MMPConstants.MMP_CATCODE_VIDEO_CAM, MMPConstants.MMP_CATCODE_DOCUMENTS_SD};

        for (int i = 0; i < genCats.length; i++) {
            if (mAbortRequested) {
                result = false;
                break;
            }

            byte cat = genCats[i];

            doMirrCat = false;
            doPlusCat = false;

            if (MMLCategory.isGenericCategoryEnabled(cat, mGenCatMask)) {
                doMirrCat = true;
            }

            if (MMLCategory.isGenericCategoryEnabled(cat, mGenPlusCatMask)) {
                doPlusCat = true;
            }

            if (doMirrCat | doPlusCat) {
                mSesd.beginGenCategory(cat);

                if (doMirrCat) {
                    mSesd.beginMirror();
                    result = mDatdDb.scanForCategoryWithStatus(cat, Storage.ITEM_MODIFIED);
                    mSesd.endMirror();
                }

                // Lol... long time after, this is a revelation - we need to send deleted tag regardless of mirror plus :)
                mSesd.beginDeleted();
                result = mDatdDb.scanForCategoryWithStatus(cat, Storage.ITEM_DELETED);
                mSesd.endDeleted();

                mSesd.endGenCategory(cat);
            }

            // result will be false on genuine errors or an abort request
            if (!result) {
                break;
            }
        }

        mSesd.endGenericData();
        mSesd.end();

        MeemEvent evt = new MeemEvent(result ? EventCode.BACKUP_PREPARATION_SUCCEEDED : EventCode.BACKUP_PREPARATION_FAILED);
        mUiCtxt.postEvent(evt);
    }

    @Override
    public boolean onDatabaseItemRetrieved(MMLGenericDataDesc desc) {
        if (mAbortRequested) {
            return false;
        }
        // final processing. one generic data item of a category is retrieved
        // from db with required status
        dbgTrace("Adding to SESD: " + desc);

        // XXX: SHOCKING HACK: if file is on external SDCARD, prepend the path
        // with 'S' else prepend with 'I'
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
        // we should ignore deleted items  while calculating session size.
        if (desc.mStatus != Storage.ITEM_DELETED) {
            mSession.updateTotalSessionDataSize(dataSize, desc.onSdCard ? false : true);
        }

        return true;
    }

    @Override
    public void onDatabaseScanCompleted(boolean result) {
        // final processing. all generic data items of a category is retrieved
        // from database with required status
        if (!result) {
            MeemEvent evt = new MeemEvent(EventCode.BACKUP_PREPARATION_FAILED);
            mUiCtxt.postEvent(evt);
        }
    }

    // Arun: 10Aug2016: added for app v2.
    private boolean addToThumbnailDb(MMLGenericDataDesc desc, boolean forceAck) {
        dbgTrace();

        // add it to db if it is not already there
        if (mThumbDb.isMirrorThumbnailPresent(desc)) {
            dbgTrace("Thumbnail already present for: " + desc);

            if (forceAck) {
                // Added by Arun on 30May2017 for the sdcard category thumbnails related bug in versions prior to 1.0.45
                mThumbDb.forceAckForEntry(desc);
            }

            return true;
        }

        dbgTrace("Creating and storing thumbnail for: " + desc);
        byte[] thumbNail = getGenDataThumbnail(desc);
        if (null == thumbNail) {
            dbgTrace("WARNING: Thumbnail creation failed!");
        }

        return mThumbDb.addMirrorThumbNail(desc, thumbNail, forceAck);
    }

    private byte[] getGenDataThumbnail(MMLGenericDataDesc desc) {
        if (desc.mCatCode == MMPConstants.MMP_CATCODE_PHOTO || desc.mCatCode == MMPConstants.MMP_CATCODE_PHOTO_CAM) {
            return GenUtils.getImageThumbnailCustomMethod(desc.mPathForThumb);
        }

        if (desc.mCatCode == MMPConstants.MMP_CATCODE_VIDEO || desc.mCatCode == MMPConstants.MMP_CATCODE_VIDEO_CAM) {
            return GenUtils.getVideoThumbnailSystemMethod(desc.mPathForThumb);
        } else {
            return null;
        }
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("Backup.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("Backup.log");
    }
}
