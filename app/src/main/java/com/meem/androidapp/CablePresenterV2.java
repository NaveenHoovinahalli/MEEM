package com.meem.androidapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Telephony;

import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.ui.GenDataInfo;
import com.meem.ui.SmartDataInfo;
import com.meem.utils.DebugTracer;
import com.meem.v2.cablemodel.CableModelBuilder;
import com.meem.v2.cablemodel.ConfigDb;
import com.meem.v2.cablemodel.SecureDb;
import com.meem.v2.mmp.MMPV2Constants;
import com.meem.v2.phone.CalendersV2;
import com.meem.v2.phone.ContactsV2;
import com.meem.v2.phone.MessagesV2;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by arun on 24/5/17.
 */

public class CablePresenterV2 extends CablePresenter {
    private DebugTracer mDbg = new DebugTracer("CablePresenterV2", "CablePresenterV2.log");

    private CableDriverV2 mCableDriverV2;

    private SmartDataSeletiveRestoreAsync seletiveRestoreAsync;

    public CablePresenterV2(String phoneUpid, CableDriver driver, CablePresenterHelper sessionHelper) {
        super(phoneUpid, driver, sessionHelper);

        mCableDriver = driver;
        mPhoneUpid = phoneUpid;
        mHelper = sessionHelper;

        mCableDriverV2 = (CableDriverV2) driver; // ugly!
    }

    @Override
    void createCableViewModel() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("Cable is disconnnected. Creating an empty cable model");
            mCableInfo = new CableInfo();
        } else {
            CableModelBuilder modelHelper = new CableModelBuilder(mCableDriver.getSerialNo(), mCableDriver.getFwVersion(), mPhoneUpid);
            mCableInfo = modelHelper.getCableInfo();
        }

        // Arun: 19Aug2017: Apply policies like "network slave can only see his vault in the app".
        applyViewModelPolicies();
    }


    @Override
    public void onVirginCablePinSetupEntry(final String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.setPIN(pin, recoveryAnswers, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mHelper.onCriticalError("PIN setup failed!");
                    return false;
                } else {
                    return responseCallback.execute(result, info, extraInfo);
                }
            }
        });
    }

    // Arun: 05July2018: Added the feature of pin recovery.
    @Override
    public void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.validateRecoveryAnswers(recoveryAnswers, responseCallback);
    }

    @Override
    boolean updateVault(VaultInfo vaultInfo, final ResponseCallback responseCallback) {
        mDbg.trace();

        // prepare category masks from category hashmaps of view model is the key operation here
        LinkedHashMap<Byte, CategoryInfo> catInfoMap = vaultInfo.getmCategoryInfoMap();
        for (Byte cat : catInfoMap.keySet()) {
            CategoryInfo catInfo = catInfoMap.get(cat);
            CategoryInfo.BackupMode curBkpMode = catInfo.getmBackupMode();

            // sdcard mapping!
            if (MMLCategory.isGenericCategoryCode(cat)) {
                if (!isSdCardMappedCat(cat)) {
                    byte sdCat = getSdCardMappedCat(cat);
                    CategoryInfo sdCatInfo = catInfoMap.get(sdCat);
                    sdCatInfo.setmBackupMode(curBkpMode);
                }
            }
        }

        return mCableDriver.updateVault(vaultInfo, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    createCableViewModel();
                }

                if (null != responseCallback) {
                    return responseCallback.execute(result, mCableInfo, extraInfo);
                }

                return result;
            }
        });
    }

    @Override
    public void onResetCable(final ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.resetCable(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("Cleaning up all databases");

                    LinkedHashMap<String, VaultInfo> vaultMap = mCableInfo.getmVaultInfoMap();
                    for (String upid : vaultMap.keySet()) {
                        mDbg.trace("Dropping tables in configdb for upid: " + upid);
                        ConfigDb configDb = new ConfigDb(upid, mAppLocaldata.getConfigDbFullPath());
                        configDb.openDatabaseForImmediateCleanup();
                        configDb.dropAllTables(upid);
                        configDb.closeDatabase();

                        mDbg.trace("Dropping tables in securedb for upid: " + upid);
                        SecureDb secureDb = new SecureDb(upid);
                        secureDb.dropAllTables();
                        secureDb.close();

                        mDbg.trace("Removing smartdata dbs for upid: " + upid);
                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_CONTACT, true)));
                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_CONTACT, false)));

                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_MESSAGE, true)));
                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_MESSAGE, false)));

                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_CALENDER, true)));
                        SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSmartDataV2DatabasePath(upid, MMPV2Constants.MMP_CATCODE_CALENDER, false)));
                    }

                    mDbg.trace("Removing config and secure databases (may fail, but they should be empty anyway)");
                    SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getConfigDbFullPath()));
                    SQLiteDatabase.deleteDatabase(new File(mAppLocaldata.getSecureDbFullPath()));
                }

                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    @Override
    public void onDeleteVault(final String vaultId, final ResponseCallback responseCallback) {
        mDbg.trace();

        mCableDriver.deleteVault(vaultId, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("Vault delete succeeded. Cable will reboot");
                    mCableInfo.getmVaultInfoMap().remove(vaultId);
                }

                return responseCallback.execute(result, mCableInfo, extraInfo);
            }
        });
    }

    @Override
    public void onRestoreGenData(final String vaultId, final Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {

                ArrayList<GenDataInfo> errItems = new ArrayList<>();

                for (GenDataInfo genDataInfo : genDataInfoList) {
                    MMLGenericDataDesc desc = new MMLGenericDataDesc();

                    // Arun: 15June2017: Must do sdcard mapping!
                    byte mappedCat = catCode;
                    if (MMLCategory.isGenericCategoryCode(catCode) && genDataInfo.isSdcard()) {
                        mappedCat = getSdCardMappedCat(catCode);
                    }

                    desc.mCatCode = mappedCat;

                    desc.mPath = genDataInfo.getfPath();
                    genDataInfo.setDestFPath(desc.mPath);

                    desc.onSdCard = genDataInfo.isSdcard();
                    desc.mMeemInternalPath = genDataInfo.getMeemPath();
                    desc.mCSum = genDataInfo.getcSum();

                    if (!mCableDriverV2.fetchGenericData(vaultId, desc, null)) {
                        errItems.add(genDataInfo);
                    }
                }

                for (GenDataInfo errItem : errItems) {
                    genDataInfoList.remove(errItem);
                }

                MeemEvent resEvent = new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, genDataInfoList, responseCallback);
                resEvent.setResult(!genDataInfoList.isEmpty());
                UiContext.getInstance().postEvent(resEvent);
            }
        });

        worker.start();

    }

    @Override
    public void onShareGenData(final String vaultId, final Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {

                ArrayList<GenDataInfo> errItems = new ArrayList<>();

                for (GenDataInfo genDataInfo : genDataInfoList) {
                    MMLGenericDataDesc desc = new MMLGenericDataDesc();

                    // Arun: 15June2017: Must do sdcard mapping!
                    byte mappedCat = catCode;
                    if (MMLCategory.isGenericCategoryCode(catCode) && genDataInfo.isSdcard()) {
                        mappedCat = getSdCardMappedCat(catCode);
                    }

                    desc.mCatCode = mappedCat;

                    desc.mPath = AppLocalData.getInstance().getTempFilePathFor_MeemV2(genDataInfo.getfPath());
                    desc.mIsTemp = true; // very important for temp files to set this before giving this to driverV2.

                    genDataInfo.setDestFPath(desc.mPath);

                    desc.onSdCard = genDataInfo.isSdcard();
                    desc.mMeemInternalPath = genDataInfo.getMeemPath();
                    desc.mCSum = genDataInfo.getcSum();

                    if (!mCableDriverV2.fetchGenericData(vaultId, desc, null)) {
                        errItems.add(genDataInfo);
                    }
                }

                for (GenDataInfo errItem : errItems) {
                    genDataInfoList.remove(errItem);
                }

                MeemEvent resEvent = new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, genDataInfoList, responseCallback);
                resEvent.setResult(!genDataInfoList.isEmpty());
                UiContext.getInstance().postEvent(resEvent);
            }
        });

        worker.start();
    }
    // ------------------------------------------------------------------------------
    // ---------------- Protected classes for smart data operations
    // ------------------------------------------------------------------------------


    @Override
    public void onRestoreSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, boolean isMirror, ResponseCallback responseCallback) {
        mDbg.trace();

        seletiveRestoreAsync = new SmartDataSeletiveRestoreAsync(catCode, smartDataInfoList, vaultId, isMirror, responseCallback);
        seletiveRestoreAsync.execute();
    }

    public void cancelSelectiveRestoreAsyncTasks() {
        mDbg.trace();
        if (null != seletiveRestoreAsync) {
            switch (seletiveRestoreAsync.getCatCode()) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    ContactsV2 aCon = seletiveRestoreAsync.getContacts();
                    if (null != aCon) aCon.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Contacts JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    CalendersV2 aCal = seletiveRestoreAsync.getCalenders();
                    if (null != aCal) aCal.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Calenders JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    MessagesV2 aMsg = seletiveRestoreAsync.getMessages();
                    if (null != aMsg) aMsg.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Messages JsonToSql processing Aborted");
                    break;
                default:
                    mDbg.trace("onCancelled :In-Valid Category Code");

            }
        }
    }

    @Override
    public void onUpdateVault(VaultInfo vault, ResponseCallback responseCallback) {
        mDbg.trace();
        updateVault(vault, responseCallback);
    }

    @Override
    public void onUpdateProfileName(String vaultId, String newName, ResponseCallback responseCallback) {
        // TODO
        responseCallback.execute(true, null, null);
    }

    @Override
    public void onFirmwareUpdate(String id, ResponseCallback responseCallback) {
        mCableDriver.updateFirmware(id, responseCallback);
    }

    // ------------------------------------------------------------------------------
    // ---------------- Protected classes for smart data operations
    // ------------------------------------------------------------------------------

    protected class SmartDataSeletiveRestoreAsync extends AsyncTask<Void, Void, Boolean> {

        private Context appCtxt = UiContext.getInstance().getAppContext();
        private Byte mCatCode;
        private ArrayList<SmartDataInfo> mSmartDataInfoList;
        private ResponseCallback mResponseCallback;
        private Boolean res = true;
        private String mVaultId;
        private Boolean mIsMirr = true;

        private ContactsV2 mCon;
        private CalendersV2 mCal;
        private MessagesV2 mMsg;

        public SmartDataSeletiveRestoreAsync(Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, String vaultId, Boolean isMirr, ResponseCallback responseCallback) {
            mCatCode = catCode;
            mSmartDataInfoList = smartDataInfoList;
            mResponseCallback = responseCallback;
            mVaultId = vaultId;
            mIsMirr = isMirr;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(Void... params) {

            switch (mCatCode) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    mCon = new ContactsV2(appCtxt, mVaultId);
                    res = mCon.iterateNaddToPhoneDb(mSmartDataInfoList, mIsMirr);
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    mCal = new CalendersV2(appCtxt, mVaultId);
                    res = mCal.iterateNaddToPhoneDb(mSmartDataInfoList, mIsMirr);
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    try {

                        int limit = 10, i;
                        String packageName = UiContext.getInstance().getAppContext().getPackageName();
                        res = false;
                        for (i = 0; i <= limit; i++) {

                            if (Telephony.Sms.getDefaultSmsPackage(UiContext.getInstance().getAppContext()).equals(packageName) || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                                mMsg = new MessagesV2(appCtxt, mVaultId);
                                res = mMsg.iterateNaddToPhoneDb(mSmartDataInfoList, mIsMirr);
                                break;
                            }
                            Thread.sleep(1000);
                        }


                    } catch (InterruptedException e) {
                        res = false;
                        e.printStackTrace();
                    }


                    break;
                default:

            }
            return res;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            MeemEvent event = new MeemEvent();
            event.setResult(result);
            event.setResponseCallback(mResponseCallback);
            UiContext.getInstance().postEvent(event);
        }

        public ContactsV2 getContacts() {
            return mCon;
        }

        public CalendersV2 getCalenders() {
            return mCal;
        }

        public MessagesV2 getMessages() {
            return mMsg;
        }

        public Byte getCatCode() {
            return mCatCode;
        }

        public void setResult(Boolean result) {
            res = result;
        }
    }

    // ------------------------------------------------------------------------------
    // ---------------- Start: MEEM V2, Desktop mode --------------------------------
    // ------------------------------------------------------------------------------
    public boolean switchToDesktopMode(ResponseCallback responseCallback) {
        return mCableDriver.changeModeOfMeem(MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM, responseCallback);
    }

    public boolean switchToBypassMode(ResponseCallback responseCallback) {
        return mCableDriver.changeModeOfMeem(MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS, responseCallback);
    }
    // ------------------------------------------------------------------------------
    // ---------------- End: MEEM V2, Desktop mode ----------------------------------
    // ------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------
    // ---------------- Start: MEEM V2, App policies --------------------------------
    // ------------------------------------------------------------------------------
    public boolean setPolicyIsNetworkSharingEnabled(final boolean enable, final ResponseCallback responseCallback) {
        mDbg.trace();
        return mCableDriver.savePolicyIsNetworkSharingEnabled(enable, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    public boolean getPolicyIsNetworkSharingEnabled() {
        mDbg.trace();

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mAppLocaldata.getConfigDbFullPath());
        configDb.openDatabase();
        boolean policyIsNetworkSharingEnabled = configDb.getPolicyIsNetworkSharingEnabled();
        configDb.closeDatabase();

        return policyIsNetworkSharingEnabled;
    }

    /**
     * Arun: 19Aug2017: Apply policies like "network slave can only see his vault in the app".
     */
    private void applyViewModelPolicies() {
        mDbg.trace();

        boolean policyIsNetworkSharingEnabled = getPolicyIsNetworkSharingEnabled();

        if(mCableDriverV2.isRemoteCable()) {
            if(!policyIsNetworkSharingEnabled) {
                mDbg.trace("User set policy: network sharing disabled");

                // take phone's vault info and create a new vaultinfo map using it.
                VaultInfo phoneVaultInfo = mCableInfo.getVaultInfo(mPhoneUpid);
                LinkedHashMap<String, VaultInfo> vaultMap = new LinkedHashMap<>(1);
                vaultMap.put(mPhoneUpid, phoneVaultInfo);
                mCableInfo.setmVaultInfoMap(vaultMap);
                mCableInfo.mNumVaults = vaultMap.size(); // Bugfix: 14Sep2017: Do not forget to update number of vaults
            } else {
                mDbg.trace("User set policy: network sharing enabled");
            }
        } else {
            mDbg.trace("Cable is local, policy: Normal operation");
        }

        // Arun: 23April2018: Allowing user to edit connected phone's mirror name in settings.
        // We are redefining cableName as connected phone's name. This is a hack, but this needs no UI changes.
        VaultInfo phoneVaultInfo = mCableInfo.getVaultInfo(mPhoneUpid);
        if(null != phoneVaultInfo) {
            mCableInfo.mName = phoneVaultInfo.mName;
        }
    }

    // ------------------------------------------------------------------------------
    // ---------------- End: MEEM V2, App policies ----------------------------------
    // ------------------------------------------------------------------------------
}
