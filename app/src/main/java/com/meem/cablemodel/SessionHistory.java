package com.meem.cablemodel;

public class SessionHistory {
    long mLastBackupTime, mLastRestoreTime, mLastCopyTime, mLastSyncTime;
    boolean mLastBackupComplete, mLastRestoreComplete, mLastCopyComplete, mLastSyncComplete;

    String mCopyUpid, mSyncUpid;

    public long getLastBackupTime() {
        return mLastBackupTime;
    }

    public boolean getLastBackupState() {
        return mLastBackupComplete;
    }

    // -------

    public long getLastRestoreTime() {
        return mLastRestoreTime;
    }

    public boolean getLastRestoreState() {
        return mLastRestoreComplete;
    }

    // ----------

    public long getLastCopyTime() {
        return mLastCopyTime;
    }

    public boolean getLastCopyState() {
        return mLastCopyComplete;
    }

    public String getCopyUpid() {
        return mCopyUpid;
    }

    // ----------

    public long getLastSyncTime() {
        return mLastSyncTime;
    }

    public boolean getLastSyncState() {
        return mLastSyncComplete;
    }

    public String getSyncUpid() {
        return mSyncUpid;
    }
}
