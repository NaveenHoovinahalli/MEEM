package com.meem.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.meem.androidapp.MeemApplication;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.ui.utils.ExifUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import meem.org.apache.commons.lang3.StringEscapeUtils;
import meem.org.apache.commons.lang3.mutable.MutableBoolean;

import static com.meem.androidapp.ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE;

/**
 * Collected from net as well as own code.
 *
 * @author Arun T A
 */

@SuppressLint("SimpleDateFormat")
public class GenUtils {
    public final static String EMPTY_INPUT_MD5SUM = "d41d8cd98f00b204e9800998ecf8427e";
    public final static String INVALID_MD5SUM = "DEADBEEFDEADBEEFDEADBEEFDEADBEEF";
    public final static String ABORTED_MD5SUM = "BEEFDEADBEEFDEADBEEFDEADBEEFDEAD"; // :>) may help in debugging.

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static final String dummyMD5() {
        return EMPTY_INPUT_MD5SUM;
    }

    public static String readableFileSize(long size) {
        // Tarun: Added to convert size in correct value, i suspect it is giving
        // value in MB
        size = size * 1024;
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getStackTrace(Throwable ex) {
        String stackTrace = "";
        if (ex != null) {
            stackTrace += ((Exception) ex).getMessage() + "\n";

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ((Exception) ex).printStackTrace(pw);
            stackTrace += sw.toString();
        }

        return stackTrace;
    }

    /**
     * Calculate MD5Sum. Never returns null! On error, it returns INVALID_MD5SUM string. On abort, it returns ABORTED_MD5SUM string.
     * 28Oct2015: Implementing abort support during calculation of MD5 sum of huge files.
     */
    public static String getFileMD5(String path, MutableBoolean abortFlagObj) {
        byte[] digest = null;
        boolean aborted = false;

        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            File f = new File(path);
            FileInputStream in = new FileInputStream(f);

            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
                if (abortFlagObj != null && abortFlagObj.booleanValue()) {
                    aborted = true;
                    break;
                }
            }
            digest = digester.digest();
            in.close();

        } catch (Exception e) {
            Log.wtf("GenUtils", "Exception during checksum calculation", e);
            GenUtils.logMessageToFile("GenUtils.log", "WTF: Exception during checksum calculation for: " + path + ": " + e.getMessage());
            return INVALID_MD5SUM;
        }

        if (aborted) {
            Log.w("GenUtils", "Aborting checksum calculation on request for: " + path);
            return ABORTED_MD5SUM;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : digest)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static String getHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String formatHexString(String input) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            if (i != 0 && (i % 2 == 0)) {
                output.append(" ");
            }

            if (i != 0 && (i % 32 == 0)) {
                output.append("\n");
            }

            output.append(input.charAt(i));
        }

        return output.toString();
    }

    public static String readableDateYYYYMMDD(String date) {
        String rdate = "";
        SimpleDateFormat givenFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat readableFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date convertedCurrentDate = givenFormat.parse(date);
            rdate = readableFormat.format(convertedCurrentDate);
        } catch (ParseException e) {
            rdate = "Invalid format";
        }
        return rdate;
    }

    public static String readableDateYYYYMMDDhhmmss() {
        String rdate = "";
        SimpleDateFormat requiredFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        requiredFormat.setLenient(false);

        Calendar cal = Calendar.getInstance();
        try {
            rdate = requiredFormat.format(cal.getTime());
        } catch (Exception ex) {
            rdate = "0000-00-00-00-00-00";
        }
        return rdate;
    }

    public static void logMethodToFile(String fileName) {
        if (!ProductSpecs.ENABLE_DEBUG_METHOD_TRACING) {
            return;
        }

        StackTraceElement stack[] = Thread.currentThread().getStackTrace();
        if (stack.length < 5) {
            return;
        }

        String methodName = stack[4].getMethodName();

        logMessageToFile(fileName, methodName);
    }

    public static void logMessageToFile(String fileName, String msg) {
        if (!ProductSpecs.ENABLE_DEBUG_MESSAGES_FILE_LOGGING) {
            return;
        }

        if (msg == null || msg.isEmpty()) {
            return;
        }

        try {
            String appRootFolderPath = getAppRootFolderPath();
            if (null == appRootFolderPath) {
                Log.w("GenUtils[filelogfail]", "" + fileName + ": " + msg);
                return;
            }

            File root = new File(appRootFolderPath, ProductSpecs.LOGS_DIR);
            if (!root.exists()) {
                root.mkdirs();
            }
            File fLog = new File(root, fileName);
            boolean appendFlag = true;
            if (fLog.length() >= ProductSpecs.DEBUG_LOG_MAX_SIZE) {
                appendFlag = false;
            }

            FileWriter writer = new FileWriter(fLog, appendFlag);

            writer.append(MicroTimeStamp.INSTANCE.get() + ": " + msg + "\n");
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void logCat(String tag, String msg) {
        if (ProductSpecs.ENABLE_DEBUG_MESSAGES_LOGCAT_LOGGING) {
            Log.d(tag, msg);
        }
    }

    /**
     * This method returns the root folder path of Meem application as per ProductSpecs. This function is almost same as
     * AppLocalData.getRootPath() - except that this will not return path ending in MeemAndroid folder. This function is re-implemented here
     * here to avoid hard dependency on singleton object initializations (AppLocalData is singleton, and GenUtils is static class).
     *
     * @return Applications root path (can be null on rare error conditions)
     */
    public static String getAppRootFolderPath() {
        File rootFolder = null;
        String appRootFolderPath = null;
        if (MEEM_APP_LOCAL_DATA_PRIVATE) {
            if (MeemApplication.mAppContext == null) {
                return null;
            }
            // countermeasures against a probable bug in some android versions
            if (null == (rootFolder = MeemApplication.mAppContext.getFilesDir())) {
                rootFolder = MeemApplication.mAppContext.getFilesDir();
            }

            if (rootFolder != null) {
                appRootFolderPath = rootFolder.getAbsolutePath();
            } else {
                return null;
            }
        } else {
            File extStorageDir = Environment.getExternalStorageDirectory();
            appRootFolderPath = extStorageDir.getAbsolutePath();
        }

        return appRootFolderPath;
    }

    public static void clearLogs() {
        String appRootFolderPath = getAppRootFolderPath();
        if (null == appRootFolderPath) {
            Log.w("GenUtils", "Could not delete log files!");
            return;
        }

        File logDir = new File(appRootFolderPath, ProductSpecs.LOGS_DIR);
        if (logDir.isDirectory()) {
            for (File c : logDir.listFiles()) {
                c.delete();
            }
        }

        Log.i("GenUtils", "All log files deleted");
    }

    public static long getObjectChecksum(Object obj) throws IOException, NoSuchAlgorithmException {

        if (obj == null) {
            return 0;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();

        Checksum checksum = new CRC32();
        checksum.update(baos.toByteArray(), 0, baos.size());

        return checksum.getValue();
    }

    /**
     * convert a time string into the equivalent long milliseconds
     *
     * @param strTime string formatted as HH:MM:SS:MSMS i.e. "23:59:59:999"
     *
     * @return long integer like 86399999
     */
    public static long strToMillis(String strTime) {
        long retVal = 0;
        String hour = strTime.substring(0, 2);
        String min = strTime.substring(3, 5);
        String sec = strTime.substring(6, 8);
        String milli = strTime.substring(9, 12);
        int h = Integer.parseInt(hour);
        int m = Integer.parseInt(min);
        int s = Integer.parseInt(sec);
        int ms = Integer.parseInt(milli);

        long lH = h * 60 * 60 * 1000;
        long lM = m * 60 * 1000;
        long lS = s * 1000;

        retVal = lH + lM + lS + ms;
        return retVal;
    }

    /**
     * convert time in milliseconds to the corresponding string, in case of day roll over start from scratch 23:59:59:999 + 1 =
     * 00:00:00:000
     *
     * @param millis the number of milliseconds corresponding to tim i.e. 34137999 that can be obtained as follows;
     *               <p/>
     *               long lH = h * 60 * 60 * 1000; //hour to milli
     *               <p/>
     *               long lM = m * 60 * 1000; // minute to milli
     *               <p/>
     *               long lS = s * 1000; //seconds to milli
     *               <p/>
     *               millis = lH + lM + lS + ms;
     *
     * @return a string formatted as HH:MM:SS:MSMS i.e. "23:59:59:999"
     */
    @SuppressLint("DefaultLocale")
    public static String millisToString(long millis) {

        long hrs = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long min = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long sec = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long mls = millis % 1000;
        String toRet = String.format("%02d:%02d:%02d:%03d", hrs, min, sec, mls);
        return toRet;
    }

    /**
     * Arun: Added on 13July2016: To handle Apache lang3 library in RedMi phones. May not be needed anymore because I used jarjar tool to
     * change package paths of apache.commons.lang3 jar
     */
    @SuppressWarnings("deprecation")
    public static String sanitizeXml(String input) {
        String output = null;
        try {
            output = StringEscapeUtils.escapeXml10(input);
        } catch (Throwable t1) {
            Log.w("GenUtils", "Warning: Build problem wrt apache commons library detected: " + t1.getMessage());
            try {
                output = StringEscapeUtils.escapeXml(input);
            } catch (Throwable t2) {
                // nothing.
            }
        } finally {
            if (null == output) {
                Log.w("GenUtils", "Warning: Falling back to handmade xml sanitizer");
                output = GenUtils.crudeEscapeXml(input);
            }
        }

        return output;
    }

    public static String crudeEscapeXml(String s) {
        return s.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }

    public static byte[] getImageThumbnailCustomMethod(String imgFilePath) {
        File imageFileObj = new File(imgFilePath);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFileObj.getPath(), bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            logMessageToFile("GenUtils.log", "getImageThumbnailCustomMethod: Unable to get bounds");
            return null;
        }

        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight : bounds.outWidth;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / ProductSpecs.IMG_THUMBNAIL_SIZE;
        Bitmap bmp = BitmapFactory.decodeFile(imageFileObj.getPath(), opts);
        if (bmp == null) {
            logMessageToFile("GenUtils.log", "getImageThumbnailCustomMethod: decoding given file to bitmap failed");
            return null;
        }

        // Arun: 06Sep2017: Fix the rotation issue of some samsung photo thumbs while viewed on iphone
        Bitmap orientedBmp = ExifUtil.fixOrientation(imageFileObj.getPath(), bmp);

        // convert bitmap to jpeg and then to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!orientedBmp.compress(Bitmap.CompressFormat.JPEG, ProductSpecs.THUMBNAIL_JPEG_QUALITY, stream)) {
            logMessageToFile("GenUtils.log", "getImageThumbnailCustomMethod: compressing thumbnail to jpeg format failed");
            return null;
        }

        return stream.toByteArray();
    }

    /**
     * Warning: this method uses large amount of memory for big image files.
     *
     * @param imgFilePath
     *
     * @return
     */
    public static byte[] getImageThumbnailSystemMethod(String imgFilePath) {
        Bitmap bmp = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFilePath), ProductSpecs.IMG_THUMBNAIL_SIZE, ProductSpecs.IMG_THUMBNAIL_SIZE);
        if (bmp == null) {
            logMessageToFile("GenUtils.log", "getImageThumbnailSystemMethod: thumbnail bitmap generation failed");
            return null;
        }

        // convert bitmap to jpeg and then to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!bmp.compress(Bitmap.CompressFormat.JPEG, ProductSpecs.THUMBNAIL_JPEG_QUALITY, stream)) {
            logMessageToFile("GenUtils.log", "getImageThumbnailSystemMethod: compressing thumbnail to jpeg format failed");
            return null;
        }

        return stream.toByteArray();
    }

    public static byte[] getVideoThumbnailCustomMethod(String videoFilePath) {
        Bitmap bmp = null;

        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoFilePath);
            bmp = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception m_e) {
            logMessageToFile("GenUtils.log", "getVideoThumbnailCustomMethod: frame bitmap generation failed");
        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }

        Bitmap resizedBmp = ThumbnailUtils.extractThumbnail(bmp, ProductSpecs.IMG_THUMBNAIL_SIZE, ProductSpecs.IMG_THUMBNAIL_SIZE);

        if (resizedBmp != null) {
            // convert bitmap to jpeg and then to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!resizedBmp.compress(Bitmap.CompressFormat.JPEG, ProductSpecs.THUMBNAIL_JPEG_QUALITY, stream)) {
                logMessageToFile("GenUtils.log", "getVideoThumbnailCustomMethod: compressing thumbnail to jpeg format failed");
                return null;
            }

            return stream.toByteArray();
        } else {
            return null;
        }
    }

    public static byte[] getVideoThumbnailSystemMethod(String videoFilePath) {
        // VID_THUMBNAIL_SIZE is defined to be MICRO_KIND: 96 x 96 (Defined in MediaStore class)
        Bitmap bmp = ThumbnailUtils.createVideoThumbnail(videoFilePath, ProductSpecs.VID_THUMBNAIL_SIZE);
        if (bmp == null) {
            String err = "getVideoThumbnailSystemMethod: thumbnail bitmap generation failed";
            logCat("GenUtils", err);
            logMessageToFile("GenUtils.log", err);
            return null;
        }

        Bitmap resizedBmp = ThumbnailUtils.extractThumbnail(bmp, ProductSpecs.IMG_THUMBNAIL_SIZE, ProductSpecs.IMG_THUMBNAIL_SIZE);

        // convert bitmap to jpeg and then to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!resizedBmp.compress(Bitmap.CompressFormat.JPEG, ProductSpecs.THUMBNAIL_JPEG_QUALITY, stream)) {
            String err = "getVideoThumbnailSystemMethod: compressing thumbnail to jpeg format failed";
            logCat("GenUtils", err);
            logMessageToFile("GenUtils.log", err);
            return null;
        }

        return stream.toByteArray();
    }

    /**
     * Demo purposes: copy a database shipped as an assets to database folder of app.
     */
    public static void copyDatabaseAssetToPhone(Context appContext, String dbName) throws IOException {
        OutputStream databaseStream = new FileOutputStream(appContext.getDatabasePath(dbName));

        AssetManager assetMgr = appContext.getAssets();
        InputStream chunkStream = assetMgr.open(dbName);

        int length;
        byte[] buffer = new byte[1024];
        while ((length = chunkStream.read(buffer)) > 0) {
            databaseStream.write(buffer, 0, length);
        }

        chunkStream.close();
        databaseStream.close();
    }

    // for debugging only
    public static boolean copyDbFileToDownloads(String dbName, String targetName) {
        if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            return false;
        }

        try {
            File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), targetName);
            File currentDB = UiContext.getInstance().getAppContext().getDatabasePath(dbName);
            if (currentDB.exists()) {
                FileInputStream fis = new FileInputStream(currentDB);
                FileOutputStream fos = new FileOutputStream(backupDB);
                fos.getChannel().transferFrom(fis.getChannel(), 0, fis.getChannel().size());
                // or fis.getChannel().transferTo(0, fis.getChannel().size(), fos.getChannel());
                fis.close();
                fos.close();
                Log.i("GenUtils", "Database successfully copied to download folder: " + targetName);
                return true;
            } else Log.i("GenUtils", "Copying Database failed, database not found: " + targetName);
        } catch (IOException e) {
            Log.d("GenUtils", "Copying Database failed, reason: " + e.getMessage());
        }
        return true;
    }

    /**
     * Used to handle XFR pathUri issues for iOS items restored to Android phone - because the iOS media URIs contains invalid pathUri
     * characters.
     *
     * @param uri iOS asset library item path (uri) in DATD / XFR requests (Do keep in mind the prefixing of I and S hacks)
     *
     * @return Sanitized iOS asset library item uri. On error, no sanitization done. Will never be null. On success, the return will be of
     * the format: "iIPhone/Media/[Unique MD5 sum of uri].[ext]". <p>Example:</p> <p> Input: "Iassets-library://asset/asset.JPG?id=497B8161-760A-4DD7-A794-AE5ABC5EAC44&amp;ext=JPG"
     * </p><p> output: "IiPhone/Media/e7b6a178944ad336f18dba7fb6eb281d.JPG" </p>
     */
    public static String sanitizeIOSAssetLibraryItemPath(String uri) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.w("Genutils", "Could not instantiate MD5: + " + e.getMessage());
            return uri;
        }

        md.update(uri.getBytes());
        byte[] digest = md.digest();
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }

        String ext = "JPG"; // default
        int extoff;

        if (-1 != (extoff = uri.indexOf("ext="))) {
            try {
                ext = uri.substring(extoff + 4);
            } catch (IndexOutOfBoundsException e) {
                // suppress
            }
        }

        sb.append(".");
        sb.append(ext);

        return "IiPhone/Media/" + sb.toString();
    }

    /**
     * Used to handle XFR pathUri issues for iOS items restored to Android phone - because the iOS media URIs contains invalid pathUri
     * characters.
     *
     * @param uri iOS asset library item path (uri) in DATD / XFR requests (Do keep in mind the prefixing of I and S hacks)
     *
     * @return Sanitized iOS asset library item uri. On error, no sanitization done. Will never be null. On success, the return will be of
     * the format: "iIPhone/Media/[Unique MD5 sum of uri].[ext]". <p>Example:</p> <p> Input: "Iassets-library://asset/asset.JPG?id=497B8161-760A-4DD7-A794-AE5ABC5EAC44&amp;ext=JPG"
     * </p><p> output: "IiPhone/Media/e7b6a178944ad336f18dba7fb6eb281d.JPG" </p>
     */
    public static String sanitizeIOSAssetLibraryItemPath_MeemV2(String uri) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.w("Genutils", "Could not instantiate MD5: + " + e.getMessage());
            return uri;
        }

        md.update(uri.getBytes());
        byte[] digest = md.digest();
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }

        String ext = "JPG"; // default
        int extoff;

        if (-1 != (extoff = uri.indexOf("ext="))) {
            try {
                ext = uri.substring(extoff + 4);
            } catch (IndexOutOfBoundsException e) {
                // suppress
            }
        }

        sb.append(".");
        sb.append(ext);

        return "iPhone/Media/" + sb.toString();
    }

    public static boolean hack__isPathOfPhoto(String path) {
        return isPicture(path);
    }

    public static boolean hack__isPathOfVideo(String path) {
        return isVideo(path);
    }

    // ------ private methods copied from storage class -------------

    private static String getExtension(String fpath) {
        String ext = "";

        int i = fpath.lastIndexOf('.');
        if (i > 0) {
            try {
                ext = fpath.substring(i);
            } catch (IndexOutOfBoundsException ex) {
                Log.e("GenUtils", "Error: Unable to find extension of " + fpath);
            }
        }

        return ext;
    }

    private static boolean isPicture(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.PICTURE_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVideo(String fname) {
        String ext = getExtension(fname);

        for (String s : ProductSpecs.VIDEO_FORMATS) {
            if (ext.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    static String getAppBuildTimeGMT(Context ctxt) {
        try {
            ApplicationInfo ai = ctxt.getPackageManager().getApplicationInfo(ctxt.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            long time = ze.getTime();
            SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getInstance();
            formatter.setTimeZone(TimeZone.getTimeZone("gmt"));
            String s = formatter.format(new java.util.Date(time));
            zf.close();
            return s;
        } catch (Exception e) {
            return "No build date";
        }
    }

    // from "Random Numbers In Scientific Computing: An Introduction"
    // by Katzgrabber.
    public static long getRandomSeed() {
        long s, seed, pid;

        pid = Thread.currentThread().getId();
        s = System.currentTimeMillis();

        seed = Math.abs(((s * 181) * ((pid - 83) * 359)) % 104729);

        Random rand = new Random(seed);
        return rand.nextLong();
    }

    // The FNV hash function (Fowler/Noll/Vo), is a very fast algorithm
    // that is widely used to create unique hashes.
    //
    // This is used internally by FW datastore to create unique file names
    // within a vault directory.
    public static long fnvHash(String key) {
        byte[] p = key.getBytes();
        long h = 2166136261l;

        int i;

        for (i = 0; i < p.length; i++) {
            h = (h * 16777619) ^ p[i];
        }

        return h;
    }

    // Generate unique String, from a given string.
    // Used to create unique file names for FW.
    public static String genUniqueString(String src, int maxLen) {
        long fnvh = fnvHash(src + String.valueOf(getRandomSeed()));
        String res = String.valueOf(fnvh);

        // remove all non-alphanumerics
        res = res.replaceAll("[^a-zA-Z0-9\\s]", "");

        if (res.length() > maxLen) {
            res = res.substring(0, maxLen);
        }

        return res;
    }

    public static String genFixedUniqueString(String src, int maxLen) {
        long fnvh = fnvHash(src + String.valueOf(0xA1B9C7D6));
        String res = String.valueOf(fnvh);

        // remove all non-alphanumerics
        res = res.replaceAll("[^a-zA-Z0-9\\s]", "");

        if (res.length() > maxLen) {
            res = res.substring(0, maxLen);
        }

        return res;
    }

    public static String sanitizeCatNameForSqLite(String catString) {
        return catString.replace('-', '_');
    }

    public static void saveByteArray(byte[] bytes, String fileName) {
        if (bytes == null) {
            return;
        }

        try {
            String appRootFolderPath = getAppRootFolderPath();
            if (null == appRootFolderPath) {
                Log.w("GenUtils[filelogfail]", "" + fileName + ": " + bytes);
                return;
            }

            File root = new File(appRootFolderPath, ProductSpecs.LOGS_DIR);
            if (!root.exists()) {
                root.mkdirs();
            }

            File fLog = new File(root, fileName);
            FileOutputStream fos = new FileOutputStream(fLog);
            fos.write(bytes);
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This is a bad thing to do - but, implementing a headless fragment and an async task inner class is too much to send a udp packet!
     */
    public static void enableNetworkOnMainThreadPolicy() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }
}
