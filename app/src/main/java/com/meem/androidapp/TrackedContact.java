package com.meem.androidapp;

public class TrackedContact {

    String mVersionCode;
    String mStatus;
    String mId;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(String versioncode) {
        mVersionCode = versioncode;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

}
