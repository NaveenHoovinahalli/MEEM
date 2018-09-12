package com.meem.core;

/**
 * This enumeration contains status codes used by MeemCore and MeemCoreListener classes.
 *
 * @author: Arun T A
 */
public enum MeemCoreStatus {
    /**
     * No error
     */
    SUCCESS(0),

    /**
     * File open failed upon an XFR_REQUEST. XFR_ERROR sent to MEEM.
     */
    XFR_ERROR_FILEOPEN(1),

    /**
     * File read failed during a backup operation. XFR_ABORT sent to MEEM.
     */
    XFR_ERROR_FILEREAD(2),

    /**
     * File write failed during a restore operation. XFR_ABORT sent to MEEM.
     */
    XFR_ERROR_FILEWRITE(3),

    /**
     * USB read operation failed. This is an unrecoverable error. MeemCore must be stopped and started again.
     */
    XFR_ERROR_USBREAD(4),

    /**
     * USB write operation failed. This is an unrecoverable error. MeemCore must be stopped and started again.
     */
    XFR_ERROR_USBWRITE(5),

    /**
     * Presently unused. The MeemCore user is to abort the XFR operations by sending SESSION_ABORT to MEEM. The XFRs will then be aborted
     * exactly as described in MMP specification. Then user will get XFR_ERROR_ABORT as a confirmation of each XFR abort.
     */
    XFR_ERROR_LOCALABORT(6),

    /**
     * The XFR is aborted by MEEM either on SESSION_ABORT request from Mobile application or because of an internal MEEM error. MeemCore
     * will send XFR_ABORTED message to MEEM on either case.
     */
    XFR_ERROR_ABORT(7),

    /**
     * Checksum mismatch detected for a received file.
     */
    XFR_ERROR_RECV_CHECKSUM(8),

    /**
     * XFR failure due to data loss for a received file.
     */
    XFR_ERROR_DATA_LOSS(9),

    /**
     * Unspecified error. Please see logs or contact developer.
     */
    XFR_ERROR_UNKNOWN(10);

    private MeemCoreStatus(int code) {
        // nothing.
    }
}
