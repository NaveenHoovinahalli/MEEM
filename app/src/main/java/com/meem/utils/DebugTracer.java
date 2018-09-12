package com.meem.utils;

/**
 * Created by arun on 5/8/16.
 */
public class DebugTracer {
    private static final String CRIT_ERR_LOG_FILE = "CriticalError.log";
    private String mLogFileName, mTag;

    public DebugTracer(String tag, String logFileName) {
        mLogFileName = logFileName;
        mTag = tag;
    }

    public void trace(String text) {
        GenUtils.logCat(mTag, text);
        GenUtils.logMessageToFile(mLogFileName, text);
    }

    public void trace() {
        trace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    public void logCriticalError(String text) {
        GenUtils.logCat(mTag, "Critical Error: " + text);
        GenUtils.logMessageToFile(CRIT_ERR_LOG_FILE, text);
    }
}
