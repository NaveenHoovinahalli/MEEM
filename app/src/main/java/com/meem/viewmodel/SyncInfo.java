package com.meem.viewmodel;

import java.util.List;

/**
 * Created by arun on 5/8/16.
 *
 * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
 */
public class SyncInfo {
    public String mSyncUpid;
    public List<Byte> mSyncedCats;

    public String getmSyncUpid() {
        return mSyncUpid;
    }

    public void setmSyncUpid(String mSyncUpid) {
        this.mSyncUpid = mSyncUpid;
    }

    public List<Byte> getmSyncedCats() {
        return mSyncedCats;
    }

    public void setmSyncedCats(List<Byte> mSyncedCats) {
        this.mSyncedCats = mSyncedCats;
    }
}
