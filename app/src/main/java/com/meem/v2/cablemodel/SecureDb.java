package com.meem.v2.cablemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.DatabaseContext;
import com.meem.androidapp.SessionCommentary;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.utils.GenUtils;

import static com.meem.mmp.mml.MMLCategory.getAllGenCatStrings;

/**
 * Note the use of DatabaseContext (this enables me to create the database at any folder specified in AppLocalData)
 * Created by arun on 24/3/17.
 * <p>
 * REMEMBER!!! ACK field and FW_PRIVATE fields in main tables are dummy. Always check and set them in extended tables (_t tables)
 */

public class SecureDb extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    // generic data table schema
    private static final String COL_GDATA_PHONE_PATH = "phone_path"; // 0
    private static final String COL_GDATA_MEEM_PATH = "meem_path"; // 1
    private static final String COL_GDATA_SIZE = "size"; // 2
    private static final String COL_GDATA_CSUM = "csum"; // 3
    private static final String COL_GDATA_IOS_CSUM = "ios_csum"; // 4
    private static final String COL_GDATA_MOD_TIME = "mod_time"; // 5
    private static final String COL_GDATA_FROM_SDCARD = "sdcard"; // 6

    private static final String COL_GDATA_FW_ACK = "fw_ack"; // 7
    private static final String COL_GDATA_FW_PRIV = "fw_private"; // 8, set 1 to indicate a 'sync' file is deleted in phone.
    private static final String COL_GDATA_THUMB_IMAGE = "thumb_image"; // 9
    private static final String COL_GDATA_BACKUP_MODE = "backup_mode"; // 10

    private static final String COL_GDATA_RSVD1 = "rsvd1"; // 11
    private static final String COL_GDATA_RSVD2 = "rsvd2"; // 12

    // smart data table schema
    private static final String COL_SDATA_CTYPE = "ctype"; // 0
    private static final String COL_SDATA_BACKUP_TIME = "backup_time"; // 1

    private static final String COL_SDATA_SYNC_PHONE_PATH = "sync_phone_path"; // 2
    private static final String COL_SDATA_SYNC_MEEM_PATH = "sync_meem_path"; // 3
    private static final String COL_SDATA_SYNC_SIZE = "sync_size"; // 4
    private static final String COL_SDATA_SYNC_CSUM = "sync_csum"; // 5

    private static final String COL_SDATA_ARCH_PHONE_PATH = "archive_phone_path"; // 6
    private static final String COL_SDATA_ARCH_MEEM_PATH = "archive_meem_path"; // 7
    private static final String COL_SDATA_ARCH_SIZE = "archive_size"; // 8
    private static final String COL_SDATA_ARCH_CSUM = "archive_csum"; // 9

    private static final String COL_SDATA_SYNC_FW_ACK = "sync_fw_ack"; // 10
    private static final String COL_SDATA_ARCH_FW_ACK = "archive_fw_ack"; // 11

    private static final String COL_SDATA_RSVD1 = "rsvd1"; // 12
    private static final String COL_SDATA_RSVD2 = "rsvd2"; // 13

    // other stuff
    private static AppLocalData mAppData = AppLocalData.getInstance();
    private static String mDbFileName = mAppData.getSecureDbPath();
    private static UiContext mUiCtxt = UiContext.getInstance();

    private String mCurrentUpid;

    public SecureDb(String upid) {
        super(new DatabaseContext(mUiCtxt.getAppContext()), mDbFileName, null, DATABASE_VERSION);
        dbgTrace("After super constructor, for upid: " + upid);
        mCurrentUpid = "_" + upid;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace();
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dbgTrace();
        this.onCreate(db);
    }

    // ===================================================================
    // Private stuff
    // ===================================================================
    private String genCatCodeToTableName(byte catCode) {
        return genCatStringToTableName(MMLCategory.toGenericCatString(catCode));
    }

    private String genCatStringToTableName(String catString) {
        catString = GenUtils.sanitizeCatNameForSqLite(catString);
        return mCurrentUpid + "_" + catString + "_genericdata";
    }

    private String getSmartDataTableName() {
        return mCurrentUpid + "_smartdata";
    }

    private void createGenDataTable(SQLiteDatabase db, String tableName) {
        dbgTrace();

        String CREATE_GDATA_TABLE = "CREATE TABLE IF NOT EXISTS " + tableName + "( " +
                COL_GDATA_PHONE_PATH + " TEXT, " +  /*0*/
                COL_GDATA_MEEM_PATH + " TEXT, " + /*1*/
                COL_GDATA_SIZE + " INTEGER, " + /*2*/
                COL_GDATA_CSUM + " TEXT, " + /*3*/
                COL_GDATA_IOS_CSUM + " TEXT, " + /*4*/
                COL_GDATA_MOD_TIME + " INTEGER, " + /*5*/
                COL_GDATA_FROM_SDCARD + " INTEGER, " + /*6*/
                COL_GDATA_FW_ACK + " INTEGER, " + /*7*/
                COL_GDATA_FW_PRIV + " INTEGER, " + /*8*/
                COL_GDATA_THUMB_IMAGE + " BLOB, " + /*9*/
                COL_GDATA_BACKUP_MODE + " INTEGER, " + /*10*/
                COL_GDATA_RSVD1 + " TEXT, " + /*11*/
                COL_GDATA_RSVD2 + " INTEGER )"; /*12*/

        db.execSQL(CREATE_GDATA_TABLE);

        String extTableName = tableName + "_t";
        String CREATE_EXT_GDATA_TABLE = "CREATE TABLE IF NOT EXISTS " + extTableName + "( " +
                COL_GDATA_PHONE_PATH + " TEXT, " + /*0*/
                COL_GDATA_MEEM_PATH + " TEXT, " + /*1*/
                COL_GDATA_FW_ACK + " INTEGER, " + /*2*/
                COL_GDATA_FW_PRIV + " INTEGER )"; /*3*/

        db.execSQL(CREATE_EXT_GDATA_TABLE);
    }

    private void cleanupDeletedGenDataTable(SQLiteDatabase db, String tableName) {
        dbgTrace();

        String dropTableName = tableName + "_delete";

        dbgTrace("Dropping table: " + dropTableName);
        String DELETE_GDATA_TABLE = "DROP TABLE IF EXISTS " + dropTableName;
        db.execSQL(DELETE_GDATA_TABLE);

        String dropExtTableName = tableName + "_t_delete";
        dbgTrace("Dropping ext table: " + dropExtTableName);
        String DELETE_EXT_GDATA_TABLE = "DROP TABLE IF EXISTS " + dropExtTableName;
        db.execSQL(DELETE_EXT_GDATA_TABLE);
    }

    private void createSmartDataTable(SQLiteDatabase db) {
        dbgTrace();

        String tableName = getSmartDataTableName();

        String CREATE_SDATA_TABLE = "CREATE TABLE IF NOT EXISTS " + tableName + "( " +
                COL_SDATA_CTYPE + " INTEGER, " +
                COL_SDATA_BACKUP_TIME + " TEXT, " +

                COL_SDATA_SYNC_PHONE_PATH + " TEXT, " +
                COL_SDATA_SYNC_MEEM_PATH + " TEXT, " +
                COL_SDATA_SYNC_SIZE + " INTEGER, " +
                COL_SDATA_SYNC_CSUM + " TEXT, " +

                COL_SDATA_ARCH_PHONE_PATH + " TEXT, " +
                COL_SDATA_ARCH_MEEM_PATH + " TEXT, " +
                COL_SDATA_ARCH_SIZE + " INTEGER, " +
                COL_SDATA_ARCH_CSUM + " TEXT, " +

                COL_SDATA_SYNC_FW_ACK + " INTEGER, " +
                COL_SDATA_ARCH_FW_ACK + " INTEGER, " +

                COL_SDATA_RSVD1 + " TEXT, " +
                COL_SDATA_RSVD2 + " INTEGER )";

        db.execSQL(CREATE_SDATA_TABLE);
    }

    private void cleanupDeletedSmartDataTable(SQLiteDatabase db) {
        dbgTrace();

        String tableName = getSmartDataTableName() + "_delete";

        dbgTrace("Dropping table: " + tableName);
        String DELETE_SDATA_TABLE = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(DELETE_SDATA_TABLE);
    }

    private void createTables(SQLiteDatabase db) {
        dbgTrace();

        String tableName;

        // create generic data tables
        String[] genCatStrings = getAllGenCatStrings();
        for (String gencatString : genCatStrings) {
            tableName = genCatStringToTableName(gencatString);
            createGenDataTable(db, tableName);
        }

        // create smart data table (only one table)
        createSmartDataTable(db);
    }

    private void cleanupDeletedTables(SQLiteDatabase db) {
        dbgTrace();

        String tableName;

        // create generic data tables
        String[] genCatStrings = getAllGenCatStrings();
        for (String gencatString : genCatStrings) {
            tableName = genCatStringToTableName(gencatString);
            cleanupDeletedGenDataTable(db, tableName);
        }

        // create smart data table (only one table)
        cleanupDeletedSmartDataTable(db);
    }

    private ContentValues getAsContentValuesForMainTable(MMLGenericDataDesc desc) {
        ContentValues values = new ContentValues();

        values.put(COL_GDATA_PHONE_PATH, desc.mPath);
        values.put(COL_GDATA_MEEM_PATH, desc.mMeemInternalPath);
        values.put(COL_GDATA_SIZE, desc.mSize);
        values.put(COL_GDATA_CSUM, desc.mCSum);
        values.put(COL_GDATA_IOS_CSUM, "");
        values.put(COL_GDATA_MOD_TIME, desc.mModTime);
        values.put(COL_GDATA_FROM_SDCARD, desc.onSdCard);

        values.put(COL_GDATA_FW_ACK, 0); // Important

        boolean delFromMeem = false;
        if (desc.mIsMirror && desc.mIsDeleted) {
            delFromMeem = true;
        }

        values.put(COL_GDATA_FW_PRIV, delFromMeem ? 1 : 0);

        if (desc.mThumbNail != null) {
            values.put(COL_GDATA_THUMB_IMAGE, desc.mThumbNail);
        }

        values.put(COL_GDATA_BACKUP_MODE, desc.mIsMirror ? 0 : 1);

        return values;
    }

    private ContentValues getAsContentValuesForExtTable(MMLGenericDataDesc desc) {
        ContentValues values = new ContentValues();

        values.put(COL_GDATA_PHONE_PATH, desc.mPath);
        values.put(COL_GDATA_MEEM_PATH, desc.mMeemInternalPath);
        values.put(COL_GDATA_FW_ACK, 0); // Important

        boolean delFromMeem = false;
        if (desc.mIsMirror && desc.mIsDeleted) {
            delFromMeem = true;
        }

        values.put(COL_GDATA_FW_PRIV, delFromMeem ? 1 : 0);

        return values;
    }

    private ContentValues getAsContentValuesForMainTable(MMLSmartDataDesc desc) {
        ContentValues values = new ContentValues();

        values.put(COL_SDATA_CTYPE, desc.mCatCode);
        values.put(COL_SDATA_BACKUP_TIME, "");
        values.put(COL_SDATA_SYNC_PHONE_PATH, desc.mPaths[0]);
        values.put(COL_SDATA_SYNC_MEEM_PATH, desc.mMeemPaths[0]);
        values.put(COL_SDATA_SYNC_SIZE, desc.mSizes[0]);
        values.put(COL_SDATA_SYNC_CSUM, desc.mCSums[0]);

        values.put(COL_SDATA_ARCH_PHONE_PATH, desc.mPaths[1]);
        values.put(COL_SDATA_ARCH_MEEM_PATH, desc.mMeemPaths[1]);
        values.put(COL_SDATA_ARCH_SIZE, desc.mSizes[1]);
        values.put(COL_SDATA_ARCH_CSUM, desc.mCSums[1]);

        values.put(COL_SDATA_SYNC_FW_ACK, 0);
        values.put(COL_SDATA_ARCH_FW_ACK, 0);

        values.put(COL_SDATA_RSVD1, "");
        values.put(COL_SDATA_RSVD2, 0);

        return values;
    }

    // ===================================================================
    // Public methods
    // ===================================================================
    public void sanitize() {
        dbgTrace();

        SQLiteDatabase db = this.getWritableDatabase();
        cleanupDeletedTables(db);
        createTables(db);
        db.close();
    }

    /**
     * Note: Here we are not using the checksum matching - instead using path which is reasonably unique for a given upid and type.
     * Checksum calculation can take ages which we definitely do not want for a product like MEEM!
     * <p>
     * Always remember! ACK check is always done in extended table (_t table)
     *
     * @param desc
     *
     * @return
     */
    public boolean checkPresenceAndGetAckStatus(MMLGenericDataDesc desc) {
        dbgTrace();

        boolean result = false;

        String extTableName = genCatCodeToTableName(desc.mCatCode) + "_t";
//        String query = "SELECT * FROM " + mainTableName + " WHERE " + COL_GDATA_SIZE + " = " + desc.mSize + " AND " + COL_GDATA_PHONE_PATH + " = ?" + " AND " + COL_GDATA_FW_ACK + " = " + 1;
        String query = "SELECT * FROM " + extTableName + " WHERE " + COL_GDATA_PHONE_PATH + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{desc.mPath});
        if (cursor == null) {
            db.close();
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                // update the status. this is used for session size calculation.
                desc.mFwAck = ((1 == cursor.getInt(2)) ? true : false);
                result = true;
            }
        } catch (Exception ex) {
            Log.wtf("SecureDb", "Exception during checkPresenceAndGetAckStatus: " + ex.getMessage());
        }

        cursor.close();
        db.close();

        return result;
    }

    public boolean insert(MMLGenericDataDesc desc) {
        dbgTrace();
        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        String mainTableName = genCatCodeToTableName(desc.mCatCode);
        ContentValues mainValues = getAsContentValuesForMainTable(desc);

        String extTableName = mainTableName + "_t";
        ContentValues extValues = getAsContentValuesForExtTable(desc);

        try {
            if (-1 == db.insert(extTableName, null, extValues)) {
                dbgTrace("Failed to add item to ext table: " + desc);
                result = false;
            } else {
                if (-1 == db.insert(mainTableName, null, mainValues)) {
                    dbgTrace("Failed to add item to main table: " + desc);
                    result = false;
                }
            }
        } catch (Exception e) {
            dbgTrace("Exception while inserting into generic data tables: " + e.getMessage());
            result = false;
        }

        db.close();
        return result;
    }

    private long getExtTableItemRowId(SQLiteDatabase db, String tableName, String meemPath) {
        dbgTrace();

        long rowid = 0;

        String extQuery = "SELECT rowid,phone_path FROM " + tableName + " WHERE " + COL_GDATA_MEEM_PATH + " = ?";
        Cursor extCursor = db.rawQuery(extQuery, new String[]{meemPath});

        if (null == extCursor) {
            dbgTrace("Null cursor in ext generic data table!");
            return 0;
        }

        String phone_path = null;
        if (extCursor.moveToFirst()) {
            rowid = extCursor.getLong(0);
            phone_path = extCursor.getString(1);
        }

        extCursor.close();

        dbgTrace("meem_path: " + meemPath + ", rowid: " + rowid + ", phone_path: " + phone_path);

        return rowid;
    }

    /**
     * This is 50% of backup logic. Get all items belonging to the given category one by one
     * and update their status in a loop.
     *
     * @param catCode   : the category code
     * @param listener: usually backup logic object, from a thread.
     *
     * @return false on any error.
     */
    public boolean process(byte catCode, SecureDbProcessor listener, boolean fwAck) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        String mainTableName = genCatCodeToTableName(catCode);
        String extTableName = mainTableName + "_t";

        String extQuery = "SELECT meem_path FROM " + extTableName + " WHERE " + COL_GDATA_FW_ACK + " = ?";
        Cursor extCursor = db.rawQuery(extQuery, new String[]{fwAck ? "1" : "0"});

        if (null == extCursor) {
            dbgTrace("Null cursor in ext generic data table!");
            db.close();
            return false;
        }

        int rowid, count = 0, totalCount = extCursor.getCount();
        String meemPath;
        MMLGenericDataDesc genDesc;

        try {
            if (extCursor.moveToFirst()) {
                do {
                    count++;
                    SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, count, totalCount, catCode, SessionCommentary.OPMODE_PROCESSING_MEEM_ITEMS);
                    commentary.post();

                    meemPath = extCursor.getString(0);
                    rowid = (int) getExtTableItemRowId(db, extTableName, meemPath);

                    // query the gen data table for further details of backed up data
                    String mainQuery = "SELECT * FROM " + mainTableName + " WHERE " + COL_GDATA_MEEM_PATH + " = ?";
                    Cursor mainCursor = db.rawQuery(mainQuery, new String[]{meemPath});

                    if (mainCursor == null) {
                        dbgTrace("Null cursor in main generic data table!");
                        continue;
                    }

                    // take only first data - meem path is generated using time of backup with micro secs precision.
                    // So, it is assumed to be unique.
                    if (mainCursor.moveToFirst()) {
                        genDesc = new MMLGenericDataDesc();

                        genDesc.mPath = mainCursor.getString(0);
                        genDesc.mMeemInternalPath = mainCursor.getString(1);
                        genDesc.mSize = mainCursor.getLong(2);
                        genDesc.mCSum = mainCursor.getString(3);
                        // column 4 ignored
                        genDesc.mModTime = mainCursor.getLong(5);
                        genDesc.onSdCard = (1 == mainCursor.getInt(6)) ? true : false;

                        genDesc.mThumbNail = mainCursor.getBlob(9);
                        genDesc.mIsMirror = (0 == mainCursor.getInt(10)) ? true : false;

                        genDesc.mCatCode = catCode;
                        genDesc.mRowId = rowid;

                        /**
                         * By default, backup_mode field in securedb is [mirror/sync]. When an item is found to be deleted,
                         * if its backup mode is [plus/archive] (the cat is in plusmask), then [backup_mode] field shall be
                         * set to 1.
                         */
                        Boolean res = listener.onSecureDbItemForProcessing(genDesc);
                        if (null == res) {
                            dbgTrace("Aborting: update callback returns null");
                            result = false;
                            break;
                        } else if (res) {
                            // Update this row in main table using CSUM as criteria - which is unique.
                            // TODO: What about same files in 2 folders in phone? Better use meem path also? Too tricky to change now!
                            ContentValues values = getAsContentValuesForMainTable(genDesc);
                            db.update(mainTableName, values, COL_GDATA_CSUM + " = ?", new String[]{genDesc.mCSum});

                            // Arun: Bugfix: 03July2018: Update ext table too! Getting this bug after 2 years! What crappy testing we are doing...
                            if(genDesc.mIsMirror && genDesc.mIsDeleted) {
                                // Update this row in main table using MEEM_PATH as criteria - which is unique.
                                ContentValues extValues = getAsContentValuesForExtTable(genDesc);
                                db.update(extTableName, extValues, COL_GDATA_MEEM_PATH + " = ?", new String[]{genDesc.mMeemInternalPath});
                            }
                        }
                    } else {
                        dbgTrace("BUG! No main table item found for ext table meem_path: " + meemPath);
                        result = false;
                    }
                    mainCursor.close();
                } while (extCursor.moveToNext());

                dbgTrace("Cleaning up deleted items from main table");
                db.delete(mainTableName, COL_GDATA_FW_PRIV + " = 1", null);
            }
        } catch (Exception ex) {
            dbgTrace("Exception during processing tables: " + mainTableName + ": " + ex.getMessage());
            result = false;
        }

        extCursor.close();
        db.close();

        listener.onSecureDbProcessingComplete();

        return result;
    }

    /**
     * Used by sessions to step through files to send/receive.
     *
     * @param catCode
     * @param listener
     * @param fwAck
     *
     * @return
     */
    public boolean scan(byte catCode, SecureDbScanner listener, boolean fwAck) {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();

        String mainTableName = genCatCodeToTableName(catCode);
        String extTableName = mainTableName + "_t";

        String extQuery = "SELECT meem_path FROM " + extTableName + " WHERE " + COL_GDATA_FW_ACK + " = ?";
        Cursor extCursor = db.rawQuery(extQuery, new String[]{fwAck ? "1" : "0"});

        if (null == extCursor) {
            dbgTrace("Null cursor in ext generic data table!");
            db.close();
            return false;
        }

        int totalCount = extCursor.getCount();
        listener.onTotalItemCountForScannedCat(catCode, totalCount);

        int rowid;
        String meemPath;
        MMLGenericDataDesc genDesc;

        try {
            if (extCursor.moveToFirst()) {
                do {
                    meemPath = extCursor.getString(0);
                    rowid = (int) getExtTableItemRowId(db, extTableName, meemPath);

                    // query the gen data table for further details of backed up data
                    String mainQuery = "SELECT * FROM " + mainTableName + " WHERE " + COL_GDATA_MEEM_PATH + " = ?";
                    Cursor mainCursor = db.rawQuery(mainQuery, new String[]{meemPath});

                    if (mainCursor == null) {
                        dbgTrace("Null cursor in main generic data table!");
                        continue;
                    }

                    // take only first data - meem path is generated using time of backup with micro secs precision.
                    // So, it is assumed to be unique.
                    if (mainCursor.moveToFirst()) {
                        genDesc = new MMLGenericDataDesc();

                        genDesc.mPath = mainCursor.getString(0);
                        genDesc.mMeemInternalPath = mainCursor.getString(1);
                        genDesc.mSize = mainCursor.getLong(2);
                        genDesc.mCSum = mainCursor.getString(3);
                        // column 4 ignored
                        genDesc.mModTime = mainCursor.getLong(5);
                        genDesc.onSdCard = (1 == mainCursor.getInt(6)) ? true : false;

                        genDesc.mThumbNail = mainCursor.getBlob(9);
                        genDesc.mIsMirror = (0 == mainCursor.getInt(10)) ? true : false;

                        genDesc.mCatCode = catCode;
                        genDesc.mRowId = rowid;

                        /**
                         * By default, backup_mode field in securedb is [mirror/sync]. When an item is found to be deleted,
                         * if its backup mode is [plus/archive] (the cat is in plusmask), then [backup_mode] field shall be
                         * set to 1.
                         */
                        Boolean res = listener.onSecureDbItemWhileScanning(genDesc);
                        if (null == res) {
                            dbgTrace("Aborting: scan result callback returns null");
                            result = false;
                            break;
                        }
                    } else {
                        dbgTrace("BUG! No main table item found for ext table meem_path: " + meemPath + " (ignored)");
                        /*result = false;*/ // TODO: Arun: 14June2017: Decided to ignore this.
                    }
                    mainCursor.close();
                } while (extCursor.moveToNext());
            }
        } catch (Exception ex) {
            dbgTrace("Exception during processing table: " + mainTableName + ": " + ex.getMessage());
            result = false;
        }

        extCursor.close();
        db.close();

        listener.onSecureDbScanningComplete(catCode);

        return result;
    }

    /**
     * Unused.
     *
     * @param catCode
     * @param ackStatus
     *
     * @return
     */
    public long getTotalSizeFor(byte catCode, boolean ackStatus) {
        dbgTrace();

        long size = 0;

        SQLiteDatabase db = this.getReadableDatabase();

        String mainTableName = genCatCodeToTableName(catCode);
        String extTableName = mainTableName + "_t";

        String extQuery = "SELECT meem_path FROM " + extTableName + " WHERE " + COL_GDATA_FW_ACK + " = ?";
        Cursor extCursor = db.rawQuery(extQuery, new String[]{ackStatus ? "1" : "0"});

        if (extCursor == null) {
            db.close();
            return size;
        }

        String meemPath = null;

        if (extCursor.moveToFirst()) {
            do {
                meemPath = extCursor.getString(0);

                // query the gen data table for further details of backed up data
                String mainQuery = "SELECT size FROM " + mainTableName + " WHERE " + COL_GDATA_MEEM_PATH + " = ?";
                Cursor mainCursor = db.rawQuery(mainQuery, new String[]{meemPath});

                if (mainCursor == null) continue;

                if (mainCursor.moveToFirst()) {
                    size += mainCursor.getLong(0);
                }

                mainCursor.close();
            } while (extCursor.moveToNext());
        }

        extCursor.close();
        db.close();

        return size;
    }

    // ===================================================================
    // Smart data support
    // ===================================================================
    public boolean updateSmartDataTable(MMLSmartDataDesc desc) {
        dbgTrace();

        boolean result = false;

        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = getSmartDataTableName();

        ContentValues values = getAsContentValuesForMainTable(desc);

        if (db.update(tableName, values, "ctype = ?", new String[]{String.valueOf(desc.mCatCode)}) > 0) {
            dbgTrace("Smart cat row updated ok");
            result = true;
        } else {
            long id = db.insert(tableName, null, values);
            if (-1 != id) {
                dbgTrace("Smart cat row inserted ok: " + id);
                result = true;
            } else {
                dbgTrace("Smart cat row insert failed!");
                result = false;
            }
        }

        return result;
    }

    // ===================================================================
    // Vault delete / reset support
    // ===================================================================
    public boolean dropAllTables() {
        dbgTrace();

        boolean result = true;

        SQLiteDatabase db = this.getWritableDatabase();
        String tableName;

        try {
            // drop generic data tables
            String[] genCatStrings = getAllGenCatStrings();
            for (String gencatString : genCatStrings) {
                tableName = genCatStringToTableName(gencatString);
                db.execSQL("DROP TABLE IF EXISTS " + tableName);

                // Very important to drop _t tables
                String extTableName = tableName + "_t";
                db.execSQL("DROP TABLE IF EXISTS " + extTableName);
            }

            // drop smart data table (only one table)
            tableName = getSmartDataTableName();
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        } catch (Exception e) {
            dbgTrace("Exception during drop: " + e.getMessage());
            result = false;
        }

        db.close();
        return result;
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logCat("SecureDb", trace);
        GenUtils.logMessageToFile("SecureDb.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
