package com.meem.v2.cablemodel;

/**
 * Created by SCS on 3/16/2017.
 */

public class VaultDbModel {
    public String mUpid;
    public int mIs_migration;
    public String mName;
    public int mBackup_time;
    public int mBackup_status;
    public int mRestore_time;
    public int mRestore_status;
    public int mCopy_time;
    public int mCopy_status;
    public int mSync_time;
    public int mSync_status;
    public int mBackup_mode;
    public int mSound;
    public int mAuto;
    public int mRsvd1;
    public int mRsvd2;
    public String mRsvd3;

    @Override
    public boolean equals(Object other) {
        boolean eq = false;

        if (other != null && other instanceof VaultDbModel) {
            eq = this.mUpid.equals(((VaultDbModel) other).mUpid);
        }

        return eq;
    }
}
