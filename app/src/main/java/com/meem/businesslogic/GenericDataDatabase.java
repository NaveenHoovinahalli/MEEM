package com.meem.businesslogic;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.phone.Storage;
import com.meem.utils.GenUtils;

/**
 * This class is used while analyzing DATD to find out and keep modified/new and deleted items in the phone storage. This is used to avoid
 * huge memory footprints if we keep all DATD and storage information in memory.
 * <p/>
 * How this is used:- During DATD parsing, whenever we encounter a new file tag, we will check with Storage to see if the file represented
 * by the tag is modified or deleted and with that updated information, it is kept in the database.
 * <p/>
 * After DATD parsing, we start a file system scan, during which each file will be checked to see if it is present in this database using
 * the checksum of the file in storage (which is primary key of this database' only table). If it is not present in database, it is marked
 * as new file and added to this DB.
 * <p/>
 * TODO: At present I'm implementing only one table and keep the status of the item as a attribute (column). For further optimization, we
 * can go for 2 tables - one for modified/new items and one for deleted items. Even further optimization is possible by making tables for
 * each categories of generic data.
 * <p/>
 * Many other optimization options are possible in terms of the order of DATD parsing and file system scanning and using separate tables for
 * DATA parsed info and file system scanned info.
 *
 * @author Arun T A
 */
public class GenericDataDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SESSION_GENERICDATA = "table_session_genericdata";

    private static final String KEY_CHECKSUM = "checksum";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PATH = "path";
    private static final String KEY_SIZE = "size";
    private static final String KEY_MODTIME = "modtime";
    private static final String KEY_STATUS = "status";
    private static final String KEY_SDCARD = "sdcard";

    private static AppLocalData mAppData = AppLocalData.getInstance();
    private static String mDbFilePath = mAppData.getDatdDbPath();
    private static UiContext mUiCtxt = UiContext.getInstance();

    GenericDataDbListener mListener;

    public GenericDataDatabase(GenericDataDbListener listener) {
        super(mUiCtxt.getAppContext(), mDbFilePath, null, DATABASE_VERSION);
        dbgTrace("After super constructor");
        mListener = listener;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace();
        String CREATE_DATD_TABLE = "CREATE TABLE " + TABLE_SESSION_GENERICDATA + "( " + "checksum TEXT, " + "category INTEGER, " + "path TEXT, " + "size INTEGER, " + "modtime INTEGER, " + "status INTEGER, " + "sdcard INTEGER )";

        db.execSQL(CREATE_DATD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbgTrace();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION_GENERICDATA);
        this.onCreate(db);
    }

    public boolean add(MMLGenericDataDesc genDataDesc) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_CHECKSUM, genDataDesc.mCSum);
        values.put(KEY_CATEGORY, (int) genDataDesc.mCatCode);
        values.put(KEY_PATH, genDataDesc.mPath);
        values.put(KEY_SIZE, genDataDesc.mSize);
        values.put(KEY_MODTIME, genDataDesc.mModTime);
        values.put(KEY_STATUS, genDataDesc.mStatus);
        values.put(KEY_SDCARD, genDataDesc.onSdCard ? 1 : 0);

        if (-1 == db.insert(TABLE_SESSION_GENERICDATA, null, values)) {
            dbgTrace("Adding item failed");
            result = false;
        }

        dbgTrace("Item added: " + genDataDesc);
        db.close();
        return result;
    }

    public boolean delete(MMLGenericDataDesc genDataDesc) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        if (0 == db.delete(TABLE_SESSION_GENERICDATA, KEY_CHECKSUM + " = ?", new String[]{genDataDesc.mCSum})) {
            dbgTrace("Item deletion failed");
            result = false;
        }

        db.close();

        return result;
    }

    public int getNumRows() {
        int count = 0;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(query, null);

            if (null != cursor) {
                count = cursor.getCount();
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            dbgTrace("Exception: " + ex.getMessage());
        }

        dbgTrace("Number of items: " + String.valueOf(count));
        return count;
    }

    public boolean update(MMLGenericDataDesc genDataDesc) {
        dbgTrace();
        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_CHECKSUM, genDataDesc.mCSum);
        values.put(KEY_CATEGORY, (int) genDataDesc.mCatCode);
        values.put(KEY_PATH, genDataDesc.mPath);
        values.put(KEY_SIZE, genDataDesc.mSize);
        values.put(KEY_MODTIME, genDataDesc.mModTime);
        values.put(KEY_STATUS, genDataDesc.mStatus);

        int onSDCard = genDataDesc.onSdCard ? 1 : 0;
        values.put(KEY_SDCARD, onSDCard);

        try {
            int res = 0;
            if (1 < (res = db.update(TABLE_SESSION_GENERICDATA, values, KEY_PATH + "=" + "\"" + genDataDesc.mPath + "\" AND " + KEY_SDCARD + "=" + String.valueOf(onSDCard), null))) {
                dbgTrace("BUG: Updated " + res + " rows for item: " + genDataDesc.mPath);
                result = false;
            } else {
                dbgTrace("Item updated: " + genDataDesc);
            }
        } catch (Exception ex) {
            dbgTrace("Exception: update: " + ex.getMessage());
        }

        db.close();

        return result;
    }

    public int resetAllItemStatus() {
        dbgTrace();

        int result = 0;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_STATUS, Storage.ITEM_UNCHANGED);

        try {
            result = db.update(TABLE_SESSION_GENERICDATA, values, null, null);
        } catch (Exception ex) {
            dbgTrace("Exception: update: " + ex.getMessage());
        }

        db.close();

        return result;
    }

    public void deleteAll() {
        dbgTrace();

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSION_GENERICDATA, null, null);
        db.close();
    }

    public boolean isPresent(String cSum, String path) {
        boolean result = false;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE checksum = " + "\"" + cSum + "\"" + " AND path = " + "\"" + path + "\"";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                result = true;
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during isPresent: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return result;
    }

    public boolean scanForCategoryWithStatus(int catCode, int status) {
        dbgTrace();

        boolean result = true;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE category = " + String.valueOf(catCode) + " AND status = " + String.valueOf(status);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        MMLGenericDataDesc genDesc;

        try {
            if (cursor.moveToFirst()) {
                do {
                    genDesc = new MMLGenericDataDesc();
                    genDesc.mCSum = cursor.getString(0);
                    genDesc.mCatCode = (byte) cursor.getInt(1);
                    genDesc.mPath = cursor.getString(2);
                    genDesc.mSize = cursor.getLong(3);
                    genDesc.mModTime = cursor.getLong(4);
                    genDesc.mStatus = cursor.getInt(5);
                    genDesc.onSdCard = (1 == cursor.getInt(6)) ? true : false;

                    result = mListener.onDatabaseItemRetrieved(genDesc);
                } while (cursor.moveToNext() && result);
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during scanForCategoryWithStatus: " + ex.getMessage());
            result = false;
        }

        cursor.close();
        db.close();

        mListener.onDatabaseScanCompleted(result);
        return result;
    }

    public boolean scanForCategoryWithoutStatus(int catCode, int status) {
        dbgTrace();

        boolean result = true;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE category = " + String.valueOf(catCode) + " AND status != " + String.valueOf(status);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        MMLGenericDataDesc genDesc;

        try {
            if (cursor.moveToFirst()) {
                do {
                    genDesc = new MMLGenericDataDesc();
                    genDesc.mCSum = cursor.getString(0);
                    genDesc.mCatCode = (byte) cursor.getInt(1);
                    genDesc.mPath = cursor.getString(2);
                    genDesc.mSize = cursor.getLong(3);
                    genDesc.mModTime = cursor.getLong(4);
                    genDesc.mStatus = cursor.getInt(5);
                    genDesc.onSdCard = (1 == cursor.getInt(6)) ? true : false;

                    mListener.onDatabaseItemRetrieved(genDesc);
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during scanForCategoryWithoutStatus: " + ex.getMessage());
            result = false;
        }

        cursor.close();
        db.close();

        mListener.onDatabaseScanCompleted(result);
        return result;
    }

    public long getModTime(String path) {
        long modTime = 0;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE path = " + "\"" + path + "\"";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        MMLGenericDataDesc genDesc;

        try {
            if (cursor.moveToFirst()) {
                do {
                    genDesc = new MMLGenericDataDesc();
                    genDesc.mCSum = cursor.getString(0);
                    genDesc.mCatCode = (byte) cursor.getInt(1);
                    genDesc.mPath = cursor.getString(2);
                    genDesc.mSize = cursor.getLong(3);
                    genDesc.mModTime = cursor.getLong(4);
                    genDesc.mStatus = cursor.getInt(5);
                    genDesc.onSdCard = (1 == cursor.getInt(6)) ? true : false;

                    modTime = genDesc.mModTime;

                    dbgTrace("Got modtime for: " + path + ": " + String.valueOf(modTime));
                    break;
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during getModTime: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return modTime;
    }

    public String getChecksum(MMLGenericDataDesc desc) {
        String csum = null;

        int onSDCard = desc.onSdCard ? 1 : 0;

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE path = " + "\"" + desc.mPath + "\" AND " + KEY_SDCARD + "=" + String.valueOf(onSDCard);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            // TODO: Add check for only one item in cursor as query result.
            if (cursor.moveToFirst()) {
                do {
                    if (desc.mSize == cursor.getLong(3)) {
                        csum = cursor.getString(0);
                        dbgTrace("Got cached checksum for: " + desc.mPath + ": " + csum);
                    } else {
                        dbgTrace("BUG: size mismatch for item with path: " + desc.mPath);
                    }

                    break;
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during getChecksum: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return csum;
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("GenericDataDatabase.log", trace);
    }

    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    // ===================================================================
    // New methods added to support out of space scenarios related GUI
    // ===================================================================
    public long getBackupSizeForCatWithStatus(byte catCode, int status) {
        dbgTrace();

        String query = "SELECT * FROM " + TABLE_SESSION_GENERICDATA + " WHERE category = " + String.valueOf(catCode) + " AND status = " + String.valueOf(status);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        long totSize = 0;

        try {
            if (cursor.moveToFirst()) {
                do {
                    totSize += cursor.getLong(3);
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.wtf("GenericDataDatabase", "Exception during getBackupSizeForCatWithStatus: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return totSize;
    }
}
