package com.meem.v2.cablemodel;

import com.meem.mmp.mml.MMLGenericDataDesc;

/**
 * Created by arun on 15/4/17.
 */

public interface SecureDbProcessor {
    /**
     * The return value is VERY important!
     * <p>
     * If returns true, the database will be updated with the
     * given MMLGenericDataDesc object, assuming it is updated and you want the changes to be reflected back in
     * the database. This is usually the case of backup logic.
     * <p>
     * If false, nothing will happen in the database - usually used by restore logic where nothing is updated in db.
     * <p>
     * IF null, processing will be stopped and no more callbacks will come. This return value shall be used to abort operations.
     *
     * @param desc Generic data descriptor - some fields are not used. See comments on usage.
     *
     * @return
     */
    Boolean onSecureDbItemForProcessing(MMLGenericDataDesc desc);
    void onSecureDbProcessingComplete();
}
