package com.meem.viewmodel;

import com.meem.mmp.mml.MMLCategory;

/**
 * Created by arun on 5/8/16.
 */
public class CategoryInfo {
    /**
     * Conversion of this code (defined in MMP spec) to corresponding name is to be done by Views as it is language dependent. A simple
     * utility class can be made for this.
     *
     * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
     */
    public byte mMmpCode;

    public String mName;
    public BackupMode mBackupMode = BackupMode.DISABLED;
    public long mMirrorSizeKB;
    public long mPlusSizeKB;

    // === For V2 ===
    public String mSmartMirrorMeemPath, mSmartPlusMeemPath;
    public String mSmartMirrorCSum, mSmartPlusCSum;
    public boolean mSmartMirrorAck, mSmartPlusAck;

    public boolean ismDummy() {
        return mIsDummy;
    }

    public void setmDummy(boolean dummy) {
        mIsDummy = dummy;
    }

    // to display unsupported categories in an iOS vault.
    public boolean mIsDummy;

    public byte getmMmpCode() {
        return mMmpCode;
    }

    public void setmMmpCode(byte mMmpCode) {
        this.mMmpCode = mMmpCode;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public BackupMode getmBackupMode() {
        return mBackupMode;
    }

    public void setmBackupMode(BackupMode mBackupMode) {
        this.mBackupMode = mBackupMode;
    }

    public long getmMirrorSizeMB() {
        return mMirrorSizeKB;
    }

    public void setmMirrorSizeMB(long mMirrorSizeMB) {
        this.mMirrorSizeKB = mMirrorSizeMB;
    }

    public long getmPlusSizeMB() {
        return mPlusSizeKB;
    }

    public void setmPlusSizeMB(long mPlusSizeMB) {
        this.mPlusSizeKB = mPlusSizeMB;
    }

    public String toString() {
        String catName;

        if (MMLCategory.isGenericCategoryCode(mMmpCode)) {
            catName = MMLCategory.toGenericCatString(mMmpCode);
        } else {
            catName = MMLCategory.toSmartCatString(mMmpCode);
        }

        return (catName + " < " + mBackupMode + " >" + " MKb:" + mMirrorSizeKB + ", PKb: " + mPlusSizeKB);
    }
    public enum BackupMode {
        DISABLED, MIRROR, PLUS;
    }
}
