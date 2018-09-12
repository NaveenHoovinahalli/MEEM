package com.meem.v2.cablemodel;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.meem.mmp.mml.MMLCategory;
import com.meem.utils.DebugTracer;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.LinkedHashMap;

/**
 * Created by SCS on 4/17/2017.
 */

public class ConfigDb {
    private String mUpid;
    private String mDbPath;
    private SQLiteDatabase mDatabase;

    private boolean mCleanupPending;
    private String mCleanupPendingUpid;

    private DebugTracer mDbg = new DebugTracer("ConfigDb", "ConfigDb.log");

    public ConfigDb(String upid, String configDbPath) {
        mDbg.trace();

        mUpid = upid;
        mDbPath = configDbPath;
    }

    private void createMeemTable() {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS meem (item integer default 1, name text default meem, crypto integer default 1, total_mb integer default 0, free_mb integer default 0, used_mb integer default 0, reserve_mb  integer default 0, num_vaults integer default 0, rsvd1 integer default 0, rsvd2 text)");
            if (isVirgin()) {
                mDbg.trace("Virgin cable: initializing meem table");
                mDatabase.execSQL("insert into meem (item,name) values (1,'MEEM')");
            } else {
                mDbg.trace("Cable is not virgin");
            }
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    private void createPinfTable() {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS pinf (upid text, name text, opetr text, lang text, pltfrm text, ver text, brand text, mod_name text, size integer DEFAULT 0, rsvd1 integer DEFAULT 0, rsvd2 text)");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    private void createVaultTable() {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS vault (upid text, is_migration integer default 0, name text, backup_time integer default 0, backup_status integer default 0, restore_time integer default 0, restore_status integer default 0, copy_time integer default 0, copy_status integer default 0, sync_time integer default 0, sync_status integer default 0, backup_mode integer default 0, sound integer default 0, auto integer default 1, rsvd1 integer default 0, rsvd2 integer default 0, rsvd3 text)");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    private void createPendingCleanupTable() {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS vault_cleanup (id integer, upid text)");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    private void createCategoryTable(String upid) {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + "_" + upid + "_category" + " (cat_code integer, sync_enable integer default 1, archive_enable integer default 0)");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    private void createAppSettingsTable() {
        mDbg.trace();

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS app_settings (meem_key text, meem_value integer)");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    // ========================================================
    // Public methods
    // ========================================================

    public void sanitize() {
        mDbg.trace();

        createMeemTable();
        createPinfTable();
        createVaultTable();
        createPendingCleanupTable();
        createCategoryTable(mUpid);
        createAppSettingsTable();
    }

    /**
     * Must call this before calling any other methods.
     */
    public void openDatabase() {
        mDbg.trace();
        mDbg.trace("Database path: " + mDbPath);

        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDbPath, null, null);
        sanitize();
        checkForPendingCleanup();
    }

    private void checkForPendingCleanup() {
        mDbg.trace();

        if(0 == DatabaseUtils.queryNumEntries(mDatabase, "vault_cleanup")) {
            mDbg.trace("No pending cleanups");
            return;
        }

        String query = "select * from vault_cleanup";
        Cursor cursor = mDatabase.rawQuery(query, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                mCleanupPending = true;
                mCleanupPendingUpid = cursor.getString(1);
                break;
            }
            cursor.close();
        }

        if(mCleanupPending) {
            mDbg.trace("Cleanup pending for upid: " + mCleanupPendingUpid);
        } else {
            mDbg.trace("No pending cleanups");
        }
    }

    public void clearCleanupPending() {
        mDbg.trace();

        if(!mCleanupPending) {
            mDbg.trace("BUG: No pending cleanup status to clear!");
            return;
        }

        mDbg.trace("Deleting cleanup table contents");

        // Arun: 05Sept2017: we can use shortcut of deleting all entries
        // since firmware will reboot each time we delete a vault, unless something really bad happens,
        // there wont be more than one entry in vault_cleanup table.
        mDatabase.delete("vault_cleanup", null, null);
    }

    public boolean isCleanupPending() {
        return mCleanupPending;
    }

    public String getCleanupPendingUpid() {
        return mCleanupPendingUpid;
    }

    public boolean getPolicyIsNetworkSharingEnabled() {
        mDbg.trace();
        boolean result = false;
        Cursor cursor = null;

        try {
            if(null != (cursor = mDatabase.rawQuery("SELECT * FROM app_settings WHERE meem_key = 'nw_sharing_enabled'", null))) {
                if (cursor.moveToFirst()) {
                    result = (cursor.getInt(1) == 0) ? false : true;
                }

                cursor.close();
            }
        } catch (SQLiteException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }

        return result;
    }

    public void setPolicyIsNetworkSharingEnabled(boolean enable) {
        mDbg.trace();

        try {
            if(enable) {
                mDatabase.execSQL("INSERT OR IGNORE INTO app_settings (meem_key,meem_value) VALUES('nw_sharing_enabled',1)");
                mDatabase.execSQL("UPDATE app_settings SET meem_value=1 WHERE meem_key='nw_sharing_enabled'");
            } else {
                mDatabase.execSQL("INSERT OR IGNORE INTO app_settings (meem_key,meem_value) VALUES('nw_sharing_enabled',0)");
                mDatabase.execSQL("UPDATE app_settings SET meem_value=0 WHERE meem_key='nw_sharing_enabled'");
            }
        } catch (SQLiteException e) {
            mDbg.trace("Exception: " + e.getMessage());
        }
    }

    public void openDatabaseForImmediateCleanup() {
        mDbg.trace();
        mDbg.trace("Database path: " + mDbPath);

        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDbPath, null, null);
    }

    public boolean isVirgin() {
        mDbg.trace();
        return (0 == DatabaseUtils.queryNumEntries(mDatabase, "meem"));
    }

    /**
     * Must call this after using mDatabase.
     */
    public void closeDatabase() {
        mDbg.trace();
        if (mDatabase != null) mDatabase.close();
    }

    public boolean isPhoneInfoAvailable(String upid) {
        mDbg.trace();
        return (0 != DatabaseUtils.queryNumEntries(mDatabase, "pinf", "upid='" + upid + "'"));
    }

    public boolean isVaultInfoAvailable(String upid) {
        mDbg.trace();
        return (0 != DatabaseUtils.queryNumEntries(mDatabase, "vault", "upid='" + upid + "'"));
    }

    public void insertIntoPinfoTable(PhoneDbModel phoneDbModel) {
        mDbg.trace();

        mDatabase.execSQL("insert into pinf (upid,name,opetr,lang,pltfrm,ver,brand,mod_name) values ('" + phoneDbModel.mUpid + "','" + phoneDbModel.mName + "','" + phoneDbModel.mOpetr + "" +
                "','" + phoneDbModel.mLang + "','" + phoneDbModel.mPltfrm + "','" + phoneDbModel.mVer + "','" + phoneDbModel.mBrand + "','" + phoneDbModel.mMod_name + "')");
    }

    public void insertIntoVInfoTable(VaultDbModel vaultDbModel) {
        mDbg.trace();

        mDatabase.execSQL("insert into vault (upid,is_migration,name,backup_time,backup_status,restore_time,restore_status,copy_time,copy_status,sync_time,sync_status,backup_mode,sound,auto)" +
                " values ('" + vaultDbModel.mUpid + "'," + vaultDbModel.mIs_migration + ",'" + vaultDbModel.mName + "'," +
                "" + vaultDbModel.mBackup_time + "," + vaultDbModel.mBackup_status + "," + vaultDbModel.mRestore_time + "," + vaultDbModel.mRestore_status + "" +
                "," + vaultDbModel.mCopy_time + "," + vaultDbModel.mCopy_status + "," + vaultDbModel.mSync_time + "," + vaultDbModel.mSync_status + "," + vaultDbModel.mBackup_mode + "," + vaultDbModel.mSound + "," + vaultDbModel.mAuto + ")");

        // create the categories table for this phone (NOTE: by default, all categories are in sync mode)
        createCategoryTable(vaultDbModel.mUpid);

        mDbg.trace("Creating category table for: " + vaultDbModel.mUpid);

        byte[] smartCats = MMLCategory.getAllSmartCatCodes();
        for (byte cat : smartCats) {
            mDatabase.execSQL("insert into " + "_" + vaultDbModel.mUpid + "_category" + " (cat_code, sync_enable, archive_enable)" + " values ( " + cat + "," + 1 + "," + 0 + ")");
        }

        byte[] genCats = MMLCategory.getAllGenCatCodes();
        for (byte cat : genCats) {
            mDatabase.execSQL("insert into " + "_" + vaultDbModel.mUpid + "_category" + " (cat_code, sync_enable, archive_enable)" + " values ( " + cat + "," + 1 + "," + 0 + ")");
        }
    }

    public MeemDbModel getMeemInfo() {
        mDbg.trace();

        MeemDbModel meem = new MeemDbModel();

        String query = "select * from meem";
        Cursor cursor = mDatabase.rawQuery(query, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                meem.mName = cursor.getString(1);
                meem.mTotal_mb = cursor.getLong(3);
                meem.mFree_mb = cursor.getLong(4);
                meem.mUsed_mb = cursor.getLong(5);
                meem.mNum_vaults = cursor.getInt(7);
            }
            cursor.close();
        }

        return meem;
    }

    public int getNumVaults() {
        mDbg.trace();

        String query = "select * from meem";
        Cursor cursor = mDatabase.rawQuery(query, null);

        int numVaults = 0;

        if (cursor != null && cursor.moveToNext()) {
            numVaults = cursor.getInt(7);
        }

        if (cursor != null) cursor.close();

        return numVaults;
    }

    // ===================================================================
    // --- Setter functions, to be used by Cable driver v2.
    // ===================================================================
    public boolean setCableName(String name) {
        mDbg.trace();
        try {
            mDatabase.execSQL("update meem set name = '" + name + "' where item=1");
            return true;
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
            return false;
        }
    }

    public boolean updateVaultInfo(VaultInfo vaultInfo) {
        mDbg.trace();

        LinkedHashMap<Byte, CategoryInfo> catMap = vaultInfo.getmCategoryInfoMap();
        String newName = vaultInfo.getmName();

        try {
            mDatabase.execSQL("update vault set name = '" + newName + "' where upid = '" + vaultInfo.getUpid() + "'");
        } catch (SQLException e) {
            mDbg.trace("Exception: " + e.getMessage());
            return false;
        }

        for (Byte cat : catMap.keySet()) {
            CategoryInfo catInfo = catMap.get(cat);

            int mirr = 0, plus = 0;

            switch (catInfo.getmBackupMode()) {
                case DISABLED:
                    mirr = 0;
                    plus = 0;
                    break;
                case MIRROR:
                    mirr = 1;
                    plus = 0;
                    break;
                case PLUS:
                    mirr = 1;
                    plus = 1;
                    break;
            }

            mDbg.trace("new values for cat: " + cat + ", sync_enable: " + mirr + ", archive_enable: " + plus);
            String tableName = "_" + vaultInfo.getUpid() + "_category";

            try {
                mDatabase.execSQL("update " + tableName + " set sync_enable = " + mirr + ", archive_enable = " + plus + " where cat_code = " + cat);
            } catch (SQLException e) {
                mDbg.trace("Exception: " + e.getMessage());
                return false;
            }
        }

        mDbg.trace("Vault updated successfully: " + vaultInfo.getUpid());
        return true;
    }

    public boolean dropAllTables(String upid) {
        mDbg.trace();
        boolean result = true;

        try {
            String catTableName = "_" + upid + "_category";
            mDatabase.execSQL("DROP TABLE IF EXISTS " + catTableName);

            mDatabase.execSQL("DROP TABLE IF EXISTS vault");
            mDatabase.execSQL("DROP TABLE IF EXISTS pinf");
            mDatabase.execSQL("DROP TABLE IF EXISTS meem");
            mDatabase.execSQL("DROP TABLE IF EXISTS app_settings");
        } catch (Exception e) {
            mDbg.trace("Exception while dropping table: " + e.getMessage());
            result = false;
        }

        return result;
    }
}
