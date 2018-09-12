package com.meem.androidapp;

import java.util.ArrayList;
import java.util.Arrays;

public class LogConfig {
    private static ArrayList<String> mEnabledLogFiles = new ArrayList<String>(Arrays.asList("Accessory.log", "AccessoryFragment.log", "MainActivity.log", "MeemCore.log", "Backup.log", "Restore.log"));

    public static boolean isLogFileEnabled(String logFile) {
        return mEnabledLogFiles.contains(logFile);
    }
}
