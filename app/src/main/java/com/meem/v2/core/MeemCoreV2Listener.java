package com.meem.v2.core;

/**
 * Created by arun on 10/5/17.
 */

import java.nio.ByteBuffer;

/**
 * MeemCoreListener interface must be implemented by the user (usually an class derived from Activity). The interface will be used to pass
 * events from MeemCore to the user.
 * <p/>
 * In this application, this interface is implemented by com.meem.mmp.control.MeemCoreHandler class
 *
 * @Author Arun T A
 */
interface MeemCoreV2Listener {
    /**
     * Will be invoked upon reception of an MMP control message by MeemCore. The ByteBuffer will be in BIG ENDIAN format and the offset will
     * be set to zero.
     * <p/>
     * Note that each time this interface will be invoked from the context of a new thread created by MeemCore.
     */
    void onCtrlMessage(final ByteBuffer pkt);

    /**
     * Arun: Abortfix 22May2015 Will be invoked when xfr-request is received. This gives a chance for the current handler to check whether
     * it has to be returned with an error. Currently this will be overridden only by ExecuteSession when the session is marked for pending
     * abort.
     */
    boolean onXfrRequest();

    /**
     * Will be invoked when the file send/receive is completed successfully
     */
    void onXfrCompletion(String path);

    /**
     * Will be invoked upon any error encountered during an XFR operation
     */
    void onXfrError(String path, int status);

    /**
     * Will be invoked to notify that a system level error was encountered and already handled by MeemCore. MeemCore may do additional
     * invocations of other functions of this interface as a part of this exception handling For example, a file read error during a backup
     * operation will result in calling onException for the failed file read operation and another invocation of onXfrError with a status of
     * MeemCore.XFRR_ERROR_FILEREAD. So, for all practical purposes, the MeemCore user can treat this interface as informational.
     *
     * @param ex The Throwable ex object can always be be casted to an Exception to get further information about the exception.
     */
    void onException(Throwable ex);

    /**
     * Warning: Only for debug.
     */
    boolean startupSanityCheck();

    /**
     * Warning: Only for debug.
     */
    void cleanup();
}
