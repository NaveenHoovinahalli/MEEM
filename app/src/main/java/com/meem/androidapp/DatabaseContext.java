package com.meem.androidapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Created by arun on 25/5/17.
 */

public class DatabaseContext extends ContextWrapper {

    private static final String TAG = "DatabaseContext";

    public DatabaseContext(Context base) {
        super(base);
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = AppLocalData.getInstance().getDatabaseFolderPath() + File.separator + name;
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db";
        }

        File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }

       /* if (Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, "getDatabasePath(" + name + ") = " + result.getAbsolutePath());
        }*/

        return result;
    }

    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name, mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        /*if (Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, "openOrCreateDatabase(" + name + ",,) = " + result.getPath());
        }*/
        return result;
    }
}
