package com.meem.cablemodel;

import com.meem.androidapp.AppLocalData;

import java.io.File;

/**
 * This class represents a vault in MEEM cable.
 *
 * @author Arun T A
 */

public class MeemVault {
    boolean ghost = true; // initially, until VSTAT is read, all vaults are
    // ghosts
    boolean dummy = false; // this is for special purpose of gui. see
    // MeemFragment.java
    VaultConfig mConfig;
    private String mUpid;
    private String mVstatPath;
    // TODO: Tarun: Must remove as this is a view property and not a part of
    // data model of vault.
    // There is already a map in MeemFragment for this.
    private int vaultID;
    private boolean bRegInProgress = false;
    private boolean settingVault = false;
    private boolean migrationVault = false;

    public MeemVault() {
        mConfig = new VaultConfig();
    }

    public VaultConfig getmConfig() {
        return mConfig;
    }

    public boolean isMigrationVault() {
        return migrationVault;
    }

    public void setMigrationVault(boolean migrationVault) {
        this.migrationVault = migrationVault;
    }

    public boolean isSettingVault() {
        return settingVault;
    }

    public void setSettingVault(boolean settingVault) {
        this.settingVault = settingVault;
        setDummy(false);
        setGhost(false);
        setVaultID(1);
    }

    public int getVaultID() {
        return vaultID;
    }

    public void setVaultID(int vaultID) {
        this.vaultID = vaultID;
    }

    public final String getName() {
        return mConfig.mName;
    }

    public void setName(String name) {
        mConfig.mName = name;
    }

    public final String getUpid() {
        return mUpid;
    }

    public void setUpid(String upid) {
        mUpid = upid;
        String root = AppLocalData.getInstance().getRootFolderPath();
        mVstatPath = root + File.separator + "vstat-" + mUpid + ".mml";
    }

    public boolean update() {
        // This is the key thing done during baptizing.
        ghost = false;
        return mConfig.parseVstat(mVstatPath);
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public boolean isRegInProgress() {
        return bRegInProgress;
    }

    public void setRegInProgress(boolean flag) {
        bRegInProgress = flag;
    }

    public boolean isDummy() {
        return dummy;
    }

    public void setDummy(boolean dummy) {
        this.ghost = false;
        this.dummy = dummy;
        //		this.mUpid = "DUMMY";
    }

    public String getVstatPath() {
        return mVstatPath;
    }

    public boolean createVcfg(String pathVcfg) {
        return mConfig.createVcfg(pathVcfg);
    }

    public int getSmartDataCategoryMask() {
        return mConfig.mSmartMirrorSettings.getCategoryMask();
    }

    public void setSmartDataCategoryMask(int mask) {
        mConfig.mSmartMirrorSettings.setCategoryMask(mask);
    }

    public int getGenericDataCategoryMask() {
        return mConfig.mGenericMirrorSettings.getCategoryMask();
    }

    public void setGenericDataCategoryMask(int mask) {
        mConfig.mGenericMirrorSettings.setCategoryMask(mask);
    }

    public int getSmartPlusDataCategoryMask() {
        return mConfig.mSmartMirrorPlusSettings.getCategoryMask();
    }

    public void setSmartPlusDataCategoryMask(int mask) {
        mConfig.mSmartMirrorPlusSettings.setCategoryMask(mask);
    }

    public int getGenericPlusDataCategoryMask() {
        return mConfig.mGenericMirrorPlusSettings.getCategoryMask();
    }

    public void setGenericPlusDataCategoryMask(int mask) {
        mConfig.mGenericMirrorPlusSettings.setCategoryMask(mask);
    }

    public final VaultUsage getUsageInfo() {
        return mConfig.getUsageInfo();
    }

    public final SessionHistory getHistory() {
        return mConfig.getHistory();
    }

    public SyncSettings getSyncInfo() {
        return mConfig.getSyncInfo();
    }

    public void setSyncInfo(SyncSettings syncInfo) {
        if (syncInfo == null) {
            mConfig.mIsSynced = false;
            mConfig.mSyncInfo = null;
            mConfig.mIsSound = false; // Hack for FW: 02Feb 2015 see VaultConfig also.
        } else {
            mConfig.mIsSynced = true;
            mConfig.mSyncInfo = syncInfo;
            mConfig.mIsSound = true; // Hack for FW: 02Feb 2015 see VaultConfig also.
        }
    }

    public boolean isSyncEnabled() {
        return (mConfig.mIsSynced || mConfig.mIsSound); // Hack for FW: 02Feb 2015 see VaultConfig also.
    }

    public String getPlatform() {
        return mConfig.mPlatform;
    }

    public String getBrand() {
        return mConfig.mBrand;
    }

    public void setBrand(String brnd) {
        mConfig.mBrand = brnd;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof MeemVault)) {
            return false;
        }

        MeemVault vault = (MeemVault) obj;

        if (this == vault) {
            return true;
        }

        if (vault.getUpid() == null) {
            return false;
        }

        if (vault.getUpid().equalsIgnoreCase(this.getUpid())) {
            return true;
        }

        return false;
    }

    // FindBugs fix on 07July2015
    @Override
    public int hashCode() {
        if (mUpid == null || mUpid.isEmpty()) {
            return -1;
        }

        return mUpid.hashCode();
    }
}
