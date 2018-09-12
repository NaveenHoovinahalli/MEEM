package com.meem.androidapp;

import com.meem.businesslogic.GenericDataThumbnailDatabase;
import com.meem.cablemodel.MeemCable;
import com.meem.cablemodel.MeemVault;
import com.meem.cablemodel.SyncSettings;
import com.meem.cablemodel.VaultUsage;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPDeleteCategorySpec;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSingleFileSpec;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataSettings;
import com.meem.mmp.mml.MMLSmartDataSettings;
import com.meem.ui.GenDataInfo;
import com.meem.utils.DebugTracer;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.SyncInfo;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
public class CablePresenterV1 extends CablePresenter {
    private DebugTracer mDbg = new DebugTracer("CablePresenterV1", "CablePresenterV1.log");

    public CablePresenterV1(String phoneUpid, CableDriver driver, CablePresenterHelper sessionHelper) {
        super(phoneUpid, driver, sessionHelper);

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

    /**
     * This is the core method of this class. This method creates the view model of cable from the model objects maintained by cable
     * driver.
     */
    @Override
    public void createCableViewModel() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("WARNING! Cable disconnected.");
            return;
        }

        MeemCable cableModel = mCableDriver.getCableModel();

        mCableInfo = new CableInfo();

        mCableInfo.mName = cableModel.getName();
        mCableInfo.mIsVirgin = false; // virgin cables acenario is never visible to the views
        mCableInfo.fwVersion = cableModel.getFwVersion();
        mCableInfo.mCapacityKB = cableModel.getUsageInfo().getTotalStorage();
        mCableInfo.mFreeSpaceKB = cableModel.getUsageInfo().getFreeStorage();
        mCableInfo.mPin = null; // pin is not kept in app
        mCableInfo.mNumVaults = cableModel.getNumVaults();

        Map<String, MeemVault> vaultMap = cableModel.getVaults();
        mCableInfo.mVaultInfoMap = new LinkedHashMap<>(mCableInfo.mNumVaults);

        for (String upid : vaultMap.keySet()) {
            MeemVault vault = vaultMap.get(upid);
            VaultInfo vaultInfo = createVaultInfo(upid, vault);
            mCableInfo.mVaultInfoMap.put(upid, vaultInfo);
        }
    }

    private VaultInfo createVaultInfo(String upid, MeemVault vault) {
        mDbg.trace();

        VaultInfo vaultInfo = new VaultInfo();

        vaultInfo.mUpid = vault.getUpid();
        vaultInfo.mIsMirror = vaultInfo.mUpid.equals(mPhoneUpid);
        vaultInfo.mName = vault.getName();
        vaultInfo.mMirrorSizeKB = vault.getUsageInfo().getTotalMirrorDataUsage();
        vaultInfo.mPlusSizeKB = vault.getUsageInfo().getTotalPlusDataUsage();
        vaultInfo.mLastBackupTime = vault.getmConfig().getHistory().getLastBackupTime();

        // create the category info from the vault model - step by step - this idea is the heart of the product!
        LinkedHashMap<Byte, CategoryInfo> genCatInfoMap = createGenCategoryInfoMap(vault);
        LinkedHashMap<Byte, CategoryInfo> smartCatInfoMap = createSmartCategoryInfoMap(vault);

        // keep all in a single category map
        vaultInfo.mCategoryInfoMap = new LinkedHashMap<>();
        vaultInfo.mCategoryInfoMap.putAll(smartCatInfoMap);
        vaultInfo.mCategoryInfoMap.putAll(genCatInfoMap);

        // Note: as of 22Sep2016: Sync feature is gone!!!
        vaultInfo.mSyncInfo = null;

        // Arun: 02Jun2017: Fix for V2 effects in V1
        vaultInfo.mGenMirrorCatMask = vault.getGenericDataCategoryMask();
        vaultInfo.mGenPlusCatMask = vault.getGenericPlusDataCategoryMask();

        vaultInfo.mSmartMirrorCatMask = vault.getSmartDataCategoryMask();
        vaultInfo.mSmartPlusCatMask = vault.getSmartPlusDataCategoryMask();

        return vaultInfo;
    }

    /**
     * This method takes care of all generic data categories, transparently handling the sdcard mapped categories related crap.
     *
     * @param vault
     *
     * @return
     */
    private LinkedHashMap<Byte, CategoryInfo> createGenCategoryInfoMap(MeemVault vault) {
        mDbg.trace();

        LinkedHashMap<Byte, CategoryInfo> genCatInfoMap = new LinkedHashMap<>();

        int genMirrorCatMask = vault.getGenericDataCategoryMask();
        int genPlusCatMask = vault.getGenericPlusDataCategoryMask();

        ArrayList<Byte> genMirrorCats = MMLCategory.getGenericCatCodesArray(genMirrorCatMask);
        ArrayList<Byte> genPlusCats = MMLCategory.getGenericCatCodesArray(genPlusCatMask);

        // a private shortcut! to get an arraylist of all generic cats supported.
        MMLGenericDataSettings defGenCatSettings = new MMLGenericDataSettings(true);
        ArrayList<Byte> defGenCats = MMLCategory.getGenericCatCodesArray(defGenCatSettings.getCategoryMask());

        for (Byte cat : defGenCats) {
            if (isSdCardMappedCat(cat)) {
                mDbg.trace("Noted sdcard mapped cat: " + cat);
                continue;
            }

            CategoryInfo catInfo = new CategoryInfo();
            CategoryInfo.BackupMode mode = CategoryInfo.BackupMode.DISABLED;

            catInfo.mMmpCode = cat;

            // TODO: The following logic is deliberately twisted to find a possible obscure bug!
            boolean isPlus = genPlusCats.contains(cat);
            boolean isMirror = genMirrorCats.contains(cat);
            if (isPlus && !isMirror) {
                throw new IllegalStateException("BUG: If PLUS mode is enabled, MIRROR mode should be automatically enabled");
            }

            if (isPlus) {
                mode = CategoryInfo.BackupMode.PLUS;
            } else if (isMirror) {
                mode = CategoryInfo.BackupMode.MIRROR;
            }

            catInfo.mBackupMode = mode;

            // whatever be the current auto-backup mode, we must get the usage information
            VaultUsage.UsageInfo catUsage = vault.getUsageInfo().getGenUsageInfo(cat);
            if (null != catUsage) {
                catInfo.mMirrorSizeKB = catUsage.getMirrorSize();
                catInfo.mPlusSizeKB = catUsage.getPlusSize();
            }

            // Handle SDCARD category code mapping stupidity
            byte sdCat = getSdCardMappedCat(cat);

            VaultUsage.UsageInfo sdCatUsage = vault.getUsageInfo().getGenUsageInfo(sdCat);
            if (null != sdCatUsage) {
                catInfo.mMirrorSizeKB += sdCatUsage.getMirrorSize();
                catInfo.mPlusSizeKB += sdCatUsage.getPlusSize();
            }

            genCatInfoMap.put(cat, catInfo);
        }

        return genCatInfoMap;
    }

    private LinkedHashMap<Byte, CategoryInfo> createSmartCategoryInfoMap(MeemVault vault) {
        mDbg.trace();

        LinkedHashMap<Byte, CategoryInfo> smartCatInfoMap = new LinkedHashMap<>();

        int smartMirrorCatMask = vault.getSmartDataCategoryMask();
        int smartPlusCatMask = vault.getSmartPlusDataCategoryMask();

        ArrayList<Byte> smartMirrorCats = MMLCategory.getSmartCatCodesArray(smartMirrorCatMask);
        ArrayList<Byte> smartPlusCats = MMLCategory.getSmartCatCodesArray(smartPlusCatMask);

        // a private shortcut! to get an arraylist of all generic cats supported.
        MMLSmartDataSettings defSmartCatSettings = new MMLSmartDataSettings(true);
        ArrayList<Byte> defSmartCats = MMLCategory.getSmartCatCodesArray(defSmartCatSettings.getCategoryMask());

        for (Byte cat : defSmartCats) {
            CategoryInfo catInfo = new CategoryInfo();
            CategoryInfo.BackupMode mode = CategoryInfo.BackupMode.DISABLED;

            catInfo.mMmpCode = cat;

            // TODO: The following logic is deliberately twisted to find a possible obscure bug!
            boolean isPlus = smartPlusCats.contains(cat);
            boolean isMirror = smartMirrorCats.contains(cat);
            if (isPlus && !isMirror) {
                throw new IllegalStateException("BUG: If PLUS mode is enabled, MIRROR mode should be automatically enabled");
            }

            if (isPlus) {
                mode = CategoryInfo.BackupMode.PLUS;
            } else if (isMirror) {
                mode = CategoryInfo.BackupMode.MIRROR;
            }

            catInfo.mBackupMode = mode;

            // whatever be the current auto-backup mode, we must get the usage information
            VaultUsage.UsageInfo catUsage = vault.getUsageInfo().getSmartUsageInfo(cat);
            if (null != catUsage) {
                catInfo.mMirrorSizeKB = catUsage.getMirrorSize();
                catInfo.mPlusSizeKB = catUsage.getPlusSize();
            }

            smartCatInfoMap.put(cat, catInfo);
        }

        return smartCatInfoMap;
    }

    // ------------------------------------------------------------------------------
    // The following 3 methdos are for ugly sdCad  category mapping done for generic
    // data.
    // ------------------------------------------------------------------------------

    // Note: 22Sep2016: Sync feature is gone.
    private SyncInfo createSyncInfo(SyncSettings syncSettings) {
        return null;
    }

    // ------------------------------------------------------------------------------
    // public methods
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
    public boolean updateVault(VaultInfo vaultInfo, final ResponseCallback responseCallback) {
        MeemCable cableModel = mCableDriver.getCableModel();
        if (cableModel == null) {
            // throw new IllegalStateException("Trying to create view-model objects with null cable model (Cable disconnected)");
            mDbg.trace("Trying to create view-model objects with null cable model (Cable disconnected)");

            if (null != responseCallback) {
                return responseCallback.execute(false, null, null);
            } else {
                return false;
            }
        }

        final String upid = vaultInfo.getmUpid();

        MeemVault vault = cableModel.getVault(upid);
        if (vault == null) {
            throw new IllegalArgumentException("Vault not found for upid: " + upid);
        }

        int genMirrorCatMask = vault.getGenericDataCategoryMask();
        int genPlusCatMask = vault.getGenericPlusDataCategoryMask();

        int smartMirrorCatMask = vault.getSmartDataCategoryMask();
        int smartPlusCatMask = vault.getSmartPlusDataCategoryMask();

        // prepare category masks from category hashmaps of view model is the key operation here
        LinkedHashMap<Byte, CategoryInfo> catInfoMap = vaultInfo.getmCategoryInfoMap();

        // careful here :)
        for (Byte cat : catInfoMap.keySet()) {
            CategoryInfo catInfo = catInfoMap.get(cat);

            CategoryInfo.BackupMode curBkpMode = catInfo.getmBackupMode();

            boolean mirrFlag = false;
            boolean plusFlag = false;

            switch (curBkpMode) {
                case DISABLED:
                    break;
                case MIRROR:
                    mirrFlag = true;
                    plusFlag = false;
                    break;
                case PLUS:
                    // when plus is enabled, mirror shall also be enabled.
                    mirrFlag = true;
                    plusFlag = true;
                    break;
                default:
                    throw new IllegalArgumentException("BUG: Invalid backup mode: " + curBkpMode);
            }

            if (MMLCategory.isGenericCategoryCode(cat)) {
                // very careful here - sdcard mapping :)
                genMirrorCatMask = MMLCategory.updateGenCatMask(genMirrorCatMask, cat, mirrFlag);
                genMirrorCatMask = MMLCategory.updateGenCatMask(genMirrorCatMask, getSdCardMappedCat(cat), mirrFlag);

                genPlusCatMask = MMLCategory.updateGenCatMask(genPlusCatMask, cat, plusFlag);
                genPlusCatMask = MMLCategory.updateGenCatMask(genPlusCatMask, getSdCardMappedCat(cat), plusFlag);
            } else {
                smartMirrorCatMask = MMLCategory.updateSmartCatMask(smartMirrorCatMask, cat, mirrFlag);
                smartPlusCatMask = MMLCategory.updateSmartCatMask(smartPlusCatMask, cat, plusFlag);
            }
        }

        vault.setGenericDataCategoryMask(genMirrorCatMask);
        vault.setGenericPlusDataCategoryMask(genPlusCatMask);

        vault.setSmartDataCategoryMask(smartMirrorCatMask);
        vault.setSmartPlusDataCategoryMask(smartPlusCatMask);

        // call the driver to set these values to the vault in cable
        String vcfgPath = AppLocalData.getInstance().getVcfgPath();
        if (!vault.createVcfg(vcfgPath)) {
            mDbg.logCriticalError("VCFG creation failed for vault: " + upid);
            return false;
        }

        File vcfgFile = new File(vcfgPath);
        MMPFPath mmpfPathVcfg = new MMPFPath(vcfgPath, vcfgFile.length());

        return mCableDriver.setVaultConfig(upid, mmpfPathVcfg, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mDbg.logCriticalError("SET_VCFG failed for vault: " + upid);
                } else {
                    mDbg.trace("SET_VCFG succeeded for vault: " + upid);
                }

                createCableViewModel();

                if (null != responseCallback) {
                    return responseCallback.execute(result, mCableInfo, extraInfo);
                } else {
                    return result;
                }
            }
        });
    }

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

    // Arun: 05July2018: Note that pin recovery is not supported in V1 hardware.
    @Override
    public void onVirginCablePinSetupEntry(final String pin, final LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mDbg.trace();

        mCableDriver.setCableName("MEEM", new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                return mCableDriver.setPIN(pin, recoveryAnswers, new ResponseCallback() {
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
        });

    }

    // Arun: 05July2018: Note that pin recovery is not supported in V1 hardware.
    @Override
    public void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mDbg.trace("WARNING: Pin recovery is not supported in V1 hardware");
        responseCallback.execute(false, null, null);
    }

    @Override
    public void onAutoBackupCountDownEnd() {
        mHelper.onAutoBackupCountDownEnd();
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mHelper.onAutoBackupCountDownUiFinish();
    }

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
        super.onUpdatePassword(newPassword, recoveryQuestions, responseCallback);
    }

    // ------------------------------------------------------------------------------
    // ---------------- End: PINChangeFragment interfaces -------------------------
    // ------------------------------------------------------------------------------

    @Override
    public void onResetCable(final ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.resetCable(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    /*GenericDataThumbnailDatabase genThumbDb = new GenericDataThumbnailDatabase(mPhoneUpid);
                    genThumbDb.setThumbNailDbVersion(0xFFFFEEEE);*/
                    if (!mAppLocaldata.deleteGenDataThumbnailDb()) {
                        mDbg.trace("Deleting generic data thumbnail database failed");
                    }

                    if (!mAppLocaldata.deleteSmartDataThumbnailDb(MMPConstants.MMP_CATCODE_CONTACT)) {
                        mDbg.trace("Deleting contact databases failed");
                    }

                    if (!mAppLocaldata.deleteSmartDataThumbnailDb(MMPConstants.MMP_CATCODE_MESSAGE)) {
                        mDbg.trace("Deleting message databases failed");
                    }

                    if (!mAppLocaldata.deleteSmartDataThumbnailDb(MMPConstants.MMP_CATCODE_CALENDER)) {
                        mDbg.trace("Deleting calendar databases failed");
                    }
                }

                return responseCallback.execute(result, mCableInfo, null);
            }
        });
    }

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
    public void onDeleteVault(final String vaultId, final ResponseCallback responseCallback) {
        mDbg.trace();

        mCableDriver.deleteVault(vaultId, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    createCableViewModel();

                    // delete thumbnail tables for this upid
                    GenericDataThumbnailDatabase genThumbDb = new GenericDataThumbnailDatabase(vaultId);
                    if (!genThumbDb.dropAllTables()) {
                        mDbg.trace("Could not drop thumbnail tables for deleted vault");
                    } else {
                        if (vaultId.equals(mPhoneUpid)) {
                            genThumbDb.createThumbnailTables();
                        }
                    }

                    DeleteEntriesInSmartDataDBforUpidAsync smartDataThumbDebDeletion = new DeleteEntriesInSmartDataDBforUpidAsync(vaultId, responseCallback);
                    smartDataThumbDebDeletion.execute();
                    return true;
                } else {
                    return responseCallback.execute(result, mCableInfo, null);
                }
            }
        });
    }

    // TODO: Naveen, what is second argument? Also, there is one more onUpdateVault interface...
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
    public void onRestoreGenData(String vaultId, Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        ArrayList<MMPSingleFileSpec> specList = new ArrayList<>();

        for (GenDataInfo genDataInfo : genDataInfoList) {
            String path = genDataInfo.getfPath();

            // since this is restore, dest path must be same as path
            genDataInfo.setDestFPath(path);

            // Arun: 15June2017: Must do sdcard mapping!
            byte mappedCat = catCode;
            if (MMLCategory.isGenericCategoryCode(catCode) && genDataInfo.isSdcard()) {
                mappedCat = getSdCardMappedCat(catCode);
            }

            MMPFPath fpath = new MMPFPath(path, 0);
            MMPSingleFileSpec spec = new MMPSingleFileSpec(new MMPUpid(vaultId), mappedCat, isMirror, fpath, fpath);

            specList.add(spec);
        }

        mDbg.trace("Sharing files:\n" + specList);

        mCableDriver.getSessionlessGenericData(specList, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                String msg = "Success";
                if (!result) {
                    msg = "Error while restoring item(s) from cable";

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
    public void onShareGenData(String vaultId, Byte catCode, final ArrayList<GenDataInfo> genDataInfoList, boolean isMirror, final ResponseCallback responseCallback) {
        mDbg.trace();

        ArrayList<MMPSingleFileSpec> specList = new ArrayList<>();

        for (GenDataInfo genDataInfo : genDataInfoList) {
            String path = genDataInfo.getfPath();

            String destPath = AppLocalData.getInstance().getTempFilePathFor(path);
            genDataInfo.setDestFPath(destPath);

            MMPFPath fpath = new MMPFPath(path, 0);
            MMPFPath destFpath = new MMPFPath(destPath, 0);

            // Arun: 15June2017: Must do sdcard mapping!
            byte mappedCat = catCode;
            if (MMLCategory.isGenericCategoryCode(catCode) && genDataInfo.isSdcard()) {
                mappedCat = getSdCardMappedCat(catCode);
            }

            MMPSingleFileSpec spec = new MMPSingleFileSpec(new MMPUpid(vaultId), mappedCat, isMirror, fpath, destFpath);

            specList.add(spec);
        }

        mDbg.trace("Sharing files:\n" + specList);

        mCableDriver.getSessionlessGenericData(specList, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                String msg = "Success";
                if (!result) {
                    msg = "Error while fetching item(s) from cable";

                    ArrayList<Integer> errIndexList = (ArrayList<Integer>) extraInfo;
                    for (Integer i : errIndexList) {
                        genDataInfoList.remove(i);
                    }
                }

                sanitizeMultiPlatformSharedPaths(genDataInfoList);

                responseCallback.execute(result, genDataInfoList, msg);

                return result;
            }
        });
    }

    /**
     * Note that in response callback execute(), the list will contain only those entries that are successfully deleted from cable. Caller
     * must use this list to delete items from its own database.
     *
     * @param vaultId
     * @param catCode
     * @param genDataInfoList
     * @param isMirror
     * @param responseCallback
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

    // ------------------------------------------------------------------------------
    // ---------------- End: RestoreOrShare interfaces -------------------------
    // ------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------
    // ---------------- Start: ProfileNameInterface interfaces -------------------------
    // ------------------------------------------------------------------------------
    @Override
    public void onUpdateCableName(final String name, final ResponseCallback responseCallback) {
        mDbg.trace();

        mCableDriver.setCableName(name, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mCableInfo.setmName(name);
                    return responseCallback.execute(result, name, null);
                }

                return result;
            }
        });
    }
    // ------------------------------------------------------------------------------
    // ---------------- End: ProfileNameInterface interfaces -------------------------
    // ------------------------------------------------------------------------------

    public void onUpdateProfileName(final String vaultId, final String newName, final ResponseCallback responseCallback) {
        mDbg.trace();

        MeemCable cableModel = mCableDriver.getCableModel();
        if (cableModel == null) {
            throw new IllegalStateException("Trying to create view-model objects with null cable model (Cable disconnected)");
        }

        final MeemVault vault = cableModel.getVault(vaultId);
        final String currName = vault.getName();

        if (vault == null) {
            throw new IllegalArgumentException("Vault not found for upid: " + vaultId);
        }

        // call the driver to set these values to the vault in cable
        String vcfgPath = AppLocalData.getInstance().getVcfgPath();
        if (!vault.createVcfg(vcfgPath)) {
            mDbg.logCriticalError("VCFG creation failed for vault: " + vaultId);
            return;
        }

        File vcfgFile = new File(vcfgPath);
        MMPFPath mmpfPathVcfg = new MMPFPath(vcfgPath, vcfgFile.length());

        mCableDriver.setVaultConfig(vaultId, mmpfPathVcfg, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mDbg.logCriticalError("Rename SET_VCFG failed for vault: " + vaultId);
                    vault.setName(currName);
                } else {
                    mDbg.trace("Rename SET_VCFG succeeded for vault: " + vaultId);
                    vault.setName(newName);
                }

                if (null != responseCallback) {
                    responseCallback.execute(result, mCableInfo, extraInfo);
                }

                return result;
            }
        });


    }
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
    public void onFirmwareUpdate(String id, final ResponseCallback responseCallback) {
        mDbg.trace();
        mCableDriver.updateFirmware(id, responseCallback);
    }

    // ------------------------------------------------------------------------------
    // ---------------- End: FWInterface interfaces -------------------------
    // ------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------
    // ---------------- Start: MEEM V2, Desktop mode (unsupported in V1)-------------
    // ------------------------------------------------------------------------------
    public boolean switchToDesktopMode(ResponseCallback responseCallback) {
        return responseCallback.execute(false, null, null);
    }

    public boolean switchToBypassMode(ResponseCallback responseCallback) {
        return responseCallback.execute(false, null, null);
    }
    // ------------------------------------------------------------------------------
    // ---------------- End: MEEM V2, Desktop mode (unsupported in V1)---------------
    // ------------------------------------------------------------------------------


    // For MEEM Network policies
    public boolean setPolicyIsNetworkSharingEnabled(boolean isPrivate, ResponseCallback responseCallback) {
        return responseCallback.execute(false, null, null);
    }

    public boolean getPolicyIsNetworkSharingEnabled() {
        return false;
    }
}

