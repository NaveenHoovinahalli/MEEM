package com.meem.ui.utils;

/**
 * Created by arun on 1/9/17.
 * http://sylvana.net/jpegcrop/exif_orientation.html
 */

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class ExifUtil {

    /**
     * @param src    Path to the original file
     * @param bitmap the bit map of the file or its thumbnail.
     *
     * @return
     */
    public static Bitmap fixOrientation(String src, Bitmap bitmap) {
        int orientation = getExifOrientation(src);

        if (orientation == 1) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case 2:
                matrix.setScale(-1, 1);
                break;
            case 3:
                matrix.setRotate(180);
                break;
            case 4:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case 5:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case 6:
                matrix.setRotate(90);
                break;
            case 7:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case 8:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return oriented;
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private static int getExifOrientation(String src) {
        int orientation = 1;

        try {
            ExifInterface exif = new ExifInterface(src);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        } catch (Exception e) {
            // nothing - this is not that important function.
        }

        return orientation;
    }
}
