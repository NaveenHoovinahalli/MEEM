package com.meem.v2.cablemodel;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.meem.utils.DebugTracer;

import java.util.ArrayList;

/**
 * Created by Naveen on 6/29/2018.
 */

public class ApplicationDB {

    String mPath;
    SQLiteDatabase db;

    private DebugTracer mDbg = new DebugTracer("ApplicationDB", "ApplicationDB.log");



    String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS APPDATA (  NAME TEXT , PKG TEXT , IMG TEXT , FLAG INTEGER )";


    ApplicationDB(String path, String dbname ){
        mPath=path+ dbname;
    }

    public  void  createDBandTable(){
        mDbg.trace();
        openOrCreateDB(mPath);
        createTable();
        db.close();

    }

    private void createTable() {
        mDbg.trace();

        db.execSQL(CREATE_TABLE);

    }

    public void insertValues(ArrayList<ApplicationListModel> applicationListModels){
        mDbg.trace();
        try {

            openOrCreateDB(mPath);
            for (ApplicationListModel applicationListModel : applicationListModels) {
                db.execSQL("insert into APPDATA (NAME ,PKG, IMG , FLAG )  values ( '" + applicationListModel.mName + "','" + applicationListModel.mPkg + "','" + applicationListModel.mImg + "','" + applicationListModel.mFlag + "')");
            }

            db.close();
        }catch (SQLException e){
            mDbg.trace("Exception: " + e.getMessage());

        }
    }

     public  void closeDB(){
        if (db.isOpen())
            db.close();
    }

    private void openOrCreateDB(String mPath) {
        mDbg.trace();
        db= SQLiteDatabase.openOrCreateDatabase(mPath,null,null);
    }
}
