package com.meem.cablemodel;

import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataSettings;
import com.meem.mmp.mml.MMLSmartDataSettings;

public class SyncSettings {
    String mUpid;
    MMLSmartDataSettings mSmartDataSettings;
    MMLGenericDataSettings mGenDataSettings;

    public SyncSettings() {
        // must be set later in logic paths.
        mUpid = "debugHere";

        mSmartDataSettings = new MMLSmartDataSettings(false);
        mGenDataSettings = new MMLGenericDataSettings(false);
    }

    public String getUpid() {
        return mUpid;
    }

    public void setUpid(String upid) {
        mUpid = upid;
    }

    public boolean getSmartCatStatus(byte cat) {
        int mask = mSmartDataSettings.getCategoryMask();
        return MMLCategory.isSmartCategoryEnabled(cat, mask);
    }

    public void setSmartCatStatus(byte cat, boolean enabled) {
        int mask = mSmartDataSettings.getCategoryMask();
        mask = MMLCategory.updateSmartCatMask(mask, cat, enabled);
        mSmartDataSettings.setCategoryMask(mask);
    }

    public boolean getGenCatStatus(byte cat) {
        int mask = mGenDataSettings.getCategoryMask();
        return MMLCategory.isGenericCategoryEnabled(cat, mask);
    }

    public void setGenericCatStatus(byte cat, boolean enabled) {
        int mask = mGenDataSettings.getCategoryMask();
        mask = MMLCategory.updateGenCatMask(mask, cat, enabled);
        mGenDataSettings.setCategoryMask(mask);
    }

    public int getSmartCatMask() {
        return mSmartDataSettings.getCategoryMask();
    }

    public int getGenCatMask() {
        return mGenDataSettings.getCategoryMask();
    }
};
