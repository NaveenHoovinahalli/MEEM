package com.meem.androidapp;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public class ContactTrackerDataBase extends ContentProvider {
    public static final String _ID = "_id";
    public static final String CONTACT_RAW_ID = "raw_id";
    public static final String CONTACT_STATUS = "contact_status";
    public static final String VERSION_CODE = "version_code";
    // Table for contact tracking
    static final String PROVIDER_NAME = "com.meem.silverlining.contacts";
    static final String URL = "content://" + PROVIDER_NAME + "/call";
    public static final Uri CONTENT_URI = Uri.parse(URL);
    static final int CALL = 1;
    static final int CALL_ID = 2;
    static final UriMatcher uriMatcher;
    static final String DATABASE_NAME = "ContactTrack";
    static final String CALLDATA_TABLE_NAME = "Contact";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE = " CREATE TABLE " + CALLDATA_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + "  version_code , " + " contact_status , " + " raw_id unique ON CONFLICT replace );";
    private static HashMap<String, String> CALLDATA_PROJECTION_MAP;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "call", CALL);
        uriMatcher.addURI(PROVIDER_NAME, "call/#", CALL_ID);
    }

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        /**
         * Create a write able database which will trigger its creation if it
         * doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();
        return (db == null) ? false : true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /**
         * Add a new call record
         */
        long rowID = db.insert(CALLDATA_TABLE_NAME, "", values);
        /**
         * If record is added successfully
         */
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CALLDATA_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case CALL:
                qb.setProjectionMap(CALLDATA_PROJECTION_MAP);
                break;
            case CALL_ID:
                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder.equals("")) {
            sortOrder = _ID;
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case CALL:
                count = db.delete(CALLDATA_TABLE_NAME, selection, selectionArgs);
                break;
            case CALL_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete(CALLDATA_TABLE_NAME, _ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case CALL:
                count = db.update(CALLDATA_TABLE_NAME, values, selection, selectionArgs);
                break;
            case CALL_ID:
                count = db.update(CALLDATA_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            /**
             * Get all student records
             */
            case CALL:
                return "vnd.android.cursor.dir/vnd.example.call";
            /**
             * Get a particular student
             */
            case CALL_ID:
                return "vnd.android.cursor.item/vnd.example.call";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    /**
     * Helper class that actually creates and manages the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + CALLDATA_TABLE_NAME);
            onCreate(db);
        }
    }
}