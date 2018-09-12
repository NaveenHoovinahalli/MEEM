package com.meem.androidapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.meem.utils.GenUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class manages different cleanup stuff left behind or overlooked items that happens during app upgrades. E.g. Log file location
 * changed in one version, and we need to delete previous log folder and files.
 * <p>
 * Note: the methods of this file will be sprinkled with references to current version of the app.
 *
 * @author arun
 */

public class HouseKeeping {
    private static final String TAG = "HouseKeeping";
    Context mContext;

    public HouseKeeping(Context appContext) {
        mContext = appContext;
    }

    /**
     * To take care of versions where BuildConfig.XXX is not working
     *
     * @return Version code. 0 on error.
     */
    public int getAppVersionCode() {
        PackageManager packageManager = mContext.getPackageManager();
        String packageName = mContext.getPackageName();

        int versionCode = 0;

        try {
            versionCode = packageManager.getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionCode;
    }

    /**
     * To take care of versions where BuildConfig.XXX is not working
     *
     * @return Version name. Empty string on error.
     */
    public String getAppVersionName() {
        PackageManager packageManager = mContext.getPackageManager();
        String packageName = mContext.getPackageName();

        String versionName = "";

        try {
            versionName = packageManager.getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionName;
    }

    /**
     * Remove old log folder "externalStorageDirectory/MeemAndroid/Logs" Applicable only to version 1.0.38-BB and below.
     */
    private boolean removeOldLogFolder() {
        boolean res = false;

        // Do nothing is log directory is declared as public
        if (!ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            return true;
        }

        File extStoreFolder = Environment.getExternalStorageDirectory();
        String oldLogFolderPath = extStoreFolder.getAbsolutePath() + File.separator + "MeemAndroid/Logs";
        File oldLogFolder = new File(oldLogFolderPath);
        if (oldLogFolder.exists() && oldLogFolder.isDirectory()) {
            for (File file : oldLogFolder.listFiles()) {
                res |= file.delete();
            }

            res |= oldLogFolder.delete();
        }

        Log.d(TAG, "Removing old log folder result: " + res);
        return res;
    }

    /**
     * Remove old log folder "externalStorageDirectory/MeemAndroid" Applicable only to version 1.0.38-BB and below.
     * <p>
     * Arun: Added on 29Feb2016
     */
    private boolean removeOldAppDataFolder() {
        boolean res = false;

        File extStoreFolder = Environment.getExternalStorageDirectory();
        String oldLogFolderPath = extStoreFolder.getAbsolutePath() + File.separator + "MeemAndroid";
        File oldLogFolder = new File(oldLogFolderPath);
        if (oldLogFolder.exists() && oldLogFolder.isDirectory()) {
            for (File file : oldLogFolder.listFiles()) {
                res |= file.delete();
            }

            res |= oldLogFolder.delete();
        }

        Log.d(TAG, "Removing old log folder result: " + res);
        return res;
    }

    /**
     * Applicable to only to version 16 and below.
     *
     * @return
     */
    private boolean cleanupCableHistoryFile() {
        boolean res = false;

        File privateRootFolder = mContext.getFilesDir();
        File historyFile = new File(privateRootFolder, "lastCables.txt");

        if (!historyFile.exists()) {
            return res;
        }

        // this will make the file empty.
        try {
            FileWriter writer = new FileWriter(historyFile);
            writer.close();
            Log.d(TAG, "Last connected cable history cleared");
            res = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    private boolean copyDummyDatabase(String dbName) {
        boolean res = true;
        try {
            GenUtils.copyDatabaseAssetToPhone(mContext, dbName);
        } catch (IOException e) {
            Log.wtf(TAG, e);
            res = false;
        }

        return true;
    }

    private void cleanupPublicTempDir() {
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String meemTempDirPath = downloadPath + File.separator + ProductSpecs.PUBLIC_TEMP_DIR_NAME;

        File tempDir = new File(meemTempDirPath);
        if (tempDir.exists() && tempDir.isDirectory()) {
            for (File file : tempDir.listFiles()) {
                if (!file.getName().equals(".nomedia")) file.delete();
            }
        }
    }

    public void cleanupLeftovers() {
        if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            removeOldLogFolder();
            removeOldAppDataFolder();
        }

        cleanupCableHistoryFile();
        cleanupPublicTempDir();

        GenUtils.clearLogs();

        /*copyDummyDatabase("thumbnails.db");
        copyDummyDatabase("Contacts.db");
        copyDummyDatabase("Messages.db");
        copyDummyDatabase("Calendar.db");*/
    }
}
