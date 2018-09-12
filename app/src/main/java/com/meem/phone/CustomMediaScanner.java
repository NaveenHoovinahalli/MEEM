package com.meem.phone;

import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import com.meem.androidapp.UiContext;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * We are not making use of android MediaScanner as it may take some time to detect and update its database of newly created files. While
 * many camera/video apps makes sure that they does update MediaScanner, unfortunately, the Meem demo/test cases will include scenarios
 * where he user is using file explorer to demo that the file is backed up and restored in a short period.
 * <p/>
 * This file is primarily intended to be used during backup. On restore we are doing the media scanner update.
 *
 * @author Arun T A
 */

public class CustomMediaScanner {
    private static final Set<String> FILTER_FOLDERS = new HashSet<String>(Arrays.asList(new String[]{"camera", "100andro", "100media"}));
    private static UiContext mUiCtxt = UiContext.getInstance();

    public CustomMediaScanner() {

    }

    public static boolean isExternalStorageUsable() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        return mExternalStorageAvailable && mExternalStorageWriteable;
    }

    public static File[] getPictureFiles() {
        File[] filesPhotos = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).listFiles();
        return filesPhotos;
    }

    public Set<String> getCameraPicturesBruteForce() {
        final String[] columns = new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.MIME_TYPE};

        // Order by options - by date & descending
        final String orderBy = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC";

        // Base URI for images
        final Cursor cursor = mUiCtxt.getAppContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);

        if (cursor == null) {
            return null;
        }

        // Total number of images
        int count = cursor.getCount();

        // Create an array to store path to all the images
        String[] picturesPath = new String[count];

        if (cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int bucketColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            do {
                if (FILTER_FOLDERS.contains(cursor.getString(bucketColumn).toLowerCase(Locale.getDefault()))) {
                    // Store the path of the image
                    picturesPath[cursor.getPosition()] = cursor.getString(dataColumn);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();

        return new HashSet<String>(Arrays.asList(picturesPath));
    }

}
