package com.meem.androidapp;

/**
 * Internal status codes used by different app modules.
 * <p>
 * All success codes are 0 or positive numbers All error codes are negative
 * numbers. Error codes are usually grouped together (e.g. XFR errors are from
 * -1 to -10)
 *
 * @author Arun T A, For MEEMV2 01March2017
 */

public class AppConstants {
    public static final int SUCCESS = 0;

    public static final int FILE_XFR_ERROR_FILEOPEN = -1;
    public static final int FILE_XFR_ERROR_RECV_CHECKSUM = -2;
    public static final int FILE_XFR_ERROR_FILEWRITE = -3;
    public static final int FILE_XFR_ERROR_DATA_LOSS = -4;
    public static final int FILE_XFR_ERROR_ABORT = -5;
    public static final int FILE_XFR_ERROR_FILE_NOT_FOUND = -6;
    public static final int FILE_XFR_ERROR_FILEREAD = -7;
    public static final int FILE_XFR_ERROR_USBWRITE = -8;
    public static final int FILE_XFR_ERROR_OTHER = -9;

    public static final long MAX_FILE_SIZE_4GB = (4L * 1024L * 1024L * 1024L);
}
