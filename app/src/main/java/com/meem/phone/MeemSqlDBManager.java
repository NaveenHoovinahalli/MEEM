package com.meem.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.meem.utils.GenUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by hercules on 18/8/16.
 */
public class MeemSqlDBManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static ArrayList<String> tableNamesList;
    private final String tag = "MeemSqlDBManager";

    public MeemSqlDBManager(Context mContext, String dbFilepath, ArrayList<String> tbNamesArrList) {
        super(mContext, dbFilepath, null, DATABASE_VERSION);
        dbgTrace(dbFilepath);

        if (null != tbNamesArrList) {
            tableNamesList = tbNamesArrList;
        } else {
            dbgTrace("Table list is null");
        }
        dbgTrace();

    }

    // individual restore related contructor

    public MeemSqlDBManager(Context mContext, String dbFilepath) {
        super(mContext, dbFilepath, null, DATABASE_VERSION);
        dbgTrace();

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

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MeemSqlDBManager.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("MeemSqlDBManager.log");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        dbgTrace("DBCreation");
        for (String tbName : tableNamesList) {
            db.execSQL(tbName);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        dbgTrace();
    }

    public boolean add(ContentValues values, String tbName) {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();


            if (-1 == db.insertOrThrow(tbName, null, values)) {
                dbgTrace("Adding item failed");
                result = false;
            }
            db.close();
        } catch (Exception ex) {
            dbgTrace("SQLiteException add " + ex.getMessage());
        }
        return result;
    }

    public String rawQuerryGetString(String query, int column) {
        String result = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(query, null);


            if (null == cursor || cursor.getCount() == 0) {
                cursor.close();
                db.close();
                return result;
            }

            cursor.moveToNext();
            result = cursor.getString(column);

            cursor.close();
            db.close();

        } catch (Exception ex) {
            dbgTrace("SQL Exception: " + ex.getMessage());
        }
        return result;
    }

    public int rawQuerryGetInt(String query, int column) {
        dbgTrace();
        int result = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(query, null);


            if (null == cursor || cursor.getCount() == 0) {
                cursor.close();
                db.close();
                return result;
            }
            cursor.moveToNext();
            result = cursor.getInt(column);


            cursor.close();
            db.close();
        } catch (Exception ex) {
            dbgTrace("SQL Exception: " + ex.getMessage());
        }
        dbgTrace("res " + result);
        return result;
    }

    public boolean executeSqlStmt(String sql) {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL(sql);
            dbgTrace("SQL result: sucess");

            db.close();
        } catch (Exception ex) {
            dbgTrace("SQL Exception: " + ex.getMessage());
            result = false;
        }
        return result;
    }

}
