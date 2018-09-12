package com.meem.cablemodel;

/**
 * @author Arun T A
 *         <p/>
 *         MSTAT gives data size in MB, VSTAT GenericData in KB and SmartData in bytes! To make life of UI developer easier, all size getter
 *         functions of back-end classes will return values in KiloBytes.
 */

public class MeemUsage {
    long mTotalStorage;
    long mUsedStorage;
    long mFreeStorage;
    long mRsvdStorage;

    public long getTotalStorage() {
        return mTotalStorage * 1024;
    }

    public long getUsedStorage() {
        return mUsedStorage * 1024;
    }

    public long getFreeStorage() {
        return mFreeStorage * 1024;
    }

    public long getRsvdStorage() {
        return mRsvdStorage * 1024;
    }
}
