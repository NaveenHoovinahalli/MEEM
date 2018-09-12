package com.meem.phone;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.SparseArray;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.utils.GenUtils;

import java.io.File;
import java.util.ArrayList;

import meem.org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * @author Arun T A
 */

public class Storage {
    public static final int ITEM_UNCHANGED = 0;
    public static final int ITEM_MODIFIED = 1;
    public static final int ITEM_DELETED = 2;
    private static final String APP_PRIVATE_DATA_PATH_COMPONENT = "/Android/data/";
    File mRoot;
    String mPrimaryExternalStorageRootPath;
    String mSecondaryExtStorageRootPath;
    String mSecondaryExtStoragePrivateRootPath;
    int mCategoryMask = 0;
    int mVisitedFilesCount = 0;
    int mListedFilesCount = 0;
    UiContext mUiCtxt = UiContext.getInstance();
    AppLocalData mAppData = AppLocalData.getInstance();
    StorageScanListener mListener;
    SparseArray<ArrayList<File>> mCategorizedFileList = new SparseArray<ArrayList<File>>();
    Thread mScannerThread;
    boolean mAbortScan;

    public Storage(StorageScanListener listener) {
        mListener = listener;
        probeStorageDetails();
    }

    public Storage() {
        mListener = null;
        probeStorageDetails();
    }

    private void probeStorageDetails() {
        mPrimaryExternalStorageRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
        mRoot = new File(mPrimaryExternalStorageRootPath);
        probeSecondaryExternalStorageDetails();
    }

    @SuppressLint("NewApi")
    private void probeSecondaryExternalStorageDetails() {
        dbgTrace();

        mSecondaryExtStorageRootPath = null;
        mSecondaryExtStoragePrivateRootPath = null;

        /**
         * TODO: Need to add this somewhere:
         * android.os.Environment.getExternalStorageState
         * ().equals(android.os.Environment.MEDIA_MOUNTED) Need to do this after
         * testing with AndroidPhoneStorage test application for scenarios: A.
         * With and without external SDCARD. B. With SDCARD/primary external
         * storage media mounted in PC using MSD and connected via MTP.
         *
         * http://developer.android.com/guide/topics/data/data-storage.html
         * says:
         *
         * "Although the directories provided by getExternalFilesDir() and
         * getExternalFilesDirs() are not accessible by the MediaStore content
         * provider..."
         *
         * "Sometimes, a device that has allocated a partition of the internal
         * memory for use as the external storage may also offer an SD card
         * slot. When such a device is running Android 4.3 and lower, the
         * getExternalFilesDir() method provides access to only the internal
         * partition and your app cannot read or write to the SD card. Beginning
         * with Android 4.4, however, you can access both locations by calling
         * getExternalFilesDirs(), which returns a File array with entries each
         * location. The first entry in the array is considered the primary
         * external storage and you should use that location unless it's full or
         * unavailable. If you'd like to access both possible locations while
         * also supporting Android 4.3 and lower, use the support library's
         * static method, ContextCompat.getExternalFilesDirs(). This also
         * returns a File array, but always includes only one entry on Android
         * 4.3 and lower."
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] extFileDirs = mUiCtxt.getAppContext().getExternalFilesDirs(null);

            int i = 1;
            for (File dir : extFileDirs) {
                String desc;

                if (i == 1) {
                    // this must be same as mPrimaryExternalStorageRootPath
                    // obtained via constructor
                    desc = "Context.getExternalFilesDir(): Primary external storage (emulated): Legacy external storage perms: ";
                } else {
                    desc = "Context.getExternalFilesDir(): Secondary external storage (real SDCARD), with KITKAT+ external storage perms: ";
                }

                if (null != dir) {
                    dbgTrace(desc + dir.getAbsolutePath());
                } else {
                    // This will be NULL if there is secondary SDCARD support,
                    // but if it is not available.
                    dbgTrace(desc + "is NULL - may be media is not present.");
                }

                if ((i > 1) && (dir != null)) {
                    // from this we need to back trace the root path (below)
                    mSecondaryExtStoragePrivateRootPath = dir.getAbsolutePath();
                }

                i++;
            }
        } else {
            File extFilesDir = mUiCtxt.getAppContext().getExternalFilesDir(null);

            String desc = "Context.getExternalFilesDir(): Primary external private storage (emulated): Legacy external storage permissions: ";
            if (null != extFilesDir) {
                // XXX: The root path component of this directory must be same
                // as mPrimaryExternalStorageRootPath obtained via constructor.
                dbgTrace(desc + extFilesDir.getAbsolutePath());
            } else {
                // HUAWEI P6-U06 (4.2.2)
                dbgTrace(desc + "is NULL - WTF! But we don't care about primary external private storage.");
            }
        }

        // now we need to traverse back so that we will get the secondary
        // storage root
        if (null != mSecondaryExtStoragePrivateRootPath) {
            File privateFolder = new File(mSecondaryExtStoragePrivateRootPath);
            if (!privateFolder.exists()) {
                if (!privateFolder.mkdirs()) {
                    dbgTrace("Error: Could not create app private folder on secondary external storage!");
                }

                dbgTrace("Private folder creation on secondary external storage is successful.");
            } else {
                dbgTrace("Private folder on secondary external storage is already present.");
            }

            String subPath = APP_PRIVATE_DATA_PATH_COMPONENT;
            int subPathOffset = mSecondaryExtStoragePrivateRootPath.indexOf(subPath);
            if (-1 == subPathOffset) {
                dbgTrace("Error: Secondary external storage path parsing failed!");
            } else {
                mSecondaryExtStorageRootPath = mSecondaryExtStoragePrivateRootPath.substring(0, subPathOffset);
                dbgTrace("Got secondary external storage root: " + mSecondaryExtStorageRootPath);
            }

            // get rid of last slash character from root paths
            mSecondaryExtStoragePrivateRootPath = mSecondaryExtStoragePrivateRootPath.replaceAll("/$", "");
            mSecondaryExtStorageRootPath = mSecondaryExtStorageRootPath.replaceAll("/$", "");

            mAppData.setSecondaryExternalStorageRootPath(mSecondaryExtStorageRootPath);
            mAppData.setSecondaryExternalStoragePrivateRootPath(mSecondaryExtStoragePrivateRootPath);
        }
    }

    private String getExtension(String fpath) {
        String ext = "";

        int i = fpath.lastIndexOf('.');
        if (i > 0) {
            try {
                ext = fpath.substring(i);
            } catch (IndexOutOfBoundsException ex) {
                dbgTrace("Error: Unable to find extension of " + fpath);
            }
        }

        return ext;
    }

    private boolean isAudio(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.AUDIO_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVideo(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.VIDEO_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPicture(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.PICTURE_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDocument(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.DOCUMENT_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private int filterFileCategory(String fname) {
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, mCategoryMask)) {
            if (isPicture(fname)) {
                return MMPConstants.MMP_CATCODE_PHOTO;
            }
        }

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, mCategoryMask)) {
            if (isPicture(fname)) {
                return MMPConstants.MMP_CATCODE_PHOTO_CAM;
            }
        }

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, mCategoryMask)) {
            if (isVideo(fname)) {
                return MMPConstants.MMP_CATCODE_VIDEO;
            }
        }

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, mCategoryMask)) {
            if (isVideo(fname)) {
                return MMPConstants.MMP_CATCODE_VIDEO_CAM;
            }
        }

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, mCategoryMask)) {
            if (isAudio(fname)) {
                return MMPConstants.MMP_CATCODE_MUSIC;
            }
        }

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, mCategoryMask)) {
            if (isDocument(fname)) {
                return MMPConstants.MMP_CATCODE_FILE;
            }
        }

        Log.d("Storage", "Returning INVALID category: " + fname);
        return 0;
    }

    @SuppressWarnings("unused")
    private boolean addToCategorizedFileList(int category, File node, MutableBoolean abortFlagObj) {
        ArrayList<File> catList = mCategorizedFileList.get(category);
        if (null == catList) {
            catList = new ArrayList<File>();
            mCategorizedFileList.append(category, catList);
        }

        // Must look for out of memory/null pointer etc as we are in a recursive
        // function with no fixed limit.
        try {
            catList.add(node);
            GenUtils.getFileMD5(node.getAbsolutePath(), abortFlagObj);
            // Log.d("Storage", String.valueOf(category) + ": " +
            // node.getAbsolutePath());
            mListedFilesCount++;
        } catch (Exception ex) {
            Log.e("Storage", "Failed to keep file path (possible OOM)", ex);
            return false;
        }

        return true;
    }

    @SuppressWarnings("unused")
    private boolean createCategorizedFileList(int catMask, MutableBoolean abortFlagObj) {
        mVisitedFilesCount = 0;
        mListedFilesCount = 0;
        mCategoryMask = catMask;
        mCategorizedFileList.clear();
        boolean result = scanFileSystemTree(mRoot, abortFlagObj);
        Log.d("Storage", "Total files: " + String.valueOf(mVisitedFilesCount) + ", kept: " + String.valueOf(mListedFilesCount));
        return result;
    }

    /**
     * Do not use file system scanning. Use MediaLibrary APIs as per new design.
     *
     * @param node
     *
     * @return
     */
    @Deprecated
    private boolean scanFileSystemTree(File node, MutableBoolean abortFlagObj) {
        boolean result = true;

        if (mAbortScan) {
            Log.w("Storage", "File system scanning aborting on request");
            mAbortScan = false;
            return false;
        }

        if (node.isDirectory()) {
            String[] subNodeNames = node.list();
            if (null != subNodeNames) {
                for (String filename : subNodeNames) {
                    result = scanFileSystemTree(new File(node, filename), abortFlagObj);
                    if (!result) {
                        Log.e("Storage", "Scan breaking on error");
                        break;
                    }
                }
            } else {

            }
        } else {
            String absPath = node.getAbsolutePath();
            int cat = filterFileCategory(absPath);
            if (cat != 0) {
                MMLGenericDataDesc newGenDataDesc = new MMLGenericDataDesc();
                newGenDataDesc.mCatCode = (byte) cat;
                newGenDataDesc.mCSum = GenUtils.getFileMD5(node.getAbsolutePath(), abortFlagObj);
                newGenDataDesc.mModTime = node.lastModified();
                newGenDataDesc.mPath = fromPrimaryExtAbsPath(absPath);
                newGenDataDesc.mSize = node.length();
                newGenDataDesc.mStatus = 0; // will be manipulated in
                // onGenericDataStorageItem.
                mListener.onGenericDataStorageItem(newGenDataDesc);
                mListedFilesCount++;
            }
            mVisitedFilesCount++;
        }
        return result;
    }

    public boolean scanFileSystem(int catMask, MutableBoolean abortFlagObj) {
        if (mListener == null) {
            throw new IllegalStateException("Storage object is not constructed by passing Listener");
        }

        mCategoryMask = catMask;

        Log.d("Storage", "Filesystem scanning started");
        boolean result = scanFileSystemTree(mRoot, abortFlagObj);

        Log.d("Storage", "Filesystem scanning finished");
        mListener.onStorageScanCompletion(result);

        return true;
    }

    public void abortFileSystemScan() {
        mAbortScan = true;
    }

    /**
     * This is a very important function which updates the generic data description of a DATD scanned item or a storage reported item during
     * backup.
     * <p/>
     * Storage reported items are passing through a cached checksum hook in business logic. That is why we are calling this function even
     * though it is reported by storage OR media storage. Be very careful modifying this code because having these two scenarios in single
     * function made some logically interesting code.
     *
     * @param genDesc
     * @param cachedChecksum
     *
     * @return
     */
    public void updateItemStatusForBackup(MMLGenericDataDesc genDesc, String cachedChecksum, MutableBoolean abortFlagObj) {
        dbgTrace();

        // TODO: Revisit here when adding support for secondary external
        // storage.
        String path;

        if (genDesc.onSdCard) {
            path = toSecExtAbsPath(genDesc.mPath);
        } else {
            path = toPrimaryExtAbsPath(genDesc.mPath);
        }

        File file = new File(path);
        if (file.exists()) {
            // TODO: Obviously here is a bug if we ignore modified time and
            // calculate checksum only if item size changed. but,
            // File.setModTime is not working in latest versions of
            // Android... known bug there, oh well.

            if (/* genDesc.mModTime == file.lastModified() */true) {
                if (genDesc.mSize == file.length()) {
                    if (null == cachedChecksum) {
                        dbgTrace(path + ": Analysing csum for item that is not in cache");
                        String curCheckSum = GenUtils.getFileMD5(path, abortFlagObj);
                        if (null != genDesc.mCSum) {
                            // ---------------------------
                            // This is DATD scanning time
                            // ---------------------------
                            if (genDesc.mCSum.equals(curCheckSum)) {
                                dbgTrace(path + ": DATD Item unchanged");
                                genDesc.mStatus = ITEM_UNCHANGED;
                            } else {
                                dbgTrace(path + ": Updating checksum of DATD item");
                                genDesc.mCSum = curCheckSum;
                                genDesc.mStatus = ITEM_MODIFIED;
                            }
                        } else {
                            // ---------------------------
                            // This is media scanning time
                            // ---------------------------
                            dbgTrace(path + ": Updating checksum for new storage item");
                            genDesc.mCSum = curCheckSum;
                            genDesc.mStatus = ITEM_MODIFIED;
                        }
                    } else {
                        dbgTrace(path + ": Analysing csum for item that is present in cache");
                        if (null != genDesc.mCSum) {
                            // ---------------------------
                            // This is DATD scanning time
                            // ---------------------------
                            if (genDesc.mCSum.equals(cachedChecksum)) {
                                // This is the only real advantage of cache
                                dbgTrace(path + ": DATD Item unchanged");
                                genDesc.mStatus = ITEM_UNCHANGED;
                            } else {
                                /*
                                 * ARUN: TO BE VERIFIED change on 29 December 2014
								 */

                                // wow! item changed mysteriously! this situation is possible because we are
                                // forced to avoid 'last changed time' attribute described above.
                                dbgTrace("WARNING: " + path + ": Calculating checksum for mysteriously changed storage item!");
                                String curCheckSum = GenUtils.getFileMD5(path, abortFlagObj);
                                genDesc.mCSum = curCheckSum; // TODO: may be we can use cached checksum...
                                genDesc.mStatus = ITEM_MODIFIED;
                            }
                        } else {
                            // ---------------------------
                            // This is media scanning time
                            // ---------------------------

							/*
                             * ARUN: TO BE VERIFIED change on 29 December 2014, commented out the block below
							 * and added following code.
							 */

							/*
                            dbgTrace(path + ": Calculating checksum for new storage item");
							String curCheckSum = GenUtils.getFileMD5(path);
							genDesc.mCSum = curCheckSum; // TODO: may be we can use cached checksum...
							genDesc.mStatus = ITEM_MODIFIED;
							*/

                            // This is the advantage of cache
                            dbgTrace(path + ": Using cached checksum for unchanged storage item");
                            genDesc.mCSum = cachedChecksum;
                            genDesc.mStatus = ITEM_UNCHANGED;
                        }
                    }
                } else {
                    dbgTrace(path + ": Item size changed");
                    genDesc.mSize = file.length();
                    genDesc.mCSum = GenUtils.getFileMD5(path, abortFlagObj);
                    genDesc.mStatus = ITEM_MODIFIED;
                }
            } /*
				* else { dbgTrace(genDesc.mPath + ": Item time changed"); status =
				* ITEM_MODIFIED; // TODO: update csum; }
				*/
        } else {
            dbgTrace(path + ": Item deleted");
            genDesc.mStatus = ITEM_DELETED;
        }
    }

    /**
     * This is a very important function which updates the generic data description of a DATD scanned item or a storage reported item during
     * restore.
     * <p/>
     * Storage reported items are passing through a cached checksum hook in business logic. That is why we are calling this function even
     * though it is reported by storage OR media storage.
     * <p/>
     * Note that during restore, we only update status - other attributes of the file are to be left alone.
     *
     * @param genDesc
     * @param
     *
     * @return
     */
    public void updateItemStatusForRestore(MMLGenericDataDesc genDesc, MutableBoolean abortFlagObj) {
        dbgTrace();

        // TODO: Revisit here when adding support for secondary external
        // storage.
        String path = genDesc.mPath;
        File file;
        if (genDesc.onSdCard) {
            file = getFileFromSecondaryExternalStorage(path);
        } else {
            path = toPrimaryExtAbsPath(genDesc.mPath);
            file = new File(path);
            if (!file.exists()) {
                file = null;
            }
        }

        if (null != file) {
            path = file.getAbsolutePath();

            // TODO: Obviously here is a bug if we ignore modified time and
            // calculate checksum only if item size changed. but,
            // File.setModTime is not working in latest versions of
            // Android... known bug there, oh well.

            if (/* genDesc.mModTime == file.lastModified() */true) {
                if (genDesc.mSize == file.length()) {
                    String currentCsum = GenUtils.getFileMD5(path, abortFlagObj);
                    if (genDesc.mCSum.equals(currentCsum)) {
                        dbgTrace("Checksum matched for: " + path);
                        genDesc.mStatus = ITEM_UNCHANGED;
                    } else {
                        dbgTrace("Checksum change detected for: " + path);
                        genDesc.mStatus = ITEM_MODIFIED;
                    }
                } else {
                    dbgTrace("Size changed for: " + path);
                    genDesc.mStatus = ITEM_MODIFIED;
                }
            }
			/*
			 * else { dbgTrace(genDesc.mPath + ": Item time changed"); status =
			 * ITEM_MODIFIED_OR_NEW; }
			 */
        } else {
            dbgTrace("Deleted item found: " + path);
            genDesc.mStatus = ITEM_DELETED;
        }
    }

    // for dealing with internal/primary external paths
    public String fromPrimaryExtAbsPath(String absPath) {
        // Arun: 20Nov2017: Many other recreates root path while restoring data to a directory!
        String retPath = absPath.replaceFirst(mPrimaryExternalStorageRootPath, "");
        dbgTrace(absPath + " -> " + retPath);
        return retPath;
    }

    // for dealing with internal/primary external paths
    public String toPrimaryExtAbsPath(String path) {
        if (null != path) {
            if (path.startsWith(mPrimaryExternalStorageRootPath)) {
                dbgTrace("[X] " + path + " -> " + path);
                return path;
            }
        }
        String retPath = mPrimaryExternalStorageRootPath + path;
        dbgTrace(path + " -> " + retPath);
        return retPath;
    }

    // For dealing with secondary external storage
    public String fromSecondaryExtAbsPath(String absPath) {
        String retPath;
        if (null != mSecondaryExtStorageRootPath) {
            // Arun: 20Nov2017: Many other recreates root path while restoring data to a directory!
            retPath = absPath.replaceFirst(mSecondaryExtStorageRootPath, "");
            dbgTrace(absPath + " -> " + retPath);
        } else {
            retPath = fromPrimaryExtAbsPath(absPath);
        }

        return retPath;
    }

    // for dealing with internal/primary external paths. this function
    // will make sure that the resulting path points to a writable location
    // on the real removable external storage.
    public String toSecExtAbsPath(String path) {
        if (null != path && null != mSecondaryExtStorageRootPath) {
            if (path.startsWith(mSecondaryExtStorageRootPath)) {
                dbgTrace("[X] " + path + " -> " + path);
                return path;
            }
        }

        String retPath;
        if (null != mSecondaryExtStorageRootPath) {
            retPath = mSecondaryExtStorageRootPath + path;
            dbgTrace(path + " -> " + retPath);
        } else {
            retPath = toPrimaryExtAbsPath(path);
        }
        return retPath;
    }

    // for dealing with internal/primary external paths. this function
    // will make sure that the resulting path points to a writable location
    // on the real removable external storage.
    public String toSecExtPrivateAbsPath(String path) {
        String retPath;
        if (null != mSecondaryExtStoragePrivateRootPath) {
            retPath = mSecondaryExtStoragePrivateRootPath + path;
            dbgTrace(path + " -> " + retPath);
        } else {
            retPath = toPrimaryExtAbsPath(path);
        }
        return retPath;
    }

    // Does the path belongs to a file on internally emulated SDCARD?
    public boolean isItemOnPrimaryExternalStorage(String mPath) {
        if (null == mPrimaryExternalStorageRootPath) {
            return false;
        }

        return mPath.contains(mPrimaryExternalStorageRootPath);
    }

    // Does the path belongs to a file on removable SD card?
    public boolean isItemOnSecondaryExternalStorage(String mPath) {
        if (null == mSecondaryExtStorageRootPath) {
            return false;
        }

        return mPath.contains(mSecondaryExtStorageRootPath);
    }

    // Does the path belongs to a file on our private root
    // in removable SDCARD?
    // Only called by MediaStorage => during backup
    public boolean isItemOnSecondaryExternalPrivateStorage(String mPath) {
        if (null == mSecondaryExtStoragePrivateRootPath) {
            return false;
        }

        return mPath.contains(mSecondaryExtStoragePrivateRootPath);
    }

    /**
     * Find the file on external SEDCARD root or in our sand boxed private root. TODO: Review this method [11sept2014]
     */
    File getFileFromSecondaryExternalStorage(String path) {
        String trialPath;
        if (null != mSecondaryExtStorageRootPath) {
            trialPath = toSecExtAbsPath(path);
            File targetFile = new File(trialPath);
            if (targetFile.exists()) {
                return targetFile;
            }
        } else {
            return null;
        }

        if (null != mSecondaryExtStoragePrivateRootPath) {
            trialPath = toSecExtPrivateAbsPath(path);
            File targetFile = new File(trialPath);
            if (targetFile.exists()) {
                return targetFile;
            }
        }

        return null;
    }

    /**
     * Gives the free space on the device on whose file system the given path is physically present.
     *
     * @param path
     *
     * @return free space in MB
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private long getFreeSpaceOnDevice(String path) {
        long freeSpace = 0;

        StatFs statFs = new StatFs(path);
        if (null != statFs) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long bytesAvailable = (long) statFs.getBlockSizeLong() * (long) statFs.getAvailableBlocksLong();
                freeSpace = bytesAvailable / (1024 * 1024);
            } else {
                long bytesAvailable = (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
                freeSpace = bytesAvailable / (1024 * 1024);
            }
        }

        return freeSpace;
    }

    /**
     * Get Free space (MB) in 'internal' memory storage of phone. Note that this will mean the internal memory mapped as an 'virtual'
     * SDCARD.
     */
    public long getPrimaryExternalStorageFreeSpace() {
        return getFreeSpaceOnDevice(Environment.getDataDirectory().getPath());
    }

    /**
     * Get Free space (MB) in 'external' memory storage of phone. Here we are explicitly checking for secondary external storage device -
     * a.k.a SDCARD!
     */
    public long getSecondaryExternalStorageFreeSpace() {
        if (null == mSecondaryExtStoragePrivateRootPath) {
            // Our probe did not detect an external SDCARD.
            return 0;
        }
        return getFreeSpaceOnDevice(mSecondaryExtStoragePrivateRootPath);
    }

    /**
     * Gives the total space on the device on whose file system the given path is physically present.
     *
     * @param path
     *
     * @return free space in MB
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private long getTotalStorageSpaceOnDevice(String path) {
        long totalSpace = 0;

        StatFs statFs = new StatFs(path);
        if (null != statFs) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long bytesAvailable = (long) statFs.getBlockSizeLong() * (long) statFs.getBlockCountLong();
                totalSpace = bytesAvailable / (1024 * 1024);
            } else {
                long bytesAvailable = (long) statFs.getBlockSize() * (long) statFs.getBlockCount();
                totalSpace = bytesAvailable / (1024 * 1024);
            }
        }

        return totalSpace;
    }

    /**
     * Get total space (MB) in 'internal' memory storage of phone. Note that this will mean the internal memory mapped as an 'virtual'
     * SDCARD.
     */
    public long getPrimaryExternalStorageCapacity() {
        return getTotalStorageSpaceOnDevice(Environment.getDataDirectory().getPath());
    }

    /**
     * Get total space (MB) in 'external' memory storage of phone. Here we are explicitly checking for secondary external storage device -
     * a.k.a SDCARD!
     */
    public long getSecondaryExternalStorageCapacity() {
        if (null == mSecondaryExtStoragePrivateRootPath) {
            // Our probe did not detect an external SDCARD.
            return 0;
        }
        return getTotalStorageSpaceOnDevice(mSecondaryExtStoragePrivateRootPath);
    }

    /**
     * Get total storage capacity (in KB) of the phone including internal memory, that includes virtual SDCAD and physical SDCARD. This API
     * is usually to be used by GUI layer only.
     *
     * @return total storage capacity of the phone in KB
     */
    public long getTotalStorageCapacity() {
        long totalPhoneStorageCapacity = getPrimaryExternalStorageCapacity() + getSecondaryExternalStorageCapacity();
        return totalPhoneStorageCapacity * 1024;
    }

    public long getTotalFreeSpace() {
        long totalFreeSpace = getPrimaryExternalStorageFreeSpace() + getSecondaryExternalStorageFreeSpace();
        return totalFreeSpace * 1024;
    }

    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("Storage.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("Storage.log");
    }
}
