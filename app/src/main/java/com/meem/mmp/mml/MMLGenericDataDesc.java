package com.meem.mmp.mml;

/**
 * This class represents generic data as described in DATD and SESD. This class is used during DATD parsing and and SESD creation.
 *
 * @author Arun T A
 */
public class MMLGenericDataDesc {
    public byte mCatCode = 0;
    public String mPath = "";
    public String mPathForThumb = "";
    public long mSize = 0;
    public long mModTime;
    public String mCSum = "";
    public int mStatus = 0;
    public boolean onSdCard = false;

    /**
     * 24Nov2015: Arun: For dealing with media store query results which gives paths that are neither on external primary or secondary
     * storage paths. E.g. Huawie Honor returns /system/pre_installed/ringtone.mp3
     */
    public boolean mQuirkyDriod = false;
    /**
     * === For V2: 15April2017
     */
    public boolean mIsMirror;
    public String mMeemInternalPath;
    public boolean mIsDeleted;
    public byte[] mThumbNail;
    public long mRowId;
    public boolean mFwAck;
    /**
     * 16Jun2017: Arun: very special usage: for driverV2 to skip path conversion logic.
     */
    public boolean mIsTemp;

    // for debugging.
    public String toString() {
        String str = "";
        str += mPath + ": size: " + String.valueOf(mSize) + ", mtime: " + String.valueOf(mModTime) + ", csum: " + mCSum + ", sdcard: " + String.valueOf(onSdCard) + ", rowId: " + mRowId;
        return str;
    }
}
