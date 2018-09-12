package com.meem.androidapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLCategory;
import com.meem.utils.GenUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Arun T A
 */

public class AppLocalData {
    static AppLocalData mThis;
    private Context mContext;

    private File mRootFolder;

    private String mAppRootFolderPath;
    private String mSecondaryExternalStorageRootPath;
    private String mSecondaryExtStoragePrivateRootPath;
    private String mDatabaseFolderPath;

    // Arun: 09June2017: Made the default value be be 'true'
    private boolean mUsePrimaryStorageOpt = true;

    private String mPinfFileName = "pinf.mml";
    private String mMinfFileName = "minf.mml";

    private String mMstatFileName = "mstat.mml";
    private String mMcfgFileName = "mcfg.mml";

    private String mVstatFileName = "vstat.mml";
    private String mVcfgFileName = "vcfg.mml";

    private String mDatdFileName = "datd.mml";
    private String mSesdFileName = "sesd.mml";

    private String mDatdDbFileName = "datd.db";
    private String mGenDataCacheFileName = "gendata_cache.db";
    private String mGenFwStoreDbFileName = "fwupdates.db";
    private String mThumbnailDbFileName = "thumbnails.db";

    private String mContactDbFileName = "Contacts.db";
    private String mMessageDbFileName = "Messages.db";
    private String mCalanderDbFileName = "Calendar.db";

    private String mContactDbMirrorFileName = "contacts_sync.db";
    private String mContactDbPlusFileName = "contacts_archive.db";
    private String mMessageDbMirrorFileName = "messages_sync.db";
    private String mMessageDbPlusFileName = "messages_archive.db";
    private String mCalandarDbMirrorFileName = "calendar_sync.db";
    private String mCalandarDbPlusFileName = "calendar_archive.db";


    private String mFwManifestFileName = "update.mml";
    private String mFwUpdateFileName = "update.dat";

    private String mUserDataFile = "userdata.txt";

    private File mMeemPublicTempDir;

    // The following are for V2
    private String mSecureDbName = "secure.db"; // strange name used by firmware!
    private String mConfigDbName = "config.db";

    private String mFwUpdateFilePrefix = "update_";
    private String mFwUpdateFileSuffix = "_firmware.dat";

    private String mBlUpdateFilePrefix = "update_";
    private String mBlUpdateFileSuffix = "_bootloader.dat";

    private AppLocalData() {
        // no!
    }

    /**
     * Get an instance of this singleton class.
     */
    public static AppLocalData getInstance() {
        if (mThis == null) {
            mThis = new AppLocalData();
        }
        return mThis;
    }

    public boolean setContext(Context context) {
        mContext = context;
        if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            // countermeasures against a probable bug in some android versions
            if (null == (mRootFolder = mContext.getFilesDir())) {
                mRootFolder = mContext.getFilesDir();
            }
            mAppRootFolderPath = mRootFolder.getAbsolutePath() + File.separator + ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE_FOLDER;
        } else {
            File extStorageDir = Environment.getExternalStorageDirectory();
            String extStoragePath = extStorageDir.getAbsolutePath();

            mAppRootFolderPath = extStoragePath + File.separator + ProductSpecs.MEEM_APP_LOCAL_DATA_PUBLIC_FOLDER;

            File rootFolder = new File(mAppRootFolderPath);
            if (!rootFolder.exists()) {
                if (false == rootFolder.mkdir()) {
                    Log.e("AppLocalData", "Could not create directory to store application data");
                    mRootFolder = null;
                    return false;
                }
            }

            mRootFolder = rootFolder;
        }

        // with a dummy database name, this API works just fine.
        mDatabaseFolderPath = mContext.getDatabasePath("dummy.db").getParent();

        // Now create a temp folder for gen data item viewing and sharing. Do nothing if it already exists.
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String meemTempNoMediaFilePath = downloadPath + File.separator + ProductSpecs.PUBLIC_TEMP_DIR_NAME + File.separator + ".nomedia";

        File noMediaFile = new File(meemTempNoMediaFilePath);
        if (!noMediaFile.exists()) {
            // create parent dir.
            if (!noMediaFile.getParentFile().mkdirs()) {
                Log.w("ApplocalData", "Unable to create .meemtemp directory! Using public downloads directory instead.");
                mMeemPublicTempDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            } else {
                mMeemPublicTempDir = noMediaFile.getParentFile();
            }

            // create .nomedia file
            try {
                noMediaFile.createNewFile();
            } catch (Exception e) {
                Log.w("ApplocalData", "Unable to add .nomedia to temp directory: " + e.getMessage());
            }
        } else {
            mMeemPublicTempDir = noMediaFile.getParentFile();
        }

        return true;
    }


    // Careful: do not call before setcontext.
    public String getMeemPublicTempDir() {
        return mMeemPublicTempDir.getAbsolutePath();
    }

    // get internal/primary external storage root path
    // TODO: Move this stuff into Storage.java
    public final String getInternalStorageRootPath() {
        // This API name is the root cause of all evil.
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        rootPath = rootPath.replaceAll("/$", "");
        GenUtils.logMessageToFile("AppLocalData.log", "Primary storage root is: " + rootPath);
        return rootPath;
    }

    public ArrayList<String> getMediaPaths() {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
        return paths;
    }

    public final String getRootFolderPath() {
        return mAppRootFolderPath;
    }

    public final String getPinfPath() {
        return mAppRootFolderPath + File.separator + mPinfFileName;
    }

    public final long getPinfSize() {
        File pinf = new File(mAppRootFolderPath + File.separator + mPinfFileName);
        return pinf.length();
    }

    public final String getMinfPath() {
        return mAppRootFolderPath + File.separator + mMinfFileName;
    }

    public final String getMstatPath() {
        return mAppRootFolderPath + File.separator + mMstatFileName;
    }

    public final String getMcfgPath() {
        return mAppRootFolderPath + File.separator + mMcfgFileName;
    }

    public final String getVstatPath() {
        return mAppRootFolderPath + File.separator + mVstatFileName;
    }

    public final String getVcfgPath() {
        return mAppRootFolderPath + File.separator + mVcfgFileName;
    }

    public final String getDatdPath() {
        return mAppRootFolderPath + File.separator + mDatdFileName;
    }

    public final String getSesdPath() {
        return mAppRootFolderPath + File.separator + mSesdFileName;
    }

    // TODO: SQLiteHelper will create DB in application's private folder.
    // This works only in certain models. Need to change this to just
    // return mDatdDbFileName;
    public String getDatdDbPath() {
        // return mRootFolderPath + File.separator + mDatdDbFileName;
        return mDatdDbFileName;
    }

    // See comments above
    public String getGennricDataCacheDbPath() {
        return mGenDataCacheFileName;
    }

    // see comments above
    public String getFwStoreDbPath() {
        return mGenFwStoreDbFileName;
    }

    // See comments above
    public String getThumbnailDbPath() {
        return mThumbnailDbFileName;
    }

    // since thumbnail database is to be sent to cable, we need another method to
    // get its full path (just like SESD path, DATD path etc)
    public String getGenDataThumbnailDbFullPath() {
        return mDatabaseFolderPath + File.separator + mThumbnailDbFileName;
    }

    public String getContactDbFullPath() {
        return mDatabaseFolderPath + File.separator + mContactDbFileName;
    }

    public String getMessageDbFullPath() {
        return mDatabaseFolderPath + File.separator + mMessageDbFileName;
    }

    public String getCalendarDbFullPath() {
        return mDatabaseFolderPath + File.separator + mCalanderDbFileName;
    }

    // === For V2 ===
    public String getCalendarV2MirrorDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mCalandarDbMirrorFileName;
    }

    // === For V2 ===
    public String getCalendarV2PlusDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mCalandarDbPlusFileName;
    }

    // === For V2 ===
    public String getMessageV2MirrorDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mMessageDbMirrorFileName;
    }

    // === For V2 ===
    public String getMessageV2PlusDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mMessageDbPlusFileName;

    }

    // === For V2 ===
    public String getContactsV2MirrorDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mContactDbMirrorFileName;
    }

    // === For V2 ===
    public String getContactsV2PlusDbFullPath(String upid) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + mContactDbPlusFileName;
    }

    // === For V2 ===
    public String getSmartDataV2DatabasePath(String upid, byte cat, boolean isSync) {
        String path;

        if (!MMLCategory.isSmartCategoryCode(cat)) {
            throw new IllegalArgumentException("Invalid smart data category code: " + cat);
        }

        switch (cat) {
            case MMPConstants.MMP_CATCODE_CONTACT:
                path = isSync ? getContactsV2MirrorDbFullPath(upid) : getContactsV2PlusDbFullPath(upid);
                break;
            case MMPConstants.MMP_CATCODE_MESSAGE:
                path = isSync ? getMessageV2MirrorDbFullPath(upid) : getMessageV2PlusDbFullPath(upid);
                break;
            case MMPConstants.MMP_CATCODE_CALENDER:
                path = isSync ? getCalendarV2MirrorDbFullPath(upid) : getCalendarV2PlusDbFullPath(upid);
                break;
            default:
                throw new IllegalArgumentException("Invalid smart data category code: " + cat);
        }

        return path;
    }

    // WARNING: Do not change file names here without knowing what you are doing.
    public String getSmartCatFilePath(String upid, String cat, boolean isMirror) {
        return mAppRootFolderPath + File.separator + upid + "-" + cat + (isMirror ? "-mirror-in" : "-plus-in") + ".json";
    }

    public String getSmartCatTempFilePath(String upid, String cat, boolean isMirror) {
        return mAppRootFolderPath + File.separator + upid + "-" + cat + (isMirror ? "-mirror-tmp" : "-plus-tmp") + ".json";
    }

    public String getSmartCatArchFilePath(String upid, String cat, boolean isMirror) {
        return mAppRootFolderPath + File.separator + upid + "-" + cat + (isMirror ? "-mirror-tmp" : "-plus-tmp") + ".json.arch";
    }

    // WARNING: Do not change file names here without knowing what you are doing.
    public String getSmartCatOutFilePath(String upid, String cat, boolean isMirror) {
        String path = mAppRootFolderPath + File.separator + upid + "-" + cat + (isMirror ? "-mirror-out" : "-plus-out") + ".json";
        truncate(path);
        return path;
    }

    // WARNING: Do not change file names here without knowing what you are doing.
    public String getSmartCatEmptyFilePath(String upid, String cat, boolean isMirror) {
        String path = mAppRootFolderPath + File.separator + upid + "-" + cat + (isMirror ? "-mirror-placeholder" : "-plus-placeholder") + ".json";
        truncate(path);
        return path;
    }

/*
    // === For V2 ===
    public String getSmartDataDbInFilePath(String upid, String catName, boolean isMirror) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + catName + (isMirror ? "_sync_in" : "_archive_in") + ".db";
    }

    // === For V2 ===
    public String getSmartDataDbOutFilePath(String upid, String catName, boolean isMirror) {
        return mDatabaseFolderPath + File.separator + "_" + upid + "_" + catName + (isMirror ? "_sync" : "_archive") + ".db";
    }

    // === For V2 ===
    public String getSmartCatEmptyDbPath(String upid, String cat, boolean isMirror) {
        String path = mDatabaseFolderPath + File.separator + upid + "-" + cat + (isMirror ? "_sync_empty" : "_archive_empty") + ".db";
        truncate(path);
        return path;
    }
*/

    public boolean truncate(String path) {
        File file = new File(path);
        try {
            // This will do the trick.
            FileWriter fw = new FileWriter(file);
            fw.close();
        } catch (IOException ex) {
            Log.w("AppLocalData", "Could not truncate: " + path + ": " + ex.getMessage());
            return false;
        }

        return true;
    }

    public boolean archive(String path) {
        boolean result = true;
        File file = new File(path);

        if (!file.exists()) {
            Log.w("AppLocalData", "WTF: Input file does not exist: " + path);
        } else {
            Log.d("AppLocalData", "Input file exists: " + path);
        }

        Log.d("AppLocalData", "About to archive: " + path);

        String archPath = path + ".arch";
        File archFile = new File(archPath);
        if (archFile.exists()) {
            result = archFile.delete();
            if (!result) {
                Log.w("AppLocalData", "Could not delete previous archive: " + archPath);
                result &= false;
            } else {
                Log.d("AppLocalData", "Previous archive deleted: " + archPath);
            }
        } else {
            Log.d("AppLocalData", "OK. Previous archive not present: " + archPath);
        }

        result = file.renameTo(archFile);

        if (!result) {
            Log.w("AppLocalData", "Could not rename given file to archive name: " + path);
            result &= false;
        } else {
            Log.d("AppLocalData", "Renamed given file to archive name: " + path);
        }

        // create an empty file in place of given file
        try {
            File emptyFile = new File(path);
            emptyFile.createNewFile();
        } catch (Exception e) {
            Log.w("AppLocalData", "Could not create empty file: " + path);
            result &= false;
        }

        Log.w("AppLocalData", "Archive operation of: " + path + ": result is: " + result);
        return result;
    }

    public boolean delete(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.delete();
        }

        return true;
    }

    public String getFwManifestFilePath() {
        return mAppRootFolderPath + File.separator + mFwManifestFileName;
    }

    public String getFwUpdateFilePath() {
        return mAppRootFolderPath + File.separator + mFwUpdateFileName;
    }

    public String getFwUpdateFilePath(String version) {
        return mAppRootFolderPath + File.separator + version + "-" + mFwUpdateFileName;
    }

    public String getFwUpdateV2FilePath(String version) {
        return mAppRootFolderPath + File.separator + mFwUpdateFilePrefix + version + mFwUpdateFileSuffix;
    }

    public String getBlUpdateV2FilePath(String version) {
        return mAppRootFolderPath + File.separator + mBlUpdateFilePrefix + version + mBlUpdateFileSuffix;
    }

    // For secondary external storage (removable SDCARD) support
    // TODO: Move these to Storage.java
    public String getSecondaryExternalStorageRootPath() {
        return mSecondaryExternalStorageRootPath;
    }

    public void setSecondaryExternalStorageRootPath(String path) {
        mSecondaryExternalStorageRootPath = path;
    }

    public void optUsingPrimaryStorage(boolean option) {
        mUsePrimaryStorageOpt = option;
    }

    public boolean getPrimaryStorageUsageOption() {
        return mUsePrimaryStorageOpt;
    }

    public String getSecondaryExternalStoragePrivateRootPath() {
        return mSecondaryExtStoragePrivateRootPath;
    }

    public void setSecondaryExternalStoragePrivateRootPath(String secondaryExtStoragePrivateRootPath) {
        mSecondaryExtStoragePrivateRootPath = secondaryExtStoragePrivateRootPath;
    }

    // End. Secondary external SDCARD support

    public String getUserDataFilePath() {
        return mAppRootFolderPath + File.separator + mUserDataFile;
    }

    /**
     * Return a unique file path to download the file (specified in path argument)
     *
     * @param path
     *
     * @return
     */
    public String getTempFilePathFor(String path) {
        String tempPath = null;

        if (path.contains("assets-library://")) {
            // it is important to keep this path intact as it is required for core to understand this path
            // belongs to an iOS file. Note that this is hacks to get around the ultimate iOS problem of
            // "changing checksum of photos during a restore"
            return new String(path);
        }

        String ext = getExtension(path);

        try {
            File temp = File.createTempFile("tmp", ext, mMeemPublicTempDir);
            tempPath = temp.getAbsolutePath();
        } catch (Exception e) {
            Log.w("AppLocalData", "Error: Unable to create temp file: " + e.getMessage());
        }

        return tempPath;
    }

    /**
     * Return a unique file path to download the file (specified in path argument)
     *
     * @param path
     *
     * @return
     */
    public String getTempFilePathFor_MeemV2(String path) {
        String tempPath = null;

        if (path.contains("assets-library://")) {
            path = GenUtils.sanitizeIOSAssetLibraryItemPath_MeemV2(path);
        }

        String ext = getExtension(path);

        try {
            File temp = File.createTempFile("tmp", ext, mMeemPublicTempDir);
            tempPath = temp.getAbsolutePath();
        } catch (Exception e) {
            Log.w("AppLocalData", "Error: Unable to create temp file: " + e.getMessage());
        }

        return tempPath;
    }

    private String getExtension(String path) {
        String ext = "tmp";

        int i = path.lastIndexOf('.');
        if (i > 0) {
            try {
                ext = path.substring(i);
            } catch (IndexOutOfBoundsException ex) {
                Log.w("AppLocalData", "Error: Unable to find extension of: " + path);
            }
        }

        return ext;
    }

    public boolean deleteGenDataThumbnailDb() {
        File genThumbDb = new File(getGenDataThumbnailDbFullPath());
        return genThumbDb.delete();
    }

    public boolean deleteSmartDataThumbnailDb(byte catCode) {
        String smartDbPath = "";

        switch (catCode) {
            case MMPConstants.MMP_CATCODE_CONTACT:
                smartDbPath = getContactDbFullPath();
                break;
            case MMPConstants.MMP_CATCODE_MESSAGE:
                smartDbPath = getMessageDbFullPath();
                break;
            case MMPConstants.MMP_CATCODE_CALENDER:
                smartDbPath = getCalendarDbFullPath();
                break;
            default:
                Log.w("AppLocalData", "Error: Invalid cat:" + catCode);
                break;
        }

        File smartDbFile = new File(smartDbPath);
        return smartDbFile.delete();
    }

    // For V2
    public String getDatabaseFolderPath() {
        return mDatabaseFolderPath;
    }

    // For V2
    public String getSecureDbPath() {
        return mSecureDbName;
    }

    // For V2: NOTE: this must not be used (since ConfigDb helper class is not using SqLiteOpenHelper.
    // instead, use thexxxxFullPath() method below.
    public String getConfigDbPath() {
        return mConfigDbName;
    }

    // For V2
    public String getSecureDbFullPath() {
        return mDatabaseFolderPath + File.separator + mSecureDbName;
    }

    // For V2
    public String getConfigDbFullPath() {
        return mDatabaseFolderPath + File.separator + mConfigDbName;
    }
}
