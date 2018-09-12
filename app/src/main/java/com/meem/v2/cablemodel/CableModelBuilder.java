package com.meem.v2.cablemodel;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.meem.androidapp.AppLocalData;
import com.meem.mmp.mml.MMLCategory;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by SCS on 3/27/2017 as FWDatabaseInfo
 * Updated by arun on 24/5/17 and remaned as CableModelBuilder
 * <p>
 * TODO: Arun: Move all sanitizing to CableDriverV2 for all UPIDs in configdb.
 */

public class CableModelBuilder {
    private MeemDbModel meemDBModel;

    private DebugTracer mDebug = new DebugTracer("CableModelBuilder", "CableModelBuilder.log");

    private ArrayList<PhoneDbModel> phoneDBModelList = new ArrayList<>();
    private ArrayList<VaultDbModel> vaultDbModelList = new ArrayList<>();

    private SQLiteDatabase db, db2;
    private int gencategoryMaskMirror = 0, smartCategoryMaskMirror = 0, gencategoryMaskPlus = 0, smartCategoryMaskPlus = 0;

    private CableInfo mCableInfo;

    private Long mMirrorSize, mPlusSize;

    private String mSerialNo, mFwVersion, mPhoneUpid;

    public CableModelBuilder(String serialNo, String fwVersion, String phoneUpid) {
        dbgTrace();

        mSerialNo = serialNo;
        mFwVersion = fwVersion;
        mPhoneUpid = phoneUpid;

        processConfigDb();//To convert DB data into object
        sanitizeSecureDb(); // To create tables for upids which are present in configdb.
        createCableInfo(); //To create cableinfo object from the model
    }

    /**
     * This is the only method to be used by external classes to get the cable view-model from downloaded databases.
     *
     * @return
     */
    public CableInfo getCableInfo() {
        return mCableInfo;
    }

    private void processConfigDb() {
        dbgTrace();

        db = openDatabase(AppLocalData.getInstance().getConfigDbFullPath());

        if (!db.isOpen()) {
            dbgTrace("DB Open failed");
            return;
        } else {
            dbgTrace("DB Open sucess");

        }

        //Fetch MEEM info
        String query = "select * from meem";
        Cursor cursor = safeRawQuery(db, query, null);

        if (cursor == null) {
            // almost impossible - but lets retrun an empty cable
            meemDBModel = new MeemDbModel();
            return;
        }

        meemDBModel = new MeemDbModel();

        if (cursor.moveToFirst()) {
            meemDBModel.mItem = Integer.parseInt(cursor.getString(0));
            meemDBModel.mName = cursor.getString(1);
            meemDBModel.mCrypto = Integer.parseInt(cursor.getString(2));
            meemDBModel.mTotal_mb = Integer.parseInt(cursor.getString(3));
            meemDBModel.mFree_mb = Integer.parseInt(cursor.getString(4));
            meemDBModel.mUsed_mb = Integer.parseInt(cursor.getString(5));
            meemDBModel.mReserve_mb = Integer.parseInt(cursor.getString(6));
            meemDBModel.mNum_vaults = Integer.parseInt(cursor.getString(7));
            meemDBModel.mdbinit = Integer.parseInt(cursor.getString(8));
        }

        cursor.close();

        //FetchPhoneinfo
        String phoneQuery = "select * from pinf";
        Cursor phoneCursor = safeRawQuery(db, phoneQuery, null);
        if (phoneCursor == null) {
            return;
        }

        while (phoneCursor.moveToNext()) {
            PhoneDbModel phoneDBModel = new PhoneDbModel();

            phoneDBModel.mUpid = phoneCursor.getString(0);
            phoneDBModel.mName = phoneCursor.getString(1);
            phoneDBModel.mOpetr = phoneCursor.getString(2);
            phoneDBModel.mLang = phoneCursor.getString(3);
            phoneDBModel.mPltfrm = phoneCursor.getString(4);
            phoneDBModel.mVer = phoneCursor.getString(5);
            phoneDBModel.mBrand = phoneCursor.getString(6);
            phoneDBModel.mMod_name = phoneCursor.getString(7);
            phoneDBModel.mSize = Integer.parseInt(phoneCursor.getString(8));

            // care against duplicate entries (may be due to crashes? TODO: investigate)
            if (!phoneDBModelList.contains(phoneDBModel)) {
                phoneDBModelList.add(phoneDBModel);
            } else {
                dbgTrace("Warning: pinf table contains a duplicate entry for upid: " + phoneDBModel.mUpid);
            }
        }

        phoneCursor.close();

        //FetchVaultinfo
        String vaultQuery = "select * from vault";
        Cursor vaultCursor = safeRawQuery(db, vaultQuery, null);
        if (vaultCursor == null) {
            return;
        }

        while ((vaultCursor.moveToNext())) {
            VaultDbModel vaultDbModel = new VaultDbModel();

            vaultDbModel.mUpid = vaultCursor.getString(0);
            vaultDbModel.mIs_migration = Integer.parseInt(vaultCursor.getString(1));
            vaultDbModel.mName = vaultCursor.getString(2);
            vaultDbModel.mBackup_time = Integer.parseInt(vaultCursor.getString(3));
            vaultDbModel.mBackup_status = Integer.parseInt(vaultCursor.getString(4));
            vaultDbModel.mRestore_time = Integer.parseInt(vaultCursor.getString(5));
            vaultDbModel.mRestore_status = Integer.parseInt(vaultCursor.getString(6));
            vaultDbModel.mCopy_time = Integer.parseInt(vaultCursor.getString(7));
            vaultDbModel.mCopy_status = Integer.parseInt(vaultCursor.getString(8));
            vaultDbModel.mSync_time = Integer.parseInt(vaultCursor.getString(9));
            vaultDbModel.mSync_status = Integer.parseInt(vaultCursor.getString(10));
            vaultDbModel.mBackup_mode = Integer.parseInt(vaultCursor.getString(11));
            vaultDbModel.mSound = Integer.parseInt(vaultCursor.getString(12));
            vaultDbModel.mAuto = Integer.parseInt(vaultCursor.getString(13));

            if (!vaultDbModelList.contains(vaultDbModel)) {
                vaultDbModelList.add(vaultDbModel);
            } else {
                dbgTrace("Warning: vault table contains a duplicate entry for upid: " + vaultDbModel.mUpid);
            }
        }

        vaultCursor.close();
        db.close();
    }

    private void sanitizeSecureDb() {
        mDebug.trace();

        for (VaultDbModel vaultdDbModel : vaultDbModelList) {
            SecureDb secureDbInstance = new SecureDb(vaultdDbModel.mUpid);
            secureDbInstance.sanitize();
            secureDbInstance.close(); // Arun: 21June2017: To resolve "Cursor finalized without prior close()" warning.
        }
    }

    private void createCableInfo() {
        dbgTrace();

        mCableInfo = new CableInfo();

        mCableInfo.mName = meemDBModel.mName;
        mCableInfo.mCapacityKB = meemDBModel.mTotal_mb * 1024; // views expects everything in KB
        mCableInfo.mFreeSpaceKB = meemDBModel.mFree_mb * 1024; // views expects everything in KB
        mCableInfo.fwVersion = mFwVersion;
        mCableInfo.mSerialNo = mSerialNo.toUpperCase();

        mCableInfo.mNumVaults = meemDBModel.mNum_vaults;

        mCableInfo.mVaultInfoMap = createVaultMap();
    }

    private String getPhonePlatform(String upid) {
        dbgTrace();

        String platform = "Android";

        for(PhoneDbModel phoneDbModel : phoneDBModelList) {
            if(phoneDbModel.mUpid.equals(upid)) {
                platform = phoneDbModel.mPltfrm;
                break;
            }
        }

        dbgTrace("Phone upid: " + upid + ", platform: " + platform);
        return platform;
    }

    private LinkedHashMap<String, VaultInfo> createVaultMap() {
        dbgTrace();

        LinkedHashMap<String, VaultInfo> vaultMap = new LinkedHashMap<String, VaultInfo>();

        for (int i = 0; i < vaultDbModelList.size(); i++) {
            VaultInfo vaultInfo = new VaultInfo();
            VaultDbModel vaultDBModel = vaultDbModelList.get(i);

            vaultInfo.mUpid = vaultDBModel.mUpid;
            vaultInfo.mName = vaultDBModel.mName;

            vaultInfo.mPlatform = getPhonePlatform(vaultInfo.mUpid);

            vaultInfo.mLastBackupTime = vaultDBModel.mBackup_time;
            vaultInfo.mCategoryInfoMap = createCatMap(vaultDBModel.mUpid);

            vaultInfo.mMirrorSizeKB = mMirrorSize;
            vaultInfo.mPlusSizeKB = mPlusSize;
            dbgTrace("Mirror size: " + mMirrorSize);
            dbgTrace("Plus size: " + mPlusSize);

            vaultInfo.mGenMirrorCatMask = gencategoryMaskMirror;
            vaultInfo.mSmartMirrorCatMask = smartCategoryMaskMirror;
            vaultInfo.mGenPlusCatMask = gencategoryMaskPlus;
            vaultInfo.mSmartPlusCatMask = smartCategoryMaskPlus;

            // This is important
            if (mPhoneUpid.toLowerCase().equals(vaultInfo.mUpid.toLowerCase())) {
                vaultInfo.mIsMirror = true;
            }

            dbgTrace("#" + i + ": " + vaultInfo.toString());

            vaultMap.put(vaultDBModel.mUpid, vaultInfo);
        }

        return vaultMap;
    }

    private LinkedHashMap<Byte, CategoryInfo> createCatMap(String mUpid) {
        dbgTrace();

        mMirrorSize = 0L;
        mPlusSize = 0L;

        gencategoryMaskMirror = 0;
        smartCategoryMaskMirror = 0;
        gencategoryMaskPlus = 0;
        smartCategoryMaskPlus = 0;

        ArrayList<String> catListGenMirror = new ArrayList<>();
        ArrayList<String> catListSmartMirror = new ArrayList<>();
        ArrayList<String> catListGenPlus = new ArrayList<>();
        ArrayList<String> catListSmartPlus = new ArrayList<>();

        LinkedHashMap<Byte, CategoryInfo> catMap = new LinkedHashMap<Byte, CategoryInfo>();

        db = openDatabase(AppLocalData.getInstance().getConfigDbFullPath());

        String catsTable = "_" + mUpid + "_category";
        String query = "select * from " + catsTable;
        Cursor catCursor = safeRawQuery(db, query, null);
        if (catCursor == null) {
            return catMap;
        }

        while (catCursor.moveToNext()) {
            byte catCode = (byte) catCursor.getInt(0);

            int sync_enabled = catCursor.getInt(1);
            int archive_enabled = catCursor.getInt(2);

            CategoryInfo categoryInfo = new CategoryInfo();
            categoryInfo.mMmpCode = catCode;

            if (sync_enabled == 1 && archive_enabled == 1) {
                categoryInfo.mBackupMode = CategoryInfo.BackupMode.PLUS;
            } else if (sync_enabled == 1) {
                categoryInfo.mBackupMode = CategoryInfo.BackupMode.MIRROR;
            } else {
                categoryInfo.mBackupMode = CategoryInfo.BackupMode.DISABLED;
            }

            String sqlCatName, mmlCatName;

            if (MMLCategory.isSmartCategoryCode(catCode)) {
                mmlCatName = MMLCategory.toSmartCatString(catCode);
                sqlCatName = GenUtils.sanitizeCatNameForSqLite(mmlCatName);

                db2 = openDatabase(AppLocalData.getInstance().getSecureDbFullPath());

                if (!db.isOpen()) {
                    dbgTrace("DB Open secure failed");
                } else {
                    dbgTrace("DB Open secure  sucess");

                }

                String table = "_" + mUpid + "_smartdata";
                String querysecure = "select * from " + table + " where ctype = " + String.valueOf(catCode);

                Cursor secureCursor = safeRawQuery(db2, querysecure, null);
                if (secureCursor == null) {
                    continue;
                }

                if (secureCursor.moveToFirst()) {
                    categoryInfo.mSmartMirrorMeemPath = secureCursor.getString(3);
                    categoryInfo.mMirrorSizeKB = secureCursor.getInt(4) / 1024; // smart data size is in bytes in securedb
                    categoryInfo.mSmartMirrorCSum = secureCursor.getString(5);

                    categoryInfo.mSmartPlusMeemPath = secureCursor.getString(7);
                    categoryInfo.mPlusSizeKB = secureCursor.getInt(8) / 1024; // smart data size is in bytes in securedb
                    categoryInfo.mSmartPlusCSum = secureCursor.getString(9);

                    categoryInfo.mSmartMirrorAck = (1 == secureCursor.getInt(10)) ? true : false;
                    categoryInfo.mSmartPlusAck = (1 == secureCursor.getInt(11)) ? true : false;
                }

                dbgTrace("SmartCat: " + categoryInfo.toString());

                secureCursor.close();
                db2.close();
            } else {
                mmlCatName = MMLCategory.toGenericCatString(catCode);
                sqlCatName = GenUtils.sanitizeCatNameForSqLite(mmlCatName);

                db2 = openDatabase(AppLocalData.getInstance().getSecureDbFullPath());

                String mainTableName = "_" + mUpid + "_" + sqlCatName + "_genericdata";
                String extTableName = mainTableName + "_t";

                // select only those rows which are ack'ed by firmware
                String extQuery = "select * from " + extTableName + " where fw_ack = 1";
                Cursor extCursor = safeRawQuery(db2, extQuery, null);
                if (extCursor == null) {
                    continue;
                }

                if (null == extCursor) {
                    dbgTrace("Null cursor in ext generic data table!");
                    continue;
                }

                String meemPath;

                long totalMirrorSize = 0;
                long totalPlusSize = 0;

                String querySecure = "SELECT backup_mode,sum(size) from " + mainTableName + "  " +
                        "where meem_path in(select meem_path from " + extTableName + " where fw_ack=1) group by backup_mode";

                Cursor secureCursor = db2.rawQuery(querySecure, null);
                while (secureCursor.moveToNext()) {
                    int mode = secureCursor.getInt(0);
                    if (mode == 0) {
                        totalMirrorSize = secureCursor.getLong(1);
                    } else {
                        totalPlusSize = secureCursor.getLong(1);

                    }
                }
                secureCursor.close();


                // generic data size is in bytes in securedb
                categoryInfo.mMirrorSizeKB = totalMirrorSize / 1024;
                categoryInfo.mPlusSizeKB = totalPlusSize / 1024;

                dbgTrace("GenCat: " + categoryInfo.toString());

                extCursor.close();
                db2.close();
            }

            // update the vault size
            mMirrorSize = categoryInfo.mMirrorSizeKB + mMirrorSize;
            mPlusSize = categoryInfo.mPlusSizeKB + mPlusSize;

            // we do not show sdcard categories in the view.
            if (MMLCategory.isGenericCategoryCode(catCode)) {
                if (sync_enabled == 1 && archive_enabled == 1) {
                    catListGenMirror.add(mmlCatName);
                    catListGenPlus.add(mmlCatName);
                } else if (sync_enabled == 1) {
                    catListGenMirror.add(mmlCatName);
                }
            } else {
                if (sync_enabled == 1 && archive_enabled == 1) {
                    catListSmartMirror.add(mmlCatName);
                    catListSmartPlus.add(mmlCatName);
                } else if (sync_enabled == 1) {
                    catListSmartMirror.add(mmlCatName);
                }
            }

            catMap.put(catCode, categoryInfo);
        }

        catCursor.close();
        db.close();

        smartCategoryMaskMirror = MMLCategory.getSmartCatMask(catListSmartMirror.toArray(new String[catListSmartMirror.size()]));
        smartCategoryMaskPlus = MMLCategory.getSmartCatMask(catListSmartPlus.toArray(new String[catListSmartPlus.size()]));

        gencategoryMaskMirror = MMLCategory.getGenericCatMask(catListGenMirror.toArray(new String[catListGenMirror.size()]));
        gencategoryMaskPlus = MMLCategory.getGenericCatMask(catListGenPlus.toArray(new String[catListGenPlus.size()]));

        return catMap;
    }

    private SQLiteDatabase openDatabase(String path) {
        dbgTrace("Opening database from path: " + path);
        return SQLiteDatabase.openOrCreateDatabase(path, null, null);
    }

    private Cursor safeRawQuery(SQLiteDatabase db, String query, String[] args) {
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(query, args);
        } catch (SQLiteException e) {
            dbgTrace("### safeRawQuery: Exception for query: " + query + " [selection args: " + args + " ]");
        }

        if (cursor == null) {
            dbgTrace("### safeRawQuery: NULL cursor returned for query: " + query + " [selection args: " + args + " ]");
        }

        return cursor;
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("CableModelBuilder.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
