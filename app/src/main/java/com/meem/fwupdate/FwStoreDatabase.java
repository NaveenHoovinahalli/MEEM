package com.meem.fwupdate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.utils.GenUtils;

import java.util.ArrayList;

public class FwStoreDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FW_STORE = "table_fw_store";
    private static final String TABLE_FW_DOWNLOAD_HISTORY = "table_fw_download_history";

    private static final String KEY_ALL_AVAILABLE_FW_DOWNLOADED_TIME = "allfwdownloadedtime";

    private static final String KEY_FW_SIZE = "size";
    private static final String KEY_FW_DATE = "date";
    private static final String KEY_FW_TIME = "time";

    private static final String KEY_FW_VERSION = "fwver";
    private static final String KEY_REQ_MEEM_VERSION = "reqmeemver";
    private static final String KEY_TOOL_VERSION = "toolver";
    private static final String KEY_FW_PRIORITY = "priority";
    private static final String KEY_FW_SCUM_TYPE = "csumtype";
    private static final String KEY_FW_CSUM = "csum";
    private static final String KEY_FW_DESC_LANGUAGE = "language";
    private static final String KEY_FW_DESC = "description";
    private static final String KEY_FW_DOWNLOAD_URL = "remoteurl";
    private static final String KEY_FW_LOCAL_FILE_PATH = "localfile";

    private static AppLocalData mAppData = AppLocalData.getInstance();
    private static String mDbFilePath = mAppData.getFwStoreDbPath();
    private static UiContext mUiCtxt = UiContext.getInstance();

    public FwStoreDatabase() {
        super(mUiCtxt.getAppContext(), mDbFilePath, null, DATABASE_VERSION);
        dbgTrace("After super constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace();
        String CREATE_FW_TABLE = "CREATE TABLE " + TABLE_FW_STORE + "( " + "size TEXT, " + "date TEXT, " + "time TEXT, " + "fwver TEXT, " + "reqmeemver TEXT, " + "toolver TEXT, " + "priority TEXT, " + "csumtype TEXT, " + "csum TEXT, " + "language TEXT, " + "description TEXT, " + "remoteurl TEXT, " + "localfile TEXT)";

        String CREATE_SCHEDULE_TABLE = "CREATE TABLE " + TABLE_FW_DOWNLOAD_HISTORY + "( " + KEY_ALL_AVAILABLE_FW_DOWNLOADED_TIME + " INTEGER)";

        db.execSQL(CREATE_FW_TABLE);
        db.execSQL(CREATE_SCHEDULE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbgTrace();
        // we shall clear history, so that we will launch a new FW check on upgrade.
        // already downloaded FW wont be downloaded again as we are keeping FW store table intact.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FW_DOWNLOAD_HISTORY);
        this.onCreate(db);
    }

    public boolean add(UpdateInfo info) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_FW_SIZE, info.mFwSize);
        values.put(KEY_FW_DATE, info.mFwDate);
        values.put(KEY_FW_TIME, info.mFwTime);

        values.put(KEY_FW_VERSION, info.mFwNewVersion);
        values.put(KEY_REQ_MEEM_VERSION, info.mFwReqMeemVersion);
        values.put(KEY_TOOL_VERSION, info.mFwToolVersion);
        values.put(KEY_FW_PRIORITY, info.mFwPriority);
        values.put(KEY_FW_SCUM_TYPE, info.mFwCSumType);
        values.put(KEY_FW_CSUM, info.mFwCSumValue);

        values.put(KEY_FW_DESC_LANGUAGE, info.mFwDescLanguage);
        values.put(KEY_FW_DESC, info.mFwDescText);
        values.put(KEY_FW_DOWNLOAD_URL, info.mFwUpdateUrl);
        values.put(KEY_FW_LOCAL_FILE_PATH, info.mFwUpdateLocalFile);

        if (-1 == db.insert(TABLE_FW_STORE, null, values)) {
            dbgTrace("Adding item failed");
            result = false;
        }

        dbgTrace("Item added: " + info);
        db.close();
        return result;
    }

    public ArrayList<UpdateInfo> getList() {
        dbgTrace();

        String query = "SELECT * FROM " + TABLE_FW_STORE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        ArrayList<UpdateInfo> updateInfoList = new ArrayList<UpdateInfo>();

        try {
            if (cursor.moveToFirst()) {
                do {
                    UpdateInfo info = new UpdateInfo();
                    info.mFwSize = cursor.getString(0);
                    info.mFwDate = cursor.getString(1);
                    info.mFwTime = cursor.getString(2);

                    info.mFwNewVersion = cursor.getString(3);
                    info.mFwReqMeemVersion = cursor.getString(4);
                    info.mFwToolVersion = cursor.getString(5);
                    info.mFwPriority = cursor.getString(6);
                    info.mFwCSumType = cursor.getString(7);
                    info.mFwCSumValue = cursor.getString(8);

                    info.mFwDescLanguage = cursor.getString(9);
                    info.mFwDescText = cursor.getString(10);
                    info.mFwUpdateUrl = cursor.getString(11);
                    info.mFwUpdateLocalFile = cursor.getString(12);

                    updateInfoList.add(info);
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            dbgTrace("Exception during fetching update info list: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return updateInfoList;
    }

    public boolean delete(String csum) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        if (0 == db.delete(TABLE_FW_STORE, KEY_FW_CSUM + " = ?", new String[]{csum})) {
            dbgTrace("Item deletion failed for csum: " + csum);
            result = false;
        }

        db.close();

        return result;
    }

    /**
     * This method is dealing with schedule table. This will retrieve the time of last successful firmware download.
     *
     * @return last successful download time (since 1970 blah blah)
     */
    public long getAllAvailableFwDownloadedTime() {
        dbgTrace();

        // must use long - as date is too large for int.
        long result = 0;

        // lazy Arun
        String query = "SELECT * FROM " + TABLE_FW_DOWNLOAD_HISTORY;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            if (0 == cursor.getCount()) {
                dbgTrace("No download history. May be first time.");
                result = 0;
            } else {
                if (cursor.moveToFirst()) {
                    result = cursor.getLong(0);
                    dbgTrace("All fw were downloaded at time: " + result);
                }
            }
        } catch (Exception ex) {
            dbgTrace("Exception during fetching last fw download time: " + ex.getMessage());
            result = -1;
        }

        cursor.close();
        db.close();

        return result;
    }

    /**
     * This method is dealing with schedule table. This will record the time for last successful complete firmware download (all that are
     * available).
     *
     * @param time future time (since 1970 blah blah)
     *
     * @return Boolean
     */
    public boolean setAllAvailableFwDownloadedTime(long time) {
        dbgTrace();

        boolean result = true;
        String query = "SELECT * FROM " + TABLE_FW_DOWNLOAD_HISTORY;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            int count = cursor.getCount();

            ContentValues values = new ContentValues();
            values.put(KEY_ALL_AVAILABLE_FW_DOWNLOADED_TIME, Long.valueOf(time));

            if (count == 0) {
                // insert new schedule
                if (-1 == db.insert(TABLE_FW_DOWNLOAD_HISTORY, null, values)) {
                    dbgTrace("Adding time failed");
                    result = false;
                }
            } else {
                // update old schedule
                if (1 < db.update(TABLE_FW_DOWNLOAD_HISTORY, values, null, null)) {
                    dbgTrace("Updating time failed");
                    result = false;
                }
            }
        } catch (Exception ex) {
            dbgTrace("Exception during processing time: " + ex.getMessage());
            result = false;
        }

        if (result) {
            dbgTrace("Completion time of downloading al available fw is set successfully as: " + time);
        }

        cursor.close();
        db.close();

        return result;
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("FwStoreDatabase.log", trace);
    }

    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
