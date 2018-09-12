package com.meem.phone;

import com.meem.mmp.mml.MMLGenericDataDesc;

public interface StorageScanListener {
    boolean onGenericDataStorageItem(MMLGenericDataDesc genDataDesc);
    void onStorageScanCompletion(boolean result);
    void onStorageScanCompletionForCat(byte cat, boolean result);
}
