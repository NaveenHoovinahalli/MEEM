package com.meem.businesslogic;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.utils.GenUtils;

/**
 * Implementation wise, This database is essentially same as the DATD database - but that is used as a scratch pad during session and this
 * database is a cache of all generic data items that IS present on this phone - before & after backup, restore and copy.
 * <p/>
 * I am duplicating code here - because it is not yet finalized whether we need to keep this info as it as or in some other format. Need to
 * revisit here once product requirements like sync etc are finalized.
 *
 * @author Arun T A
 */
public class GenericDataCacheDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_GENERICDATA_CACHE = "table_genericdata_chache";

    private static final String KEY_CHECKSUM = "checksum";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_PATH = "path";
    private static final String KEY_SIZE = "size";
    private static final String KEY_MODTIME = "modtime";
    private static final String KEY_STATUS = "status";
    private static final String KEY_SDCARD = "sdcard";

    private static AppLocalData mAppData = AppLocalData.getInstance();
    private static String mDbFilePath = mAppData.getGennricDataCacheDbPath();
    private static UiContext mUiCtxt = UiContext.getInstance();

    public GenericDataCacheDatabase() {
        super(mUiCtxt.getAppContext(), mDbFilePath, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace();
        String CREATE_CSUM_TABLE = "CREATE TABLE " + TABLE_GENERICDATA_CACHE + "( " + "checksum TEXT, " + "category INTEGER, " + "path TEXT, " + "size INTEGER, " + "modtime INTEGER, " + "status INTEGER, " + "sdcard INTEGER )";

        db.execSQL(CREATE_CSUM_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbgTrace();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENERICDATA_CACHE);
        this.onCreate(db);
    }

    public int getNumRows() {
        int count = 0;

        String query = "SELECT * FROM " + TABLE_GENERICDATA_CACHE;

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

        if (-1 == db.insert(TABLE_GENERICDATA_CACHE, null, values)) {
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

        if (0 == db.delete(TABLE_GENERICDATA_CACHE, KEY_CHECKSUM + " = ?", new String[]{genDataDesc.mCSum})) {
            dbgTrace("Item deletion failed");
            result = false;
        }

        db.close();

        return result;
    }

    public void deleteAll() {
        dbgTrace();

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GENERICDATA_CACHE, null, null);
        db.close();
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
            if (1 < (res = db.update(TABLE_GENERICDATA_CACHE, values, KEY_PATH + "=" + "\"" + genDataDesc.mPath + "\" AND " + KEY_SDCARD + "=" + String.valueOf(onSDCard), null))) {
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

    public String getChecksum(MMLGenericDataDesc desc) {
        String csum = null;

        int onSDCard = desc.onSdCard ? 1 : 0;

        String query = "SELECT * FROM " + TABLE_GENERICDATA_CACHE + " WHERE path = " + "\"" + desc.mPath + "\" AND " + KEY_SDCARD + "=" + String.valueOf(onSDCard);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            // Check for only one item in cursor as query result.
            int numRes = cursor.getCount();
            if (numRes > 1) {
                dbgTrace("Possible bug: " + numRes + " entries found in checksum database for: " + desc);
            }

            if (cursor.moveToFirst()) {
                do {
                    if (desc.mSize == cursor.getLong(3)) {
                        csum = cursor.getString(0);
                        dbgTrace("Got cached checksum for: " + desc.mPath + ": " + csum);
                    } else {
                        dbgTrace("Possible bug: size mismatch: " + cursor.getLong(3) + " for item: " + desc.mPath);
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

    public long getModTime(String path) {
        long modTime = 0;

        String query = "SELECT * FROM " + TABLE_GENERICDATA_CACHE + " WHERE path = " + "\"" + path + "\"";

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

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("GenericDataChecksumCacheDatabase.log", trace);
    }

    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
