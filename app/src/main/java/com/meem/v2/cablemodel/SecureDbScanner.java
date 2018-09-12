package com.meem.v2.cablemodel;

import com.meem.mmp.mml.MMLGenericDataDesc;

/**
 * Created by arun on 31/5/17.
 */

public interface SecureDbScanner {
    /**
     * This is to be used while the actula XFR. If the return value is null, it is considered as an abort notification.
     *
     * @param desc
     *
     * @return
     */
    void onTotalItemCountForScannedCat(byte cat, int itemCount);
    Boolean onSecureDbItemWhileScanning(MMLGenericDataDesc desc);
    void onSecureDbScanningComplete(byte catCode);
}
