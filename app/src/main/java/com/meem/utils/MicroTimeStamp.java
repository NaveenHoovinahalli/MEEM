package com.meem.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Class to generate time stamps with approximated microsecond precision. For example: MicroTimeStamp.INSTANCE.get() = "2012-10-21
 * 19:13:45.267128"
 *
 * @author: Arun T A
 */
public enum MicroTimeStamp {
    INSTANCE;

    private long startDate;
    private long startNanoseconds;
    private SimpleDateFormat dateFormat;

    private MicroTimeStamp() {
        this.startDate = System.currentTimeMillis();
        this.startNanoseconds = System.nanoTime();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }

    public String get() {
        long microSeconds = (System.nanoTime() - this.startNanoseconds) / 1000;
        long date = this.startDate + (microSeconds / 1000);
        return this.dateFormat.format(date) + String.format("%03d", microSeconds % 1000);
    }
}