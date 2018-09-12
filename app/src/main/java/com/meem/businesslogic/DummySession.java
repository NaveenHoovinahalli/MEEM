package com.meem.businesslogic;

import com.meem.androidapp.ProductSpecs;
import com.meem.phone.Storage;

import meem.org.apache.commons.lang3.RandomUtils;

import static com.meem.businesslogic.DummySession.DummySessionType.DUMMY_NONE;

/**
 * Created by arun on 4/5/17.
 */

public class DummySession {
    boolean mLive;
    Storage mStorage;
    long startTimeSec, estimatedTimeSecs, remainingSecs;
    DummySessionType mDummySessionType = DUMMY_NONE;

    public DummySession(DummySessionType type) {
        mDummySessionType = type;

        mStorage = new Storage();
        long usedSpaceKB = mStorage.getTotalStorageCapacity() - mStorage.getTotalFreeSpace();
        estimatedTimeSecs = (long) (usedSpaceKB / ProductSpecs.NOMINAL_DATA_TRANSFER_SPEED_KBPS);

        if (estimatedTimeSecs > 1200 || estimatedTimeSecs < 120) {
            estimatedTimeSecs = 1000 + RandomUtils.nextLong(10, 200);
        }
    }

    public void start() {
        mLive = true;
        startTimeSec = System.currentTimeMillis() / 1000;
    }

    public void stop() {
        mLive = false;
        remainingSecs = 0;
    }

    public boolean isSessionLive() {
        return mLive;
    }

    public long getEstimatedTimeSecs() {
        long nowSec = System.currentTimeMillis() / 1000;
        long duration = nowSec - startTimeSec;

        remainingSecs = estimatedTimeSecs - duration;

        if (!mLive) {
            remainingSecs = -1;
        }

        return remainingSecs;
    }

    public DummySessionType getDummySessionType() {
        return mDummySessionType;
    }

    public enum DummySessionType {DUMMY_NONE, DUMMY_BACKUP, DUMMY_RESTORE, DUMMY_COPY}
}
