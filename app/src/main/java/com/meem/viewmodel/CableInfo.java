package com.meem.viewmodel;

import java.util.LinkedHashMap;

/**
 * CableInfo is the object that seperate the real model maintained by the CableDriver and the cable information used for presentation in
 * views by CablePresenter. Thus this class is essentially used to seperate the model and the views. Created by arun on 5/8/16.
 * <p>
 * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
 *
 * @author Arun T A
 */
public class CableInfo {
    public String mName = "MEEM";
    public long mCapacityKB;
    public long mFreeSpaceKB;
    public String fwVersion = "";

    public boolean mIsVirgin;
    public String mPin;

    public int mNumVaults;
    public LinkedHashMap<String, VaultInfo> mVaultInfoMap = new LinkedHashMap<>();
    // === For V2 ===
    public String mSerialNo = "MEEM-V1";

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public long getmCapacityKB() {
        return mCapacityKB;
    }

    public void setmCapacityKB(long mCapacityKB) {
        this.mCapacityKB = mCapacityKB;
    }

    public long getmFreeSpaceKB() {
        return mFreeSpaceKB;
    }

    public void setmFreeSpaceKB(long mFreeSpaceMB) {
        this.mFreeSpaceKB = mFreeSpaceMB;
    }

    public String getFwVersion() {
        return fwVersion;
    }

    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }

    public boolean ismIsVirgin() {
        return mIsVirgin;
    }

    public void setmIsVirgin(boolean mIsVirgin) {
        this.mIsVirgin = mIsVirgin;
    }

    public String getmPin() {
        return mPin;
    }

    public void setmPin(String mPin) {
        this.mPin = mPin;
    }

    public int getmNumVaults() {
        return mNumVaults;
    }

    public void setmNumVaults(int mNumVaults) {
        this.mNumVaults = mNumVaults;
    }

    public LinkedHashMap<String, VaultInfo> getmVaultInfoMap() {
        return mVaultInfoMap;
    }

    public void setmVaultInfoMap(LinkedHashMap<String, VaultInfo> mVaultInfoMap) {
        this.mVaultInfoMap = mVaultInfoMap;
    }

    public VaultInfo getVaultInfo(String upid) {
        return mVaultInfoMap.get(upid);
    }

    public String getSerialNo() {
        return mSerialNo;
    }

    public void setSerialNo(String serialNo) {
        mSerialNo = serialNo;
    }
}
