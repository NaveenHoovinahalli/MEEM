package com.meem.phone;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.SessionCommentary;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;

import java.io.File;

/**
 * Wrapper for Android media store for MEEM specific interface.
 * <p/>
 * Technical hint: MediaStore.Audio.Media.DATA column actually contains audio media file path.
 * <p/>
 * * @author Arun TA
 * <p/>
 * ========================================================================== ============================= :MUST READ:
 * ================================ ==========================================================================
 * <p/>
 * Historical info: ========================================================================== Environment.getExternalStorageDirectory()
 * <p/>
 * Refers to whatever the device manufacturer considered to be "external storage". On some devices, this is removable media, like an SD
 * card. On some devices, this is a portion of on-device flash. Here, "external storage" means "the stuff accessible via USB Mass Storage
 * mode when mounted on a host machine" - at least until Android 2.x
 * <p/>
 * Android has no concept of "external SD", aside from external storage, as described above.
 * <p/>
 * If a device manufacturer has elected to have external storage be on-board flash and also has an SD card, you will need to contact that
 * manufacturer to determine whether or not you can use the SD card (not guaranteed) and what the rules are for using it, such as what path
 * to use for it.
 * <p/>
 * IMPORTANT UPDATE: =========================================================================== First, on Android 4.4+, you do not have
 * write access to removable media (e.g., "external SD"), except for any locations on that media that might be returned by
 * getExternalFilesDirs() and getExternalCacheDirs(). See Dave Smith's excellent analysis of this:
 * <p/>
 * http://www.doubleencore.com/2014/03/android-external-storage/ particularly if you want the low-level details.
 * <p/>
 * Second, lest anyone quibble on whether or not removable media access is otherwise part of the Android SDK, here is Dianne Hackborn's
 * assessment:
 * <p/>
 * https://groups.google.com/forum/#!topic/android-platform/14VUiIgwUjY[101-125- false]
 * <p/>
 * To summarize: ============================================================================= ...keep in mind: until Android 4.4, the
 * official Android platform has not supported SD cards at all except for two special cases: the old school storage layout where external
 * storage is an SD card (which is still supported by the platform today), and a small feature added to Android 3.0 where it would scan
 * additional SD cards and add them to the media provider and give apps read-only access to their files (which is also still supported in
 * the platform today).
 * <p/>
 * Android 4.4 is the first release of the platform that has actually allowed applications to use SD cards for storage. Any access to them
 * prior to that was through private, unsupported APIs. We now have a quite rich API in the platform that allows applications to make use of
 * SD cards in a supported way, in better ways than they have been able to before: they can make free use of their app-specific storage area
 * without requiring any permissions in the app, and can access any other files on the SD card as long as they go through the file picker,
 * again without needing any special permissions. =============================================================================
 * http://developer.android.com/guide/topics/data/data-storage.html says:
 * <p/>
 * "Although the directories provided by getExternalFilesDir() and getExternalFilesDirs() are not accessible by the MediaStore content
 * provider..."
 * <p/>
 * "Sometimes, a device that has allocated a partition of the internal memory for use as the external storage may also offer an SD card
 * slot. When such a device is running Android 4.3 and lower, the getExternalFilesDir() method provides access to only the internal
 * partition and your app cannot read or write to the SD card. Beginning with Android 4.4, however, you can access both locations by calling
 * getExternalFilesDirs(), which returns a File array with entries each location. The first entry in the array is considered the primary
 * external storage and you should use that location unless it's full or unavailable. If you'd like to access both possible locations while
 * also supporting Android 4.3 and lower, use the support library's static method, ContextCompat.getExternalFilesDirs(). This also returns a
 * File array, but always includes only one entry on Android 4.3 and lower."
 */

public class MediaStorage {
    UiContext mUiCtxt = UiContext.getInstance();
    StorageScanListener mListener;
    ContentResolver mContentResolver;
    Storage mStorage;

    boolean mAbortScan;

    public MediaStorage(StorageScanListener listener) {
        mListener = listener;
        mContentResolver = mUiCtxt.getAppContext().getContentResolver();
        mStorage = new Storage();
    }

    private void notifyItemFound(byte cat, String path) {
        File file = new File(path);

        // MediaScanner is lazy to update file system changes in real time.
        if (!file.exists()) {
            return;
        }

        long size, modtime;
        size = file.length();
        modtime = file.lastModified();
        // MediaScanner is lazy to update file system changes in real time.
        if (0 == size || 0 == modtime) {
            return;
        }

        // No need to consider items on private storage for further backup.
        if (mStorage.isItemOnSecondaryExternalPrivateStorage(path)) {
            return;
        }

        MMLGenericDataDesc newGenDataDesc = new MMLGenericDataDesc();

        newGenDataDesc.mCatCode = cat;
        newGenDataDesc.mModTime = modtime;
        newGenDataDesc.mPath = path;
        newGenDataDesc.mSize = size;
        // the following will be manipulated in onGenericDataStorageItem.
        newGenDataDesc.mStatus = 0;
        newGenDataDesc.mCSum = null;

        // Very important. See comment for this function below.
        sanitizeMediaStorageItem(newGenDataDesc);

        // Very important. See comment for this function below.
        remapCategoryCode(newGenDataDesc);

        if (null != mListener) {
            mListener.onGenericDataStorageItem(newGenDataDesc);
        }
    }

    /**
     * 24Nov2015: Arun
     * <p/>
     * This is important. This method finds out whether the path of this media storage item is on emulated sdcard (primary external) or real
     * sdcard (secondary external). It also checks whether some idiotic MediaStore implementations on some phone models returns paths that
     * belongs to android system paths (typically Chinese phones like Huawie Honor has this quirk). The backup logic will reject such items
     * as per current design.
     */
    private void sanitizeMediaStorageItem(MMLGenericDataDesc desc) {
        if (!mStorage.isItemOnPrimaryExternalStorage(desc.mPath)) {
            if (mStorage.isItemOnSecondaryExternalStorage(desc.mPath)) {
                desc.onSdCard = true; // by default it is false.
            } else {
                desc.mQuirkyDriod = true; // by default it is false.
            }
        }
    }

    /**
     * KLUDGE: Here is a piece of shit to support internal/external storage where external generic data will be re-mapped to categories as
     * below:
     * <p/>
     * Phone memory (primary external storage): --------------------------------------------------- Video = MMPConstants.MMP_CATCODE_VIDEO,
     * Photo = MMPConstants.MMP_CATCODE_PHOTO Music = MMPConstants.MMP_CATCODE_MUSIC
     * <p/>
     * External SDCARD (removable, aka secondary external storage) --------------------------------------------------- Video =
     * MMPConstants.MMP_CATCODE_VIDEO_CAM, Photo = MMPConstants.MMP_CATCODE_PHOTO_CAM, Music = MMPConstants.MMP_CATCODE_FILE
     * <p/>
     * This is pure shit as MMP_CATCODE_FILE is mapped to MML string "misc" and more than that, earlier versions of this app was using this
     * category for "Documents".
     */
    private boolean remapCategoryCode(MMLGenericDataDesc desc) {
        if (desc.onSdCard) {
            if (desc.mCatCode == MMPConstants.MMP_CATCODE_VIDEO) {
                desc.mCatCode = MMPConstants.MMP_CATCODE_VIDEO_CAM;
            }

            if (desc.mCatCode == MMPConstants.MMP_CATCODE_PHOTO) {
                desc.mCatCode = MMPConstants.MMP_CATCODE_PHOTO_CAM;
            }

            if (desc.mCatCode == MMPConstants.MMP_CATCODE_MUSIC) {
                desc.mCatCode = MMPConstants.MMP_CATCODE_FILE; // :(
            }

            if (desc.mCatCode == MMPConstants.MMP_CATCODE_DOCUMENTS) {
                desc.mCatCode = MMPConstants.MMP_CATCODE_DOCUMENTS_SD;
            }
        }
        return true;
    }

    public boolean getImageFiles(boolean isIntenalStorage) {
        boolean result = true;

        Uri uri = isIntenalStorage ? MediaStore.Images.Media.INTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Images.Media.DATA};
        String selection = null;
        Cursor cur = mContentResolver.query(uri, projection, selection, null, null);

        int totalCount = 0, count = 0;
        if (cur != null) {
            totalCount = cur.getCount();
            if (totalCount > 0) {
                while (cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Images.Media.DATA));

                    count++;
                    SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, count, totalCount,
                            MMPConstants.MMP_CATCODE_PHOTO, SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS);
                    commentary.post();

                    notifyItemFound(MMPConstants.MMP_CATCODE_PHOTO, data);

                    if (mAbortScan) {
                        result = false;
                        break;
                    }
                }
            }
            cur.close();
        }

        return result;
    }

    public boolean getMusicFiles(boolean isInternalStorage) {
        boolean result = true;

        Uri uri = isInternalStorage ? MediaStore.Audio.Media.INTERNAL_CONTENT_URI : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Audio.Media.DATA};
        // some music files may not be marked as music
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Cursor cur = mContentResolver.query(uri, projection, selection, null, null);

        int totalCount = 0, count = 0;
        if (cur != null) {
            totalCount = cur.getCount();
            if (totalCount > 0) {
                while (cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));

                    count++;
                    SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, count, totalCount,
                            MMPConstants.MMP_CATCODE_MUSIC, SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS);
                    commentary.post();

                    notifyItemFound(MMPConstants.MMP_CATCODE_MUSIC, data);

                    if (mAbortScan) {
                        result = false;
                        break;
                    }
                }
            }
            cur.close();
        }

        return result;
    }

    public boolean getVideoFiles(boolean isInternalStorage) {
        boolean result = true;

        Uri uri = isInternalStorage ? MediaStore.Video.Media.INTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Video.Media.DATA};
        String selection = null;
        Cursor cur = mContentResolver.query(uri, projection, selection, null, null);

        int totalCount = 0, count = 0;
        if (cur != null) {
            totalCount = cur.getCount();
            if (totalCount > 0) {
                while (cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Video.Media.DATA));

                    count++;
                    SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, count, totalCount,
                            MMPConstants.MMP_CATCODE_VIDEO, SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS);
                    commentary.post();

                    notifyItemFound(MMPConstants.MMP_CATCODE_VIDEO, data);

                    if (mAbortScan) {
                        result = false;
                        break;
                    }
                }
            }
            cur.close();
        }

        return result;
    }

    /**
     * This method is rather awkward because it looks for all files under media librtary and selects doc files based upon extensions. This
     * means, in this method, we will get all videos, audio files, pictures etc... which are already scanned using specific queries. This is
     * waste of time and cpu.
     *
     * @param isInternalStorage
     *
     * @return
     */
    private boolean getDocFiles(boolean isInternalStorage) {
        boolean result = true;

        Uri uri = MediaStore.Files.getContentUri(isInternalStorage ? "internal" : "external");

        String[] projection = {MediaStore.Files.FileColumns.DATA};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        Cursor cur = mContentResolver.query(uri, projection, selection, null, null);

        /* // only pdf type (can add more)

		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("docx");
		String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";

		String[] selectionArgsPdf = new String[]{ mimeType };
		Cursor cur = mContentResolver.query(uri, null, selectionMimeType, selectionArgsPdfnull, null);*/

        int totalCount = 0, count = 0;
        if (cur != null) {
            totalCount = cur.getCount();
            if (totalCount > 0) {
                while (cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Files.FileColumns.DATA));

                    if (data != null && isDocument(data)) {

                        count++;
                        SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, count, totalCount,
                                MMPConstants.MMP_CATCODE_DOCUMENTS, SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS);
                        commentary.post();

                        notifyItemFound(MMPConstants.MMP_CATCODE_DOCUMENTS, data);
                    }

                    if (mAbortScan) {
                        result = false;
                        break;
                    }
                }
            }
            cur.close();
        }

        return result;
    }

    public boolean scanForMedia(int mask) {
        boolean doImageScan = false, doVideoScan = false, doMusicScan = false, doDocScan = false, result = true;

        doImageScan = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, mask) | MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, mask);

        doVideoScan = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, mask) | MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, mask);

        doMusicScan = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, mask) | MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, mask);

        doDocScan = MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS, mask) | MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS_SD, mask);

        // We will always scan using EXTERNAL_URI of media store
        if (doImageScan) {
            result = getImageFiles(false);
            mListener.onStorageScanCompletionForCat(MMPConstants.MMP_CATCODE_PHOTO, result);
        }

        if (doVideoScan) {
            result = getVideoFiles(false);
            mListener.onStorageScanCompletionForCat(MMPConstants.MMP_CATCODE_VIDEO, result);
        }

        if (doMusicScan) {
            result = getMusicFiles(false);
            mListener.onStorageScanCompletionForCat(MMPConstants.MMP_CATCODE_MUSIC, result);
        }

        if (doDocScan) {
            result = getDocFiles(false);
            mListener.onStorageScanCompletionForCat(MMPConstants.MMP_CATCODE_DOCUMENTS, result);
        }

        if (null != mListener) {
            mListener.onStorageScanCompletion(result);
        }

        return result;
    }

    public void abortMediaStorageScan() {
        mAbortScan = true;
    }

    /*
     * To get total media sizes for initial session time calculations.
     * Note: returns size in KB.
     */
    private long getTotalMediaSize(Uri uri, String mediaSizeColumName) {
        long totalSize = 0;

        String[] columns = new String[]{"sum(" + mediaSizeColumName + ")"};
        try {
            Cursor cursor = mContentResolver.query(uri, columns, null, null, null);

            if (null != cursor) {
                cursor.moveToFirst();
                totalSize = cursor.getLong(0);
                cursor.close();
            }
        } catch (Exception e) {
            // nothing
        }

        return totalSize / 1024;
    }

    public long getTotalImageSizeKb() {
        return getTotalMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.ImageColumns.SIZE);
    }

    public long getTotalVideoSizeKb() {
        return getTotalMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.VideoColumns.SIZE);
    }

    public long getTotalMusicSizeKb() {
        return getTotalMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.AudioColumns.SIZE);
    }

    public long getTotalDocSizeKb() {
        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = {MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATA};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        Cursor cur = mContentResolver.query(uri, projection, selection, null, null);

        /* // only pdf type (can add more)

		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("docx");
		String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";

		String[] selectionArgsPdf = new String[]{ mimeType };
		Cursor cur = mContentResolver.query(uri, null, selectionMimeType, selectionArgsPdfnull, null);*/

        int count = 0;
        long totalSize = 0;
        if (cur != null) {
            count = cur.getCount();
            if (count > 0) {
                while (cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Files.FileColumns.DATA));

                    if (data != null && isDocument(data)) {
                        totalSize += cur.getLong(cur.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
                    }

                    if (mAbortScan) {
                        break;
                    }
                }
            }
            cur.close();
        }

        return totalSize / 1024;
    }

    // some utility functions for documents handling
    // ---------------------------------------------
    private String getExtension(String fpath) {
        String ext = "";

        int i = fpath.lastIndexOf('.');
        if (i > 0) {
            try {
                ext = fpath.substring(i);
            } catch (IndexOutOfBoundsException ex) {
                //dbgTrace("Error: Unable to find extension of " + fpath);
            }
        }

        return ext;
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
}
