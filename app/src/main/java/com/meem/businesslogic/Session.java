package com.meem.businesslogic;

import android.annotation.SuppressLint;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ContactTrackerWrapper;
import com.meem.androidapp.ProductSpecs;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.mml.MMLCategory;
import com.meem.phone.MediaStorage;
import com.meem.phone.SmartDataStats;
import com.meem.utils.GenUtils;
import com.meem.utils.MovingAverage;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;
import java.util.HashMap;

import meem.org.apache.commons.lang3.RandomUtils;

/**
 * The session information - a key class in managing/tracking the session.
 *
 * @author Arun T A
 */

public class Session {
    public boolean mDropSessionOnUserOpt = false;

    protected boolean mPendingAbort = false;
    protected boolean mDataXfrStarted = false;
    protected boolean mSyncing = false; // whether the current copy session is part of a sync operation.
    protected boolean mSessionPreperationCompleted = false;
    protected boolean mSessionPreperationSucceeded = false;

    protected byte mHandle;
    protected int mType;

    protected VaultInfo mVaultInfo;
    protected ContactTrackerWrapper mContactTrackingWrapper;

    protected double mInitialEstimatedTime = 0;
    protected int mSmartDataRetryCount = 0;

    protected SessionSmartDataInfo mSmartDataInfo;
    protected SessionGenericDataInfo mGenericDataInfo;

    protected long mFreeSpaceInCable;

    protected long mTotalPhoneImageSize, mTotalPhoneMusicSize, mTotalPhoneVideoSize, mTotalPhoneDocsSize;
    protected long mTotalPhoneGenDataSize;

    protected long mTotalSessionDataSize, mSessionInternalStorageDataSize, mSessionExternalStorageDataSize;
    protected long mEstimatedTotalSessionDataSize, mEstimatedSessionInternalStorageDataSize, mEstimatedSessionExternalStorageDataSize;
    protected long mCompletedSessionDataSize;

    protected SessionCatFilter mCatFilter;

    protected AppLocalData mAppData = AppLocalData.getInstance();

    protected double mAvgSpeed = 0;
    protected boolean mEnableStats = false;
    protected Thread mStatCalcThread;
    protected MediaStorage mMediaStorage;
    protected SmartDataStats mSmartDataStats;
    protected long firstInitialEstimateSysTimeMillis = 0;
    protected long mSessionRunningTimeSecs = 0;
    protected boolean mIsFirstBackup = false;
    protected boolean mEarlyDetectedSpaceShortage = false;

    protected SessionStatUpdateHelper mStatHelper;
    protected Status mGlobalStatus = Status.SESSION_INIT;

    public Session(int mmpSessionType, SessionStatUpdateHelper statHelper) {
        initialize();

        mType = mmpSessionType;
        mStatHelper = statHelper;
    }

    public void initialize() {
        mGlobalStatus = Status.SESSION_INIT;
        mPendingAbort = false;
        mDropSessionOnUserOpt = false;
        mTotalSessionDataSize = 0;
        mCompletedSessionDataSize = 0;
        mSessionInternalStorageDataSize = 0;
        mSessionExternalStorageDataSize = 0;

        mMediaStorage = new MediaStorage(null);
        mSmartDataStats = new SmartDataStats();
    }

    public int getSmartDataRetryCount() {
        return mSmartDataRetryCount;
    }

    public void incSmartDataRetryCount() {
        mSmartDataRetryCount++;
    }

    public ContactTrackerWrapper getContactTrackingWrapper() {
        return mContactTrackingWrapper;
    }

    public void setContactTrackingWrapper(ContactTrackerWrapper mContactTrackingWrapper) {
        this.mContactTrackingWrapper = mContactTrackingWrapper;
    }

    /**
     * In KB
     *
     * @return
     */
    public long getTotalSessionDataSize() {
        if (mSessionPreperationCompleted && mSessionPreperationSucceeded) {
            return mTotalSessionDataSize;
        } else {
            return mEstimatedTotalSessionDataSize;
        }
    }

    /**
     * In KB
     *
     * @return
     */
    public void updateTotalSessionDataSize(long size, boolean isInternalStorage) {
        if (isInternalStorage) {
            mSessionInternalStorageDataSize += size;
        } else {
            mSessionExternalStorageDataSize += size;
        }

        mTotalSessionDataSize = mSessionInternalStorageDataSize + mSessionExternalStorageDataSize;
    }

    /**
     * In KB - not to be used by any class outside package
     *
     * @return
     */
    public void updateEstimatedTotalSessionDataSize(long size, boolean isInternalStorage) {
        if (mIsFirstBackup) {
            // we have done estimates already - smart :)
            return;
        }

        if (isInternalStorage) {
            mEstimatedSessionInternalStorageDataSize += size;
        } else {
            mEstimatedSessionExternalStorageDataSize += size;
        }

        mEstimatedTotalSessionDataSize = mEstimatedSessionInternalStorageDataSize + mEstimatedSessionExternalStorageDataSize;
    }

    /**
     * In KB
     *
     * @return
     */
    public long getSessionExternalStorageDataSize() {
        return mSessionExternalStorageDataSize;
    }

    /**
     * In KB
     *
     * @return
     */
    public long getSessionInternalStorageDataSize() {
        return mSessionInternalStorageDataSize;
    }

    /**
     * In KB
     *
     * @return
     */
    public long getCompletedSessionDataSize() {
        return mCompletedSessionDataSize;
    }

    /**
     * In KB
     *
     * @return
     */
    /*public void setCompletedSessionDataSize(long completedSessionDataSize) {
        mCompletedSessionDataSize = completedSessionDataSize;
    }*/
    public boolean getDataXfrStatus() {
        return mDataXfrStarted;
    }

    public void setDataXfrStatus(boolean boo) {
        mDataXfrStarted = boo;
        mGlobalStatus = mDataXfrStarted ? Status.SESSION_XFR_STARTED : Status.SESSION_XFR_STOPPED;
    }

    public boolean isAbortPending() {
        return mPendingAbort;
    }

    public void setAbortPending(boolean boo) {
        mPendingAbort = boo;
        mGlobalStatus = Status.SESSION_ABORT_PENDING;
    }

    public int getType() {
        return mType;
    }

    public byte getHandle() {
        return mHandle;
    }

    public void setHandle(byte handle) {
        this.mHandle = handle;
        mGlobalStatus = Status.SESSION_STARTED;
    }

    public VaultInfo getVaultInfo() {
        return mVaultInfo;
    }

    public void setSessionParams(long freeSpaceInCable, VaultInfo vaultInfo, SessionCatFilter filter) {
        mFreeSpaceInCable = freeSpaceInCable;
        mVaultInfo = vaultInfo;
        mCatFilter = filter;

        if (0 == mVaultInfo.getTotalMirrorDataUsage() && 0 == mVaultInfo.getTotalPlusDataUsage()) {
            dbgTrace("Voila... empty vault OR first backup!");
            mIsFirstBackup = true;
        }

        mInitialEstimatedTime = getInitialEstimatedTime();
        if (mIsFirstBackup) {
            mEstimatedTotalSessionDataSize = mTotalPhoneGenDataSize;

            if (mEstimatedTotalSessionDataSize >= mFreeSpaceInCable) {
                dbgTrace("Oh well... phone already has more data: " + mEstimatedTotalSessionDataSize + " kB. Cable can store only: " + mFreeSpaceInCable + " kB");
                mEarlyDetectedSpaceShortage = true;
            }
        }
    }

    public boolean isFirstBackupPossible() {
        return !mEarlyDetectedSpaceShortage;
    }

    public Status getGlobalStatus() {
        return mGlobalStatus;
    }

    public void setGlobalStatus(Status status) {
        mGlobalStatus = status;
    }

    /**
     * To cleanup all files, including database, smart data json files from previous session (if any).
     *
     * @return
     */
    public boolean cleanupPreviousSmartDataFiles() {
        dbgTrace();

        boolean ret = true;

        if (null == mSmartDataInfo) {
            // TODO: check this logic.
            dbgTrace("WARNING: must have called prepare smart data first");
            return false;
        }

        for (Byte cat : mSmartDataInfo.mCatsFilesMap.keySet()) {
            ArrayList<MMPFPath> fPaths = mSmartDataInfo.mCatsFilesMap.get(cat);
            for (MMPFPath fpath : fPaths) {
                if (null != fpath.toString()) { // it can be null
                    ret &= mAppData.truncate(fpath.toString());
                }
            }
        }

        return ret;
    }

    public SessionSmartDataInfo getSmartDataInfo() {
        return mSmartDataInfo;
    }

    public SessionGenericDataInfo getGenericDataInfo() {
        return mGenericDataInfo;
    }

    public boolean prepareGenericDataInfoForBackup() {
        dbgTrace();

        int genCatMask = mVaultInfo.getGenericDataCategoryMask();
        int genPlusCatMask = mVaultInfo.getGenericPlusDataCategoryMask();

        ArrayList<Byte> genCats = MMLCategory.getGenericCatCodesArray(genCatMask);
        ArrayList<Byte> genPlusCats = MMLCategory.getGenericCatCodesArray(genPlusCatMask);

        dbgTrace("genCats: " + genCats);
        dbgTrace("genPlusCats: " + genPlusCats);

        if (null != mCatFilter) {
            dbgTrace("Applying individual generic category filters");
            mCatFilter.applyGenCatFilter(genCats);
            mCatFilter.applyGenPlusCatFilter(genPlusCats);

            genCatMask = 0;
            genPlusCatMask = 0;

            for (Byte cat : genCats) {
                genCatMask = MMLCategory.updateGenCatMask(genCatMask, cat, true);
            }

            for (Byte cat : genPlusCats) {
                genPlusCatMask = MMLCategory.updateGenCatMask(genPlusCatMask, cat, true);
            }

            // for all plus enabled categories, mirror is also enabled.
            genCatMask |= genPlusCatMask;

            dbgTrace("After filtering generic category mask: " + genCatMask);
        }

        mGenericDataInfo = new SessionGenericDataInfo(genCatMask, genPlusCatMask);
        return true;
    }

    /**
     * For restore and copy/sync operations, the category masks can not be taken from vault as the data need be put into phone can be
     * modified by user - e.g. avoiding sdcard items.
     * <p/>
     * Important: Filter applying and all is done already before calling this method
     * <p/>
     * Note that the filter application and sdcard items stuff are now in session manager.
     *
     * @param genCatMask
     * @param genPlusCatMask - always zero. (we do not consider plus data for restore/copy/sync)
     *
     * @return
     */
    public boolean prepareGenericDataInfoForRestoreOrCopy(int genCatMask, int genPlusCatMask) {
        mGenericDataInfo = new SessionGenericDataInfo(genCatMask, genPlusCatMask);
        return true;
    }

    @SuppressLint("UseSparseArrays")
    public boolean prepareSmartDataInfo() {
        dbgTrace();
        mSmartDataInfo = new SessionSmartDataInfo();

        int smartCatMask = mVaultInfo.getSmartDataCategoryMask();
        int smartPlusCatMask = mVaultInfo.getSmartPlusDataCategoryMask();

        ArrayList<Byte> smartCats = MMLCategory.getSmartCatCodesArray(smartCatMask);
        ArrayList<Byte> smartPlusCats = MMLCategory.getSmartCatCodesArray(smartPlusCatMask);

        dbgTrace("Smart mirror cats: " + smartCats);
        dbgTrace("Smart plus cats: " + smartPlusCats);

        // apply filter (for any session)
        if (null != mCatFilter) {
            dbgTrace("Applying individual smart category filters");
            mCatFilter.applySmartCatFilter(smartCats);
            mCatFilter.applySmartPlusCatFilter(smartPlusCats);

            // for backup only: if mirror plus is enabled, mirror must be enabled.
            if (mType == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
                for (Byte cat : smartPlusCats) {
                    if (!smartCats.contains(cat)) {
                        smartCats.add(cat);
                    }
                }
            }

            dbgTrace("After filtering, smart mirror cats: " + smartCats);
            dbgTrace("After filtering, smart plus cats: " + smartPlusCats);
        } else {
            dbgTrace("No filter to apply.");
        }

		/* for a whole vault restore/copy session, consider only those categories that has data. */
        if (mCatFilter == null && (mType == MMPConstants.MMP_CODE_CREATE_RESTORE_SESSION || mType == MMPConstants.MMP_CODE_CREATE_CPY_SESSION)) {
            smartCatMask = 0x07;

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_CONTACT)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_CONTACT, false);
            }

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_MESSAGE)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_MESSAGE, false);
            }

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_CALENDER)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_CALENDER, false);
            }

            smartCats = MMLCategory.getSmartCatCodesArray(smartCatMask);

            // Arun: 11-11-2016: This is needed to make sure that the complicated FPath processing logic below is proper.
            // Anyway, tyhere is ultimately no effect as we are ignoring plus items for restore/copy.
            smartPlusCatMask = smartCatMask;
            smartPlusCats = MMLCategory.getSmartCatCodesArray(smartPlusCatMask);
        }

        dbgTrace("Finalised smart data category codes for session: mirror: " + smartCats + "; plus (ignore for restore/copy): " + smartPlusCats);

        AppLocalData appData = AppLocalData.getInstance();
        HashMap<Byte, ArrayList<MMPFPath>> allSmartCatsFilesMap = new HashMap<Byte, ArrayList<MMPFPath>>();

        // Add FPATH for smart cats, adding null FPATH for smart plus, which
        // will be replaced by real FPATH if plus is enabled for the category
        for (Byte cat : smartCats) {
            ArrayList<MMPFPath> fpaths = new ArrayList<MMPFPath>();
            String spath = appData.getSmartCatFilePath(mVaultInfo.getUpid(), MMLCategory.toSmartCatString(cat.intValue()), true);

            MMPFPath sfpath = new MMPFPath(spath, 0);
            fpaths.add(sfpath);

            MMPFPath spfpath = new MMPFPath(null, 0);
            fpaths.add(spfpath);

            allSmartCatsFilesMap.put(cat, fpaths);
        }

        // add FPATH for smart plus, taking care of setting
        // FPATH null as necessary
        for (Byte cat : smartPlusCats) {
            ArrayList<MMPFPath> fpaths = allSmartCatsFilesMap.get(cat);

            if (null == fpaths) {
                // this means, there is no smart data enabled for this category.
                // add null FPATH for it first and then add FPATH for plus
                fpaths = new ArrayList<MMPFPath>();
                MMPFPath sfpath = new MMPFPath(null, 0);
                fpaths.add(sfpath);
            } else {
                // this means, smart is enabled for this category.
                // remove null FPATH previously set for this plus item
                // and add add a new fpath for this plus category
                fpaths.remove(1);

                String path = appData.getSmartCatFilePath(mVaultInfo.getUpid(), MMLCategory.toSmartCatString(cat.intValue()), false);
                MMPFPath spfpath = new MMPFPath(path, 0);
                fpaths.add(spfpath);
                allSmartCatsFilesMap.put(cat, fpaths);
            }
        }

        // merge filtered cat masks : TODO can be removed.
        ArrayList<Byte> allSmartCats = new ArrayList<Byte>();
        allSmartCats.addAll(smartCats);
        for (Byte cat : smartPlusCats) {
            if (!allSmartCats.contains(cat)) {
                allSmartCats.add(cat);
            }
        }

        dbgTrace("After preparing smart data category mask: " + allSmartCats);

        mSmartDataInfo.setCatCodes(allSmartCats);
        mSmartDataInfo.setCatsFilesMap(allSmartCatsFilesMap);

        return true;
    }

    public boolean isSyncing() {
        return mSyncing;
    }

    public void setSyncing(boolean mSyncOp) {
        this.mSyncing = mSyncOp;
    }

    // statistics thread function. it calculates avg
    // bandwidth in kilobytes per second using a moving average mechanism.
    private void calcAvgSpeedThreadFn() {
        dbgTrace();

        mSessionRunningTimeSecs = 0;

        MovingAverage movAvg = new MovingAverage(10);

        double t1 = 0.0;
        double t2 = 0.0;
        double avg = 0.0;
        mAvgSpeed = 0.0;

        mStatHelper.resetStats();

        while (mEnableStats) {
            t1 = mStatHelper.getStats();
            try {
                Thread.sleep(1000);
                mSessionRunningTimeSecs++;

                if (mGlobalStatus != Status.SESSION_XFR_STARTED) {
                    if (mSessionRunningTimeSecs > (mInitialEstimatedTime * 0.75)) {
                        // well... we are way off the mark in initial estimation. increment it by 5 minutes
                        dbgTrace("Stat thread: applying correction to estimate time: " + ProductSpecs.SESSION_ESTIMATION_TIME_CORRECTION_SECS);
                        mInitialEstimatedTime += ProductSpecs.SESSION_ESTIMATION_TIME_CORRECTION_SECS;
                    }
                }

            } catch (Exception ex) {
                dbgTrace("Exception in stat thread sleep: " + ex.getMessage());
            }
            t2 = mStatHelper.getStats();
            mCompletedSessionDataSize = (long) t2 / 1024; // KB

            movAvg.newNum(t2 - t1);
            avg = movAvg.getAvg(); // this is average per 1000 ms
            avg = avg / 1024;

            mAvgSpeed = avg;
        }

        dbgTrace("Session stat thread exiting.");
    }

    // ---------------------------------------------------------------------
    // thread to calculate throughput

    public void startStats() {
        dbgTrace();

        // start speed statistics thread
        mEnableStats = true;
        mStatCalcThread = new Thread(new Runnable() {
            @Override
            public void run() {
                calcAvgSpeedThreadFn();
            }
        });

        mStatCalcThread.start();
    }

    public void stopStats() {
        dbgTrace();

		/* this will flag the thread to stop */
        mEnableStats = false;

        if (mStatCalcThread != null) {
            if (mStatCalcThread.isAlive()) {
                try {
                    mStatCalcThread.join();
                } catch (InterruptedException ex) {
                    dbgTrace("Exception stopping stat thread: " + ex.getMessage());
                } finally {
                    mAvgSpeed = 0;
                }
            }
        }
    }

    /**
     * Function to return average speed
     *
     * @return speed in kilobytes per second
     */
    public double getAverageSpeed() {
        if (mEnableStats) {
            return mAvgSpeed;
        }

        return 0;
    }

    public double getInitialEstimatedTime() {
        dbgTrace();

        firstInitialEstimateSysTimeMillis = System.currentTimeMillis();

        /**
         * Take care of session filter for generic data. Ideally for smart data
         * also we need to do this - but we can not get the number of contacts,
         * messages etc in any reliable way immediately from UI thread. See
         * comments in SmartDataStats.java.
         */
        boolean considerPhotos = true;
        boolean considerMusic = true;
        boolean considerVideos = true;
        boolean considerDocs = true;

        // Beware of the nasty HACK for category codes for SDCARD items.
        if (mCatFilter != null) {
            // This is a category-wise session
            considerPhotos = mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_PHOTO) || mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_PHOTO_CAM);
            considerMusic = mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_MUSIC) || mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_FILE);
            considerVideos = mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_VIDEO) || mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_VIDEO_CAM);
            considerDocs = mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_DOCUMENTS) || mCatFilter.containsGenMirrCat(MMPConstants.MMP_CATCODE_DOCUMENTS_SD);
        } else {
            // this is a whole-meem session
            int genCatMask = mVaultInfo.getGenericDataCategoryMask();
            considerPhotos = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, genCatMask) || MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, genCatMask);
            considerMusic = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, genCatMask) || MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, genCatMask);
            considerVideos = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, genCatMask) || MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, genCatMask);
            considerDocs = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS, genCatMask) || MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS_SD, genCatMask);
        }

        // okay, first consider smart data stuff
        double contactsProcessingTimeEstSecs = 0;
        double messagesProcessingTimeEstSecs = 0;
        double calendarProcessingTimeEstSecs = 0;

        double smartDataProcessingTimeEstSecs = 0;


        long smartDataCount = 0;

        // if cable has some contact smart data, then, FOR BACKUP, we may ignore the contact smart data processing time
        // this can be a huge saving in time because of contact tracker service.
        long contactMirrorSize = mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_CONTACT);
        dbgTrace("Total contact mirror data stored in cable now for this phone: " + contactMirrorSize + " KB");

        // get current phone smart data info
        smartDataCount = mSmartDataStats.contactTotalCount();
        dbgTrace("Contacts count: " + smartDataCount);

        if (mType == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
            if (contactMirrorSize == 0) {
                dbgTrace("First backup .Considering worst case processing time for contacts");
                if (smartDataCount > ProductSpecs.MINIMUM_NUMBER_CONTACTS_FOR_STATS) {
                    contactsProcessingTimeEstSecs = smartDataCount * ProductSpecs.SINGE_CONTACT_PROCESSING_TIME_SEC;
                }
            } else {
                // TODO: ideally, we should check number of contacts in mirror file - but it is not that viable at present because
                // we are taking profile pictures also in contact mirror JSON - which can vary in size. Here I'm assuming that there
                // are no large changes in contacts during subsequent backups.
                dbgTrace("Subsequent backup. Reducing processing time for contacts (justified in real-life scenarios)");
                if (smartDataCount > ProductSpecs.MINIMUM_NUMBER_CONTACTS_FOR_STATS) {
                    contactsProcessingTimeEstSecs = smartDataCount * ProductSpecs.SUBSEQUENT_SINGE_CONTACT_PROCESSING_TIME_SEC;
                }
            }
        } else {
            if (smartDataCount > ProductSpecs.MINIMUM_NUMBER_CONTACTS_FOR_STATS) {
                contactsProcessingTimeEstSecs = smartDataCount * ProductSpecs.SINGE_CONTACT_PROCESSING_TIME_SEC;
            }
        }

        smartDataCount = mSmartDataStats.messageTotalCount();
        if (smartDataCount > ProductSpecs.MINIMUM_NUMBER_MESSAGES_FOR_STATS) {
            dbgTrace("SMS count: " + smartDataCount);
            messagesProcessingTimeEstSecs = smartDataCount * ProductSpecs.SINGE_MESSAGE_PROCESSING_TIME_SEC;
        }

        smartDataCount = mSmartDataStats.calenderTotalCount();
        if (smartDataCount > ProductSpecs.MINIMUM_NUMBER_CALENDAR_EVENTS_FOR_STATS) {
            dbgTrace("Calendar event count: " + smartDataCount);
            calendarProcessingTimeEstSecs = smartDataCount * ProductSpecs.SINGE_CALENDAR_EVENT_PROCESSING_TIME_SEC;
        }

        smartDataProcessingTimeEstSecs = contactsProcessingTimeEstSecs + messagesProcessingTimeEstSecs + calendarProcessingTimeEstSecs;
        dbgTrace("Total smart data processing estimated time: " + smartDataProcessingTimeEstSecs + " seconds");

        // get current phone media info.
        long totPhoneImageSizeKB = considerPhotos ? mMediaStorage.getTotalImageSizeKb() : 0;
        long totPhoneMusicSizeKB = considerMusic ? mMediaStorage.getTotalMusicSizeKb() : 0;
        long totPhoneVideoSizeKB = considerVideos ? mMediaStorage.getTotalVideoSizeKb() : 0;
        long totPhoneDocsSizeKB = considerDocs ? mMediaStorage.getTotalDocSizeKb() : 0;

        mTotalPhoneImageSize = totPhoneImageSizeKB;
        mTotalPhoneMusicSize = totPhoneMusicSizeKB;
        mTotalPhoneVideoSize = totPhoneVideoSizeKB;
        mTotalPhoneDocsSize = totPhoneDocsSizeKB;

        mTotalPhoneGenDataSize = totPhoneImageSizeKB + totPhoneMusicSizeKB + totPhoneVideoSizeKB + totPhoneDocsSizeKB;
        dbgTrace("For this session, in phone we have to consider: " + totPhoneImageSizeKB + " KB Photos, " + totPhoneMusicSizeKB + " KB Music, " + totPhoneVideoSizeKB + " KB Videos, " + totPhoneDocsSizeKB + " KB Docs." + "Total media size now: " + mTotalPhoneGenDataSize + " KB");

        // get current vault media usage into
        long totalGenDataInCableKB = 0;

        long totCableImageSizeKB = mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_PHOTO) + mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_PHOTO_CAM);
        long totCableMusicSizeKB = mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_MUSIC) + mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_FILE);
        long totCableVideoSizeKB = mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_VIDEO) + mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_VIDEO_CAM);
        long totCableDocsSizeKB = mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_DOCUMENTS) + mVaultInfo.getTotalGenCatDataUsage(MMPConstants.MMP_CATCODE_DOCUMENTS_SD);

        if (null != mCatFilter) {
            totCableImageSizeKB = considerPhotos ? totCableImageSizeKB : 0;
            totCableMusicSizeKB = considerMusic ? totCableMusicSizeKB : 0;
            totCableVideoSizeKB = considerVideos ? totCableVideoSizeKB : 0;
            totCableDocsSizeKB = considerDocs ? totCableDocsSizeKB : 0;
        }

        totalGenDataInCableKB = totCableImageSizeKB + totCableMusicSizeKB + totCableVideoSizeKB + totCableDocsSizeKB;
        dbgTrace("Total generic data stored in cable for this session for this phone: " + totalGenDataInCableKB + " KB");

        double sessionDataSizeKB = 0;
        double genDataProcessingTimeEstSecs = 0;

        if (mType == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
            if (mTotalPhoneGenDataSize >= totalGenDataInCableKB) {
                sessionDataSizeKB = mTotalPhoneGenDataSize - totalGenDataInCableKB;
                dbgTrace("We have approx: " + sessionDataSizeKB + " Kbytes of media data to backup");

                genDataProcessingTimeEstSecs = sessionDataSizeKB / ProductSpecs.NOMINAL_DATA_PROCESSING_SPEED_KBPS + sessionDataSizeKB / ProductSpecs.NOMINAL_DATA_TRANSFER_SPEED_KBPS;
            } else {
                // here, we will consider the scanning time only - not transfer time - because there is no data to backup.
                // Remember:this case is SESD creation stuff only.
                dbgTrace("Well, During backup, phone has less data than cable.");
                sessionDataSizeKB = totalGenDataInCableKB - mTotalPhoneGenDataSize;
                dbgTrace("We have approx: " + sessionDataSizeKB + " Kbytes of media data to process");

                genDataProcessingTimeEstSecs = sessionDataSizeKB / ProductSpecs.NOMINAL_DATA_PROCESSING_SPEED_KBPS;
            }
        } else {
            // Restore/copy initial time estimation is is more or less bogus.
            sessionDataSizeKB = Math.abs(mTotalPhoneGenDataSize - totalGenDataInCableKB);
            dbgTrace("We have approx: " + sessionDataSizeKB + " Kbytes of media data to process for restore/copy");

            genDataProcessingTimeEstSecs = sessionDataSizeKB / ProductSpecs.NOMINAL_DATA_PROCESSING_SPEED_KBPS + sessionDataSizeKB / ProductSpecs.NOMINAL_DATA_TRANSFER_SPEED_KBPS;
        }

        dbgTrace("Total generic data processing estimated time: " + genDataProcessingTimeEstSecs + " seconds");

        double totalEstTime = genDataProcessingTimeEstSecs + smartDataProcessingTimeEstSecs;
        dbgTrace("Total initial estimated time (secs) : " + totalEstTime);

        return totalEstTime;
    }

    /**
     * Function to get estimated time remaining for session completion in seconds. At no point in time, this function will return a negative
     * value as remaining time.
     *
     * @return remaining time in seconds. It will be negative if the user is jumping the guns to know the time too early. If it returns 0,
     * then thats the end.
     */
    public double getEstimatedTime() {
        dbgTrace();

        double remainingSeconds = 0;

        if (mGlobalStatus == Status.SESSION_ENDED) {
            dbgTrace("Sessoin ended. No more updates");
            return 0;
        }

        if (mGlobalStatus == Status.SESSION_XFR_STARTED) {
            double remainingKiloBytes = (mTotalSessionDataSize - mCompletedSessionDataSize);
            // important: avoid division by zero
            if (mAvgSpeed == 0) {
                mAvgSpeed = ProductSpecs.NOMINAL_DATA_TRANSFER_SPEED_KBPS;
            }
            remainingSeconds = (remainingKiloBytes) / mAvgSpeed;
            dbgTrace("XFR Started. Total session KB: " + mTotalSessionDataSize +
                    ", Completed session KB: " + mCompletedSessionDataSize +
                    ", Remaining KB: " + remainingKiloBytes +
                    ", Remaining seconds + " + remainingSeconds +
                    ", At speed (KBps): " + mAvgSpeed);
        } else {
            // XFR is not started yet. But initial estimation is done. adjust stuff with user's actions.
            long currentTimeMillis = System.currentTimeMillis();
            remainingSeconds = mInitialEstimatedTime - ((currentTimeMillis - firstInitialEstimateSysTimeMillis)) / 1000;
            dbgTrace("XFR not started. Giving updated estimated time: " + remainingSeconds + " seconds");

            // check whether we are getting way off the mark in initial estimation.
            if ((remainingSeconds < (mInitialEstimatedTime * 0.25)) || (mSessionRunningTimeSecs > (mInitialEstimatedTime * 0.75))) {
                // well... we are way off the mark in initial estimation. increment it by 5 minutes
                dbgTrace("Applying correction to estimate time: " + ProductSpecs.SESSION_ESTIMATION_TIME_CORRECTION_SECS);
                mInitialEstimatedTime += ProductSpecs.SESSION_ESTIMATION_TIME_CORRECTION_SECS;
            }
        }

		/* must consider some corner cases  - this will rarely happen in usual scenarios anyway */
        if (remainingSeconds < 1.0f && mGlobalStatus != Status.SESSION_ENDED) {
            dbgTrace("Supressing too small time estimation: " + remainingSeconds);
            remainingSeconds = ProductSpecs.SESSION_ESTIMATION_MINIMUM_DURATION_SECS + RandomUtils.nextLong(0, 60);
        }

        if (remainingSeconds >= ProductSpecs.SESSION_DURATION_ESTIMATION_LIMIT) {
            dbgTrace("Supressing too big time estimation: " + remainingSeconds);
            remainingSeconds = ProductSpecs.SESSION_ESTIMATION_MAXIMUM_DURATION_SECS + RandomUtils.nextLong(0, 3600);
        }

        return remainingSeconds;
    }

    public void setSessionPreparationCompleteWithStatus(boolean status) {
        mSessionPreperationCompleted = true;
        mSessionPreperationSucceeded = status;
    }

    public boolean getSessionPreperationCompletionStatus() {
        return mSessionPreperationCompleted;
    }

    // Following getters are added on 17-18 Feb2017
    public long getFreeSpaceInCable() {
        return mFreeSpaceInCable;
    }

    public long getTotalPhoneImageSize() {
        return mTotalPhoneImageSize;
    }

    public long getTotalPhoneMusicSize() {
        return mTotalPhoneMusicSize;
    }

    public long getTotalPhoneVideoSize() {
        return mTotalPhoneVideoSize;
    }

    public long getTotalPhoneDocsSize() {
        return mTotalPhoneDocsSize;
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat("Session", trace);
        GenUtils.logMessageToFile("Session.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("Session.log");
    }

    // ===================================================
    // ------------------ Debug support ------------------
    // ===================================================

    public enum Status {
        SESSION_INIT, SESSION_STARTED, SESSION_XFR_STARTED, SESSION_XFR_STOPPED, SESSION_ABORT_PENDING, SESSION_ENDED
    }

    public interface SessionStatUpdateHelper {
        void resetStats();
        double getStats();
    }
}
