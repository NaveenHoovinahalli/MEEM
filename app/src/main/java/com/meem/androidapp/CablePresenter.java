package com.meem.androidapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Telephony;

import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPDeleteCategorySpec;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSingleFileSpec;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLCategory;
import com.meem.phone.CalendarsDb;
import com.meem.phone.Calenders;
import com.meem.phone.Contacts;
import com.meem.phone.ContactsDb;
import com.meem.phone.Messages;
import com.meem.phone.MessagesDb;
import com.meem.ui.AppList;
import com.meem.ui.DetailsFragment;
import com.meem.ui.FirmwareItemDetails;
import com.meem.ui.GenDataInfo;
import com.meem.ui.HomeFragment;
import com.meem.ui.OutOfMemorySetting;
import com.meem.ui.PinChangeFragment;
import com.meem.ui.ProfileNameFragment;
import com.meem.ui.RestoreOrShareGenDataInterface;
import com.meem.ui.RestoreOrShareSmartDataInterface;
import com.meem.ui.SettingsFragment;
import com.meem.ui.SmartDataInfo;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.v2.cablemodel.ApplicationListModel;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This is the "presenter" in the overall app design. This guy will manage the model objects and provides ViewController/Fragments with
 * whatever data that is needed to be presented in the UI. View classes do not know anything about actual model objects that are meem/MMP
 * specific.
 * <p/>
 * Design detail: CableDriver is the class that will handle all communications with cable. On backend events, it will notify this presenter
 * class, and in turn, this presenter class will update the ViewController using an interface (using view model objects) that completely
 * hides the model objects from ViewController (and hence from rest of the Ui classes).
 * <p/>
 * Created by arun on 10/3/16.
 */
abstract public class CablePresenter implements HomeFragment.HomeFragmentInterface, PinChangeFragment.PinChangeInterface, DetailsFragment.DetailsFragmentInterface, RestoreOrShareGenDataInterface, RestoreOrShareSmartDataInterface, ProfileNameFragment.CableNameInterface, SettingsFragment.SettingInterface, FirmwareItemDetails.FirmwareInterface, OutOfMemorySetting.OutOfMemoryInterface, AppList.ApplicationListInterface {
    /**
     * The view model top level object.
     */
    protected CableDriver mCableDriver;
    protected CableInfo mCableInfo;
    protected String mPhoneUpid;
    protected CablePresenterHelper mHelper;
    protected AppLocalData mAppLocaldata = AppLocalData.getInstance();

    private DebugTracer mDbg = new DebugTracer("CablePresenter", "CablePresenter.log");

    private SmartDataSeletiveRestoreAsync seletiveRestoreAsync;

    public CablePresenter(String phoneUpid, CableDriver driver, CablePresenterHelper sessionHelper) {
        mCableDriver = driver;
        mPhoneUpid = phoneUpid;
        mHelper = sessionHelper;
    }

    public CableInfo getCableViewModel() {
        if (mCableDriver == null) {
            mDbg.trace("CableDriver instance is null! Improper handling of Cable disconnection");
            return null;
        }

        createCableViewModel();
        return mCableInfo;
    }

    public CableInfo getCableInfo() {
        return mCableInfo;
    }

    /**
     * This is the core method of this class. This method creates the view model of cable from the model objects maintained by cable
     * driver.
     */
    abstract void createCableViewModel();

    protected boolean isSdCardMappedCat(byte viewModelCat) {
        return (viewModelCat == MMPConstants.MMP_CATCODE_PHOTO_CAM ||
                viewModelCat == MMPConstants.MMP_CATCODE_VIDEO_CAM ||
                viewModelCat == MMPConstants.MMP_CATCODE_FILE ||
                viewModelCat == MMPConstants.MMP_CATCODE_DOCUMENTS_SD);
    }

    protected byte getSdCardMappedCat(byte viewModelCat) {
        byte sdCat = 0;
        switch (viewModelCat) {
            case MMPConstants.MMP_CATCODE_PHOTO:
                sdCat = MMPConstants.MMP_CATCODE_PHOTO_CAM;
                break;
            case MMPConstants.MMP_CATCODE_VIDEO:
                sdCat = MMPConstants.MMP_CATCODE_VIDEO_CAM;
                break;
            case MMPConstants.MMP_CATCODE_MUSIC:
                sdCat = MMPConstants.MMP_CATCODE_FILE; // wow!
                break;
            case MMPConstants.MMP_CATCODE_DOCUMENTS:
                sdCat = MMPConstants.MMP_CATCODE_DOCUMENTS_SD;
                break;
            default:
                throw new IllegalArgumentException("Invalid generic category code: " + viewModelCat);
        }

        return sdCat;
    }

    protected ArrayList<CategoryInfo> getSdCardMappedCatInfoList(ArrayList<CategoryInfo> viewModelCatInfoList) {
        ArrayList<CategoryInfo> sdMappedCatInfoList = new ArrayList<>();

        for (CategoryInfo catInfo : viewModelCatInfoList) {
            sdMappedCatInfoList.add(catInfo);
            byte catCode = catInfo.getmMmpCode();

            if (MMLCategory.isGenericCategoryCode(catCode)) {
                Byte sdMappedcat = getSdCardMappedCat(catCode);
                CategoryInfo.BackupMode sdMappedCatMode = catInfo.getmBackupMode();

                CategoryInfo sdMappedCatInfo = new CategoryInfo();
                sdMappedCatInfo.setmMmpCode(sdMappedcat);
                sdMappedCatInfo.setmBackupMode(sdMappedCatMode);

                // Other params are not neeeded.
                sdMappedCatInfoList.add(sdMappedCatInfo);
            }
        }

        return sdMappedCatInfoList;
    }

    // ------------------------------------------------------------------------------
    // ---------------- Public Methods
    // ------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------
    // ---------------- Start: HomeViewController interfaces ------------------------
    // ------------------------------------------------------------------------------

    /**
     * This method will be called by view classes with the view-model object of vault. Must take care of the sdcard mappings and update the
     * real model object before calling the cable driver to get this done with the cable.
     *
     * @param vaultInfo
     * @param responseCallback
     *
     * @return Will invoke responseCallback with updated model object.
     */
    abstract boolean updateVault(VaultInfo vaultInfo, final ResponseCallback responseCallback);

    @Override
    public void onDropOnPhone(String srcUpid) {
        mDbg.trace();

        VaultInfo vaultInfo = mCableInfo.getmVaultInfoMap().get(srcUpid);

        if (mPhoneUpid.equals(srcUpid)) {
            mHelper.startRestore(vaultInfo, null);
        } else {
            mHelper.startCopy(vaultInfo, null);
        }
    }

    @Override
    public void onDropOnMirror() {
        mDbg.trace();
        VaultInfo mirrVaultInfo = mCableInfo.getmVaultInfoMap().get(mPhoneUpid);
        mHelper.startBackup(mirrVaultInfo, null);
    }

    @Override
    public void onCategorySetSwipeToMirror(ArrayList<CategoryInfo> catInfoList) {
        mDbg.trace();

        ArrayList<CategoryInfo> sdCardMappedCatList = getSdCardMappedCatInfoList(catInfoList);
        mDbg.trace("SdCardMappedCatInfoList: " + sdCardMappedCatList);

        VaultInfo mirrVaultInfo = mCableInfo.getmVaultInfoMap().get(mPhoneUpid);
        mHelper.startBackup(mirrVaultInfo, sdCardMappedCatList);
    }

    @Override
    public void onCategorySetSwipeToPhone(String srcUpid, ArrayList<CategoryInfo> catInfoList) {
        mDbg.trace();

        ArrayList<CategoryInfo> sdCardMappedCatList = getSdCardMappedCatInfoList(catInfoList);
        mDbg.trace("SdCardMappedCatInfoList: " + sdCardMappedCatList);

        VaultInfo vaultInfo = mCableInfo.getmVaultInfoMap().get(srcUpid);

        if (mPhoneUpid.equals(srcUpid)) {
            mHelper.startRestore(vaultInfo, sdCardMappedCatList);
        } else {
            mHelper.startCopy(vaultInfo, sdCardMappedCatList);
        }
    }

    @Override
    public void onUpdateVault(VaultInfo vault, ResponseCallback responseCallback) {
        mDbg.trace();
        updateVault(vault, responseCallback);
    }

    @Override
    public void onAbortRequest() {
        mDbg.trace();
        mHelper.abortSession();
    }

    @Override
    abstract public void onVirginCablePinSetupEntry(final String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback);

    @Override
    public void onVirginCablePinSetupUiFinished() {
        mDbg.trace();
        mHelper.onVirginCablePinSetupComplete();
    }

    @Override
    public void onUnregisteredPhoneAuthEntry(String pin, final ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.performAuth(pin, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                // check 'info' Integer param and add logs
                boolean authRes = false;
                int resp = (Integer) info;
                if (resp == 0) {
                    authRes = true;
                } else if (resp == MMPConstants.MMP_ERROR_PIN_MISMATCH) {
                    mDbg.trace("PIN auth error: PIN mismatch");
                } else if (resp == MMPConstants.MMP_ERROR_CABLE_LOCKED) {
                    mDbg.trace("PIN auth error: Cable locked");
                    mHelper.onCriticalError("Too many wrong PIN attemps. Cable is locked!");
                }

                if (resp != MMPConstants.MMP_ERROR_CABLE_LOCKED) {
                    return responseCallback.execute(authRes, info, extraInfo);
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public void onUnregisteredPhoneAuthUiFinished() {
        mHelper.onUnregisteredPhoneAuthComplete();
    }

    @Override
    public void onAutoBackupCountDownEnd() {
        mHelper.onAutoBackupCountDownEnd();
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mHelper.onAutoBackupCountDownUiFinish();
    }

    @Override
    abstract public void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);

    // ------------------------------------------------------------------------------
    // ---------------- End: HomeViewController interfaces --------------------------
    // ------------------------------------------------------------------------------

    @Override
    public void onDisconnectedStateUiFinished() {
        mHelper.onDisconnectedStateUiFinished();
    }

    // ------------------------------------------------------------------------------
    // ---------------- Start: PINChangeFragment interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    public void onValidatePassword(final String password, final ResponseCallback responseCallback) {
        mDbg.trace();

        mCableDriver.performAuth(password, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                Integer authResp = (Integer) info;
                // TODO: ExtraInfo is attempts so far in V2.

                if (authResp == 0) {
                    responseCallback.execute(true, null, null);
                } else {
                    if (authResp == MMPConstants.MMP_ERROR_CABLE_LOCKED) {
                        mHelper.onCriticalError("Too many PIN attempts. Cable locked!");
                    }
                    responseCallback.execute(false, info, null);
                }

                return true;
            }
        });
    }

    @Override
    public void onUpdatePassword(String newPassword, LinkedHashMap<Integer, String> recoveryQuestions, ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.setPIN(newPassword, recoveryQuestions, responseCallback);
    }

    // ------------------------------------------------------------------------------
    // ---------------- End: PINChangeFragment interfaces -------------------------
    // ------------------------------------------------------------------------------

    @Override
    abstract public void onResetCable(final ResponseCallback responseCallback);

    // ------------------------------------------------------------------------------
    // ---------------- Start: DetailsFragment interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    public void onDeleteCategory(String vaultId, int CategoryId, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        MMPDeleteCategorySpec delSpec = new MMPDeleteCategorySpec(new MMPUpid(vaultId), (byte) CategoryId, isMirror);
        mCableDriver.deleteCategory(delSpec, responseCallback);
    }

    @Override
    abstract public void onDeleteVault(final String vaultId, final ResponseCallback responseCallback);

    @Override
    public void onUpdateVault(String vaultId, final Object vault, final ResponseCallback responseCallback) {
        mDbg.trace();

        VaultInfo vaultInfo = mCableInfo.getmVaultInfoMap().get(vaultId);
        updateVault(vaultInfo, responseCallback);
    }

    @Override
    public void onRequestGenDB(String vaultId, int CategoryId, ResponseCallback responseCallback) {
        responseCallback.execute(true, null, null);
    }
    // ------------------------------------------------------------------------------
    // ---------------- End: DetailsFragment interfaces -------------------------
    // ------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------
    // ---------------- Start: RestoreOrShare interfaces -------------------------
    // ------------------------------------------------------------------------------

    @Override
    public void onRequestSmartDB(String vaultId, int CategoryId, ResponseCallback responseCallback) {
        responseCallback.execute(true, null, null);
    }

    @Override
    abstract public void onRestoreGenData(String vaultId, Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback);

    @Override
    abstract public void onShareGenData(String vaultId, Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback);

    // Hackish ideas to deal with photos from iOS vault. See comments of sanitizeIOSAssetLibraryItemPath
    protected void sanitizeMultiPlatformSharedPaths(ArrayList<GenDataInfo> genDataInfoList) {
        for (GenDataInfo gdInfo : genDataInfoList) {
            String destPath = gdInfo.getDestFPath();
            if (destPath.contains("Iassets-library://")) {
                String sanitizedPath = GenUtils.sanitizeIOSAssetLibraryItemPath(destPath);

                // remove SDCard hack I/S
                sanitizedPath = sanitizedPath.substring(1);

                // Append external storage root path
                File extStorageDir = Environment.getExternalStorageDirectory();
                String extStoragePath = extStorageDir.getAbsolutePath();
                sanitizedPath = extStoragePath + File.separator + sanitizedPath;

                mDbg.trace("Sanitized sharing path :" + sanitizedPath);
                gdInfo.setDestFPath(sanitizedPath);
            }
        }
    }

    /**
     * Note that in response callback execute(), the list will contain only those entries that are successfully deleted from cable. Caller
     * must use this list to delete items from its own database.
     */
    @Override
    public void onDeleteGenData(String vaultId, Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        ArrayList<MMPSingleFileSpec> specList = new ArrayList<>();

        for (GenDataInfo genDataInfo : genDataInfoList) {
            String path = genDataInfo.getfPath();
            MMPFPath fpath = new MMPFPath(path, 0);

            // since this is delete, dest path is irrelevant
            MMPSingleFileSpec spec = new MMPSingleFileSpec(new MMPUpid(vaultId), catCode, isMirror, fpath, fpath);

            specList.add(spec);
        }

        mCableDriver.deleteGenericData(specList, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                String msg = "Success";
                if (!result) {
                    msg = "Error deleting item(s) from cable";

                    ArrayList<Integer> errIndexList = (ArrayList<Integer>) extraInfo;
                    for (Integer i : errIndexList) {
                        genDataInfoList.remove(i);
                    }
                }

                responseCallback.execute(result, genDataInfoList, msg);

                return result;
            }
        });
    }

    @Override
    public void onRestoreSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        seletiveRestoreAsync = new SmartDataSeletiveRestoreAsync(catCode, smartDataInfoList, responseCallback);
        seletiveRestoreAsync.execute();
    }

    public void cancelSelectiveRestoreAsyncTasks() {
        mDbg.trace();
        if (null != seletiveRestoreAsync) {
            switch (seletiveRestoreAsync.getCatCode()) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    Contacts aCon = seletiveRestoreAsync.getContacts();
                    if (null != aCon) aCon.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Contacts JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    Calenders aCal = seletiveRestoreAsync.getCalenders();
                    if (null != aCal) aCal.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Calenders JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    Messages aMsg = seletiveRestoreAsync.getMessages();
                    if (null != aMsg) aMsg.abort();
                    seletiveRestoreAsync.setResult(false);
                    mDbg.trace("Messages JsonToSql processing Aborted");
                    break;
                default:
                    mDbg.trace("onCancelled :In-Valid Category Code");

            }
        }


    }

    /**
     * Unused even in v1
     */
    @Override
    public void onShareSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        // TODO: Vignesh
        responseCallback.execute(true, smartDataInfoList, null);
    }


    /**
     * Unused even in v1
     */
    @Override
    public void onDeleteSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        // TODO: Vignesh
        responseCallback.execute(true, smartDataInfoList, null);
    }

    // ------------------------------------------------------------------------------
    // ---------------- Start: ProfileNameInterface interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    public void onUpdateCableName(final String name, final ResponseCallback responseCallback) {
        mDbg.trace();

        // Arun:23April2018: This scenario is redefined to update the connected phone's vault name.

        /*mCableDriver.setCableName(name, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mCableInfo.setmName(name);
                    return responseCallback.execute(result, name, null);
                }

                return result;
            }
        });*/

        // Trim the name to MAX_VAULT_NAME_LEN
        String trimmedName;
        if (name.length() > ProductSpecs.MAX_VAULT_NAME_LEN) {
            trimmedName = name.substring(0, ProductSpecs.MAX_VAULT_NAME_LEN) + "...";
        } else {
            trimmedName = name;
        }

        final VaultInfo mirrVaultInfo = mCableInfo.getmVaultInfoMap().get(mPhoneUpid);
        final String origName = mirrVaultInfo.mName;
        mirrVaultInfo.mName = trimmedName;

        updateVault(mirrVaultInfo, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mDbg.trace("Error updating vault name. Reverting to old name!");
                    mirrVaultInfo.mName = origName;
                } else {
                    // update home fragment.
                    mHelper.onVaultNameChanged(mirrVaultInfo);
                }
                return responseCallback.execute(result, mirrVaultInfo.mName, extraInfo);
            }
        });
    }

    // ------------------------------------------------------------------------------
    // ---------------- End: ProfileNameInterface interfaces -------------------------
    // ------------------------------------------------------------------------------

    abstract public void onUpdateProfileName(final String vaultId, final String newName, final ResponseCallback responseCallback);
    // ------------------------------------------------------------------------------
    // ---------------- End: SettingInterface interfaces -------------------------
    // ------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------
    // ---------------- Start: SettingInterface interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    public void onSoundUpdate(boolean isOn, final ResponseCallback responseCallback) {
        mDbg.trace();
        mHelper.onSoundUpdate(isOn);
    }


    // ------------------------------------------------------------------------------
    // ---------------- End: SettingInterface interfaces -------------------------
    // ------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------
    // ---------------- Start: FWInterface interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    abstract public void onFirmwareUpdate(String id, final ResponseCallback responseCallback);

    // ------------------------------------------------------------------------------
    // ---------------- End: FWInterface interfaces -------------------------
    // ------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------
    // ---------------- Start: For OOM Fragment -------------------------
    // ------------------------------------------------------------------------------

    @Override
    public void onCatModeChanged(final VaultInfo vaultInfo, final ResponseCallback responseCallback) {
        mDbg.trace();

        updateVault(vaultInfo, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mHelper.onCatModeChanged(vaultInfo);
                }

                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }


    //    TODO : update data to the database
    @Override
    public void onUpdateAppInfoToDb(ArrayList<ApplicationListModel> applicationListModels, ResponseCallback responseCallback) {

    }

    // ------------------------------------------------------------------------------
    // ---------------- End: For OOM Fragment -------------------------
    // ------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------
    // ---------------- Protected classes for smart data operations
    // ------------------------------------------------------------------------------

    protected class SmartDataSeletiveRestoreAsync extends AsyncTask<Void, Void, Boolean> {

        private Context appCtxt = UiContext.getInstance().getAppContext();
        private Byte mCatCode;
        private ArrayList<SmartDataInfo> mSmartDataInfoList;
        private ResponseCallback mResponseCallback;
        private Boolean res = true;

        private Contacts mCon;
        private Calenders mCal;
        private Messages mMsg;

        public SmartDataSeletiveRestoreAsync(Byte catCode, ArrayList<SmartDataInfo> smartDataInfoList, ResponseCallback responseCallback) {
            mCatCode = catCode;
            mSmartDataInfoList = smartDataInfoList;
            mResponseCallback = responseCallback;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected Boolean doInBackground(Void... params) {

            switch (mCatCode) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    mCon = new Contacts(appCtxt);
                    res = mCon.iterateNaddToPhoneDb(mSmartDataInfoList);
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    mCal = new Calenders(appCtxt);
                    res = mCal.iterateNaddToPhoneDb(mSmartDataInfoList);
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    try {

                        int limit = 10, i;
                        String packageName = UiContext.getInstance().getAppContext().getPackageName();
                        res = false;
                        for (i = 0; i <= limit; i++) {

                            if (Telephony.Sms.getDefaultSmsPackage(UiContext.getInstance().getAppContext()).equals(packageName) || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                                mMsg = new Messages(appCtxt);
                                res = mMsg.iterateNaddToPhoneDb(mSmartDataInfoList);
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

        public Contacts getContacts() {
            return mCon;
        }

        public Calenders getCalenders() {
            return mCal;
        }

        public Messages getMessages() {
            return mMsg;
        }

        public Byte getCatCode() {
            return mCatCode;
        }

        public void setResult(Boolean result) {
            res = result;
        }
    }

    protected class DeleteEntriesInSmartDataDBforUpidAsync extends AsyncTask<Void, Void, Boolean> {

        private Context appCtxt = UiContext.getInstance().getAppContext();
        private ResponseCallback mResponseCallback;
        private Boolean res = true;
        private String mUpid;

        private ContactsDb mCondb;
        private CalendarsDb mCaldb;
        private MessagesDb mMsgdb;

        public DeleteEntriesInSmartDataDBforUpidAsync(String upid, ResponseCallback responseCallback) {
            mUpid = upid;
            mResponseCallback = responseCallback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mDbg.trace("Contacts Db Entries Delete");
            mCondb = new ContactsDb(appCtxt, mAppLocaldata.getContactDbFullPath(), mUpid);
            res = mCondb.delteAllEntriesOfUpidInDb();
            if (!res) {
                return res;
            }

            mDbg.trace("Calendars Db Entries Delete");
            mCaldb = new CalendarsDb(appCtxt, mAppLocaldata.getCalendarDbFullPath(), mUpid);
            mCaldb.delteAllEntriesOfUpidInDb();
            if (!res) {
                return res;
            }

            mDbg.trace("Messages Db Entries Delete");
            mMsgdb = new MessagesDb(appCtxt, mAppLocaldata.getMessageDbFullPath(), mUpid);
            res = mMsgdb.delteAllEntriesOfUpidInDb();
            return res;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            MeemEvent event = new MeemEvent();
            event.setResult(result);
            event.setInfo(mCableInfo);
            event.setResponseCallback(mResponseCallback);
            UiContext.getInstance().postEvent(event);
        }
    }

    // For MEEM Desktop
    abstract public boolean switchToDesktopMode(ResponseCallback responseCallback);
    abstract public boolean switchToBypassMode(ResponseCallback responseCallback);

    // For MEEM Network policies
    abstract public boolean setPolicyIsNetworkSharingEnabled(boolean isPrivate, ResponseCallback responseCallback);
    abstract public boolean getPolicyIsNetworkSharingEnabled();
}
