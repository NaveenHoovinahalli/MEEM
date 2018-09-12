package com.meem.businesslogic;

import com.meem.mmp.mml.MMLGenericDataDesc;

/**
 * Listener interface for generic data database action results. These interfaces are exclusively used in backup and restore logic.
 *
 * @author Arun T A
 */
public interface GenericDataDbListener {
    public boolean onDatabaseItemRetrieved(MMLGenericDataDesc desc);

    public void onDatabaseScanCompleted(boolean result);
}
