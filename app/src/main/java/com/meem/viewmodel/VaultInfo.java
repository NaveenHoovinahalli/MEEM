package com.meem.viewmodel;

import java.util.LinkedHashMap;

/**
 * Created by arun on 5/8/16.
 *
 * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
 *
 */
public class VaultInfo {
    public String mName;
    public String mUpid;

    public String mPlatform = "Android"; // "Apple" or "Android"

    public boolean mIsMirror;

    public long mMirrorSizeKB;
    public long mPlusSizeKB;

    public LinkedHashMap<Byte, CategoryInfo> mCategoryInfoMap;
    public SyncInfo mSyncInfo;

    public long mLastBackupTime;
    public int mGenMirrorCatMask, mGenPlusCatMask;
    public int mSmartMirrorCatMask, mSmartPlusCatMask;

    public boolean ismIsMirror() {
        return mIsMirror;
    }

    public void setmIsMirror(boolean mIsMirror) {
        this.mIsMirror = mIsMirror;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmUpid() {
        return mUpid;
    }

    public void setmUpid(String mUpid) {
        this.mUpid = mUpid;
    }

    public String getUpid() {
        return mUpid;
    }

    public long getmMirrorSizeKB() {
        return mMirrorSizeKB;
    }

    public void setmMirrorSizeKB(long mMirrorSizeKB) {
        this.mMirrorSizeKB = mMirrorSizeKB;
    }

    public long getmPlusSizeKB() {
        return mPlusSizeKB;
    }

    public void setmPlusSizeKB(long mPlusSizeKB) {
        this.mPlusSizeKB = mPlusSizeKB;
    }

    public LinkedHashMap<Byte, CategoryInfo> getmCategoryInfoMap() {
        return mCategoryInfoMap;
    }

    public void setmCategoryInfoMap(LinkedHashMap<Byte, CategoryInfo> mCategoryInfoMap) {
        this.mCategoryInfoMap = mCategoryInfoMap;
    }

    public SyncInfo getmSyncInfo() {
        return mSyncInfo;
    }

    public void setmSyncInfo(SyncInfo mSyncInfo) {
        this.mSyncInfo = mSyncInfo;
    }

    // === For V2 ===

    public long getmLastBackupTime() {
        return mLastBackupTime;
    }

    public void setmLastBackupTime(long mLastBackupTime) {
        this.mLastBackupTime = mLastBackupTime;
    }

    public String toString() {
        return "Vault: " + mName + " upid: " + mUpid + " MKB: " + mMirrorSizeKB + " PKB: " + mPlusSizeKB +
                " GMM: " + Integer.toBinaryString(mGenMirrorCatMask) +
                " GPM: " + Integer.toBinaryString(mGenPlusCatMask) +
                " SMM: " + Integer.toBinaryString(mSmartMirrorCatMask) +
                " SPM: " + Integer.toBinaryString(mSmartPlusCatMask);
    }

    public int getGenericDataCategoryMask() {
        return mGenMirrorCatMask;
    }

    public int getGenericPlusDataCategoryMask() {
        return mGenPlusCatMask;
    }

    public int getSmartDataCategoryMask() {
        return mSmartMirrorCatMask;
    }

    public int getSmartPlusDataCategoryMask() {
        return mSmartPlusCatMask;
    }

    /**
     * In KB, for legacy reasons
     */
    public long getTotalGenCatDataUsage(byte genCat) {
        return getCatUsage(genCat);
    }

    /**
     * In KB, for legacy reasons
     */
    public long getTotalSmartCatDataUsage(byte smartCat) {
        return getCatUsage(smartCat);
    }

    /**
     * In KB, for legacy reasons
     */
    public long getGenDataMirrorOnlyUsage(byte genCat) {
        return getMirrorOnlyCatUsage(genCat);
    }

    /**
     * In KB, for legacy reasons
     */
    public long getSmartDataMirrorOnlyUsage(byte smartCat) {
        return getMirrorOnlyCatUsage(smartCat);
    }

    private long getCatUsage(byte cat) {
        long size = 0;

        CategoryInfo catInfo = mCategoryInfoMap.get(cat);
        if (catInfo != null) {
            size = catInfo.mMirrorSizeKB + catInfo.mPlusSizeKB;
        }

        return size; // KB for legacy reasons
    }

    private long getMirrorOnlyCatUsage(byte cat) {
        long size = 0;

        CategoryInfo catInfo = mCategoryInfoMap.get(cat);
        if (catInfo != null) {
            size = catInfo.mMirrorSizeKB;
        }

        return size; // KB for legacy reasons
    }

    /**
     * In KB, for legacy reasons
     *
     * @return
     */
    public long getTotalMirrorDataUsage() {
        return (mMirrorSizeKB);
    }


    /**
     * In KB, for legacy reasons
     *
     * @return
     */
    public long getTotalPlusDataUsage() {
        return (mMirrorSizeKB);
    }
}
