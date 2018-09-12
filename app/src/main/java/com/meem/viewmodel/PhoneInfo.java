package com.meem.viewmodel;

/**
 * Created by arun on 17/8/16.
 *
 * Always think about V1 and V2 whenever changes are made to ViewModel classes. Else bugs are for sure.
 */
public class PhoneInfo {
    public String mUpid = "TESTPHONE";
    public String mBaseModel;

    /**
     * Careful here. 1. This member can be null when app starts 2. When no cable is connected, accessing this member illegal.
     */
    public VaultInfo mVaultInfo;

    public String getmUpid() {
        return mUpid;
    }

    public void setmUpid(String mUpid) {
        this.mUpid = mUpid;
    }

    public String getmBaseModel() {
        return mBaseModel;
    }

    public void setmBaseModel(String mBaseModel) {
        this.mBaseModel = mBaseModel;
    }

    public VaultInfo getVaultInfo() {
        return mVaultInfo;
    }

    public void setVaultInfo(VaultInfo vaultInfo) {
        mVaultInfo = vaultInfo;
    }
}
