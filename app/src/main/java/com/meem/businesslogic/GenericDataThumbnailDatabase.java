package com.meem.businesslogic;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.utils.GenUtils;

/**
 * The thumbnail image database for photos and videos. Note that the role of this class in the whole business logic is simple: add
 * thumbnails of new photos and videos files into corresponding mirror tables and send the db file to FW at the beginning of backup. FW will
 * make sure that items are moved from mirror to corresponding plus tables based upon the SESD <deleted> tag as well as the backup mode of
 * category.
 * <p/>
 * When a category is deleted, the corresponding table will be dropped by FW. App may or may not decide to fetch the db file again or update
 * the temporary DB file it downloaded during that connection - as it will always fetch the db again during a connection or app kill.
 * <p/>
 * GUI views will use read operations for all tables. No update is ever made by app on this db - except one initial insert for the version
 * table.
 *
 * @author Arun T A, 10Aug2016
 */
public class GenericDataThumbnailDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final int THUMBNAIL_DB_INIT_VERSION = -1;
    private static final String VERSION_TABLE_NAME = "dbversion";
    private static final String COL_VERSION = "version";
    // TODO: These column names are total crap.
    // TODO: Need to clean it up once first version is ready for all platforms
    private static final String COL_SRC_PATH = "srcFilePath";
    private static final String COL_SRC_CSUM = "srcCsum";
    private static final String COL_SRC_ON_SDCARD = "sdcard";
    private static final String COL_FW_ACK = "fwAck";
    private static final String COL_THUMB_IMAGE = "thumbImage";
    private static final String COL_SRC_SIZE = "filesize";
    private static final String COL_SRC_DATE = "creationdate";

    private static String mUpid;
    private static AppLocalData mAppData = AppLocalData.getInstance();
    private static String mDbFilePath = mAppData.getThumbnailDbPath();
    private static UiContext mUiCtxt = UiContext.getInstance();

    private int mThumbnailDbVersion = THUMBNAIL_DB_INIT_VERSION;

    public GenericDataThumbnailDatabase(String upid) {
        super(mUiCtxt.getAppContext(), mDbFilePath, null, DATABASE_VERSION);
        dbgTrace("After super constructor, for upid: " + upid);
        mUpid = "_" + upid;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace();

        String tableName = VERSION_TABLE_NAME;
        createVersionTable(db, tableName);

        createThumbnailTables(db);
    }

    public void createThumbnailTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        createThumbnailTables(db);
        db.close();
    }

    private void createThumbnailTables(SQLiteDatabase db) {
        dbgTrace();

        String tableName;

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_PHOTO, true);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_PHOTO, false);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_VIDEO, true);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_VIDEO, false);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_MUSIC, true);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_MUSIC, false);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_DOCUMENTS, true);
        createThumbnailTable(db, tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_DOCUMENTS, false);
        createThumbnailTable(db, tableName);
    }

    private void createThumbnailTable(SQLiteDatabase db, String tableName) {
        dbgTrace();

        String CREATE_THUMBNAIL_TABLE = "CREATE TABLE IF NOT EXISTS " + tableName + "( " + COL_SRC_PATH + " TEXT, " + COL_SRC_CSUM + " TEXT, " + COL_SRC_ON_SDCARD + " INTEGER, " + COL_FW_ACK + " INTEGER, " + COL_THUMB_IMAGE + " BLOB, " + COL_SRC_SIZE + " INTEGER, " + COL_SRC_DATE + " INTEGER )";

        db.execSQL(CREATE_THUMBNAIL_TABLE);
    }

    public boolean dropAllTables() {
        dbgTrace();

        SQLiteDatabase db = this.getWritableDatabase();

        String tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_PHOTO, true);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_PHOTO, false);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_VIDEO, true);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_VIDEO, false);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_MUSIC, true);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_MUSIC, false);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_DOCUMENTS, true);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_DOCUMENTS, false);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);

        db.close();

        return true;
    }

    // _357215061589308_doc_int_mirror
    private String catCodeToTableName(byte catCode, boolean isMirror) {
        String catName;
        switch (catCode) {
            case MMPConstants.MMP_CATCODE_PHOTO:
            case MMPConstants.MMP_CATCODE_PHOTO_CAM:
                catName = "_photo";
                break;
            case MMPConstants.MMP_CATCODE_VIDEO:
            case MMPConstants.MMP_CATCODE_VIDEO_CAM:
                catName = "_video";
                break;
            case MMPConstants.MMP_CATCODE_MUSIC:
            case MMPConstants.MMP_CATCODE_FILE:
                catName = "_music";
                break;
            case MMPConstants.MMP_CATCODE_DOCUMENTS:
            case MMPConstants.MMP_CATCODE_DOCUMENTS_SD:
                catName = "_doc_int";
                break;
            default:
                throw new IllegalArgumentException("Invalid category code for thumbnail db: " + catCode);
        }

        return mUpid + catName + (isMirror ? "_mirror" : "_plus");
    }


    private void createVersionTable(SQLiteDatabase db, String tableName) {
        dbgTrace();

        String CREATE_VERSION_TABLE = "CREATE TABLE IF NOT EXISTS " + tableName + "( " + COL_VERSION + " INTEGER )";
        db.execSQL(CREATE_VERSION_TABLE);

        if (!isVersionInfoPresent(db)) {
            insertThumbNailDbVersion(db, THUMBNAIL_DB_INIT_VERSION);
        }
    }

    private boolean isVersionInfoPresent(SQLiteDatabase db) {
        dbgTrace();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + VERSION_TABLE_NAME + ";", null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mThumbnailDbVersion = cursor.getInt(0);
                    dbgTrace("Version query succeeded: " + mThumbnailDbVersion);
                    cursor.close();
                    return true;
                } else {
                    dbgTrace("No version information in table. Virgin db.");
                    cursor.close();
                    return false;
                }
            } else {
                dbgTrace("Cursor is null!");
                return false;
            }
        } catch (SQLException e) {
            dbgTrace("Exception while querying version table: " + e.getMessage());
            return false;
        }
    }

    private boolean isThisPhoneTablesPresent(SQLiteDatabase db) {
        dbgTrace();

        // Try a query with any table - one is enough.
        String tableName = catCodeToTableName(MMPConstants.MMP_CATCODE_PHOTO, true);
        String query = "SELECT * FROM " + tableName;

        try {
            Cursor cursor = db.rawQuery(query, null);
            if (null == cursor) {
                return false;
            }
            cursor.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public void sanitizeDownloadedDb() {
        SQLiteDatabase db = this.getWritableDatabase();

        if (!isThisPhoneTablesPresent(db)) {
            createThumbnailTables(db);
        }

        db.close();
    }

    public int getThumbnailDbVersion() {
        dbgTrace();

        SQLiteDatabase db = this.getWritableDatabase();

        if (!isVersionInfoPresent(db)) {
            insertThumbNailDbVersion(db, THUMBNAIL_DB_INIT_VERSION);
        }

        db.close();
        return mThumbnailDbVersion;
    }

    private boolean insertThumbNailDbVersion(SQLiteDatabase db, int version) {
        dbgTrace();

        boolean result = true;

        ContentValues values = new ContentValues();
        values.put(COL_VERSION, version);

        try {
            long ret = 0;
            if (1 != (ret = db.insert(VERSION_TABLE_NAME, null, values))) {
                dbgTrace("Failed to insert version: return: " + ret);
                result = false;
            }
        } catch (Exception ex) {
            dbgTrace("Exception while inserting version: " + ex.getMessage());
            result = false;
        }

        return result;
    }

    public boolean setThumbNailDbVersion(int version) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_VERSION, version);
        try {
            int ret = 0;
            if (1 != (ret = db.update(VERSION_TABLE_NAME, values, null, null))) {
                dbgTrace("Failed to update version: return: " + ret);
                result = false;
            }

        } catch (Exception ex) {
            dbgTrace("Exception while updating version: " + ex.getMessage());
            result = false;
        }

        db.close();

        if (result) {
            mThumbnailDbVersion = version;
        }

        return result;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbgTrace();
        dropAllTables();
        this.onCreate(db);
    }

    // ===================================================================
    // Operations | Remember: We are adding rows to only the _mirror table.
    // ===================================================================
    public boolean addMirrorThumbNail(MMLGenericDataDesc desc, byte[] thumbnail, boolean forceAck) {
        dbgTrace();

        boolean result = true;

        String tableName = catCodeToTableName(desc.mCatCode, true);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String pathHack;
        if (desc.onSdCard) {
            pathHack = "S";
        } else {
            pathHack = "I";
        }

        pathHack = pathHack + desc.mPath;

        values.put(COL_SRC_PATH, pathHack);
        values.put(COL_SRC_CSUM, desc.mCSum);
        values.put(COL_SRC_ON_SDCARD, desc.onSdCard ? 1 : 0);
        if (forceAck) {
            values.put(COL_FW_ACK, 1); // Note: This must be a DATD item came from an old version of FW (pre-v2).
        } else {
            values.put(COL_FW_ACK, 0); // Note: This will be set by v2 FW once data is transferred
        }
        values.put(COL_THUMB_IMAGE, thumbnail);
        values.put(COL_SRC_SIZE, desc.mSize);
        values.put(COL_SRC_DATE, desc.mModTime);

        long dbret = 0;
        if (-1 == (dbret = db.insert(tableName, null, values))) {
            dbgTrace("Adding item failed, ret: " + dbret);
            result = false;
        } else {
            dbgTrace("Item added: " + desc);
        }

        db.close();
        return result;
    }

    /**
     * Remember: We are searching the rows of the _mirror table only.
     *
     * @param desc
     *
     * @return
     */
    public boolean isMirrorThumbnailPresent(MMLGenericDataDesc desc) {
        boolean result = false;

        String tableName = catCodeToTableName(desc.mCatCode, true);

        String pathHack;
        if (desc.onSdCard) {
            pathHack = "S";
        } else {
            pathHack = "I";
        }

        pathHack = pathHack + desc.mPath;

        String query = "SELECT * FROM " + tableName + " WHERE srcCsum = " + "\"" + desc.mCSum + "\"" + " AND srcFilePath = " + "\"" + pathHack + "\"";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                result = true;
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataThumbnailDb", "Exception during isPresent: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return result;
    }

    /**
     * This method can be used to clear our _mirror table or _mirror+ table for a given category
     *
     * @param catCode
     * @param isMirror
     *
     * @return
     */
    public boolean deleteAllThumbs(byte catCode, boolean isMirror) {
        dbgTrace();

        String tableName = catCodeToTableName(catCode, isMirror);

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(tableName, null, null);
        db.close();

        return true;
    }

    /**
     * Added by Arun on 30May2017 for the sdcard category thumbnails related bug in versions prior to 1.0.45
     *
     * @param desc
     *
     * @return
     */
    public boolean forceAckForEntry(MMLGenericDataDesc desc) {
        dbgTrace("updateThumbnail: fix for sdcard cats thumbnail bug in versions prior 1.0.45");

        boolean result = true;

        String tableName = catCodeToTableName(desc.mCatCode, true);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_FW_ACK, 1); // Note: This must be a DATD item came from an old version of FW (pre-v2).

        long dbret = 0;
        if (-1 == (dbret = db.update(tableName, values, "srcCSum=?", new String[]{desc.mCSum}))) {
            dbgTrace("Forcing ack for item failed, ret: " + dbret);
            result = false;
        } else {
            dbgTrace("Item ack forced for: " + desc);
        }

        db.close();
        return result;
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        Log.d("GenericDataThumbnailDb", trace);
        GenUtils.logMessageToFile("GenericDataThumbnailDatabase.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }

}
