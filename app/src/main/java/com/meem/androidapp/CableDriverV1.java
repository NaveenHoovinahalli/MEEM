package com.meem.androidapp;

import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.cablemodel.MeemCable;
import com.meem.cablemodel.MeemVault;
import com.meem.core.MeemCore;
import com.meem.events.ResponseCallback;
import com.meem.mmp.control.CableInit;
import com.meem.mmp.control.CloseSession;
import com.meem.mmp.control.DeleteCategory;
import com.meem.mmp.control.DeleteGenericData;
import com.meem.mmp.control.DoDestroyVault;
import com.meem.mmp.control.DoFactoryReset;
import com.meem.mmp.control.ExecuteSession;
import com.meem.mmp.control.GetAllVaultStatus;
import com.meem.mmp.control.GetCopySmartData;
import com.meem.mmp.control.GetDataDescriptor;
import com.meem.mmp.control.GetMeemStatus;
import com.meem.mmp.control.GetSessionlessGenericData;
import com.meem.mmp.control.GetSessionlessSmartData;
import com.meem.mmp.control.GetSingleFile;
import com.meem.mmp.control.GetSingleSmartData;
import com.meem.mmp.control.GetSmartData;
import com.meem.mmp.control.GetThumbnailDb;
import com.meem.mmp.control.MeemCoreHandler;
import com.meem.mmp.control.PerformAuthentication;
import com.meem.mmp.control.RegistrationCheck;
import com.meem.mmp.control.SendThumbnailDb;
import com.meem.mmp.control.SetMeemConfig;
import com.meem.mmp.control.SetPassword;
import com.meem.mmp.control.SetVaultConfig;
import com.meem.mmp.control.UpdateFirmware;
import com.meem.mmp.control.VaultCreation;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPDeleteCategorySpec;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSingleFileSpec;
import com.meem.mmp.messages.MMPSingleSmartDataSpec;
import com.meem.mmp.messages.MMPUmid;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.v2.core.MeemCoreV2;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by arun on 11/5/17.
 */

public class CableDriverV1 extends CableDriver {
    protected MMPUpid mPhoneUpid;
    protected MeemCoreHandler mMeemCoreHandler;

    public CableDriverV1(CableDriverListener listener) {
        super(listener);
        mDbg = new DebugTracer("CableDriverV1", "CableDriverV1.log");
    }

    @Override
    public boolean onCableConnect(AccessoryInterface accessory, final ResponseCallback responseCallback) {
        mDbg.trace();

        mPhoneUpid = new MMPUpid(mListener.getPhoneUpid());
        mDbg.trace("Phone upid: " + mPhoneUpid.toString());

        MeemCore core = MeemCore.getInstance();
        core.start(accessory);

        mMeemCoreHandler = (MeemCoreHandler) core.getHandler();

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            mIsConnected = true;

            MeemCoreV2 corev2 = MeemCoreV2.getInstance();
            corev2.start(accessory);
            // corev2.probeForCable();

            mCableModel = new MeemCable();
            mCableModel.updateStatus(null);

            MeemVault vault = new MeemVault();
            vault.setUpid(mListener.getPhoneUpid());
            vault.setName(mListener.getPhoneName());
            mCableModel.addVault(vault);
            responseCallback.execute(true, null, null);
            return true;
        }

        mMeemCoreHandler.addHandler(new CableInit(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                int authMethod = (Integer) info;
                String serial = (String) extraInfo;

                if (result && authMethod != MMPConstants.MMP_ERROR_CABLE_LOCKED) {
                    mIsConnected = true;

                    // Uncomment below only for debugging. Leaving this on in production code will make the "Searching for driver" issue
                    // in Windows happen.

                    /*return mMeemCoreHandler.addHandler(new ToggleRNDIS((byte) 1, new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            mDbg.trace("RNDIS mode set to 1: " + result);
                            return onCableInitSucceeded(authMethod, serial, responseCallback);
                        }
                    }));*/

                    return onCableInitSucceeded(authMethod, serial, responseCallback);
                } else {
                    String errMsg = "Cable init failed!";
                    if (authMethod == MMPConstants.MMP_ERROR_CABLE_LOCKED) {
                        errMsg = "Cable is locked!";
                    }

                    return onCableInitFailed(responseCallback, errMsg);
                }
            }
        }));

        // This initiates everything.
        core.probeForCable(accessory.isSlowSpeedMode());

        return true;
    }

    @Override
    public int getCableVersion() {
        return ProductSpecs.HW_VERSION_1_TI;
    }

    @Override
    public boolean onCableDisconnect() {
        mDbg.trace();
        mIsConnected = false;
        return true;
    }

    @Override
    public int getAuthMethod() {
        return mAuthMethod;
    }

    @Override
    public boolean onPhoneAuthSucceeded(final ResponseCallback responseCallback) {
        mDbg.trace();

        return refreshModel(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return registerPhone(responseCallback);
            }
        });
    }

    private boolean registerPhone(final ResponseCallback responseCallback) {
        mDbg.trace();

        if ((null != mCableModel) && (ProductSpecs.LIMIT_MAX_VAULTS == mCableModel.getNumVaults())) {
            mDbg.trace("Maximum number of vaults are there in cable. Can't continue.");
            mListener.onMaxNumVaultsDetected();
            // Stuck! No Go!
            return false;
        }

        File pinfFile = mListener.createPinf();
        String pinfPath = pinfFile.getAbsolutePath();
        final MMPFPath pinfFPath = new MMPFPath(pinfPath, pinfFile.length());

        // register this phone
        return mMeemCoreHandler.addHandler(new VaultCreation(mPhoneUpid, pinfFPath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("Vault creation succeeded. Adding to model and setting vault config. upid: " + mListener.getPhoneUpid());
                    MeemVault vault = new MeemVault();
                    vault.setUpid(mListener.getPhoneUpid());
                    vault.setName(mListener.getPhoneName());
                    mCableModel.addVault(vault);

                    String vcfgPath = AppLocalData.getInstance().getVcfgPath();
                    vault.createVcfg(vcfgPath);
                    File vcfgFile = new File(vcfgPath);
                    MMPFPath vcfgFPath = new MMPFPath(vcfgPath, vcfgFile.length());

                    // set vault config
                    return mMeemCoreHandler.addHandler(new SetVaultConfig(mPhoneUpid, vcfgFPath, new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            if (result) {
                                mDbg.trace("Setting vault config succeeded");
                                return refreshModel(responseCallback);
                            } else {
                                onCriticalError("Setting vault config failed!");
                                return false;
                            }
                        }
                    }));
                } else {
                    onCriticalError("Vault creation failed!");
                    return refreshModel(responseCallback);
                }
            }
        }));
    }

    @Override
    public void onPhoneAuthFailed() {
        mDbg.trace();
    }

    @Override
    public boolean onVirginCablePINSettingFinished(final ResponseCallback responseCallback) {
        mDbg.trace();

        // Note: Phone can be considered authentic with a virgin cable. !BUT! Firmware needs a
        // RegistrationCheck command for proper functioning. Crap!
        return mMeemCoreHandler.addHandler(new RegistrationCheck(mPhoneUpid, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                // Result will be false, because cable is virgin. See comment above.
                // We must now refresh the cable model so that mstat is updated with newly registered vault.
                return refreshModel(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return registerPhone(responseCallback);
                    }
                });
            }
        }));
    }

    // ===============================================================================================================================
    // Misc cable operations
    // ===============================================================================================================================

    // Arun: 05July2018: Note: Recovery is ignored in V1
    @Override
    public boolean setPIN(String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new SetPassword(pin, mPhoneUpid, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean performAuth(String pin, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new PerformAuthentication(MMPConstants.MMP_CODE_PIN_AUTH, pin, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean setCableName(String name, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        mCableModel.setName(name);
        return setMCFG(responseCallback);
    }


    @Override
    public boolean setVaultConfig(String upid, MMPFPath vcfgPath, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new SetVaultConfig(new MMPUpid(upid), vcfgPath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean getThumbnailDb(String dbPath, int dbVersion, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        MMPFPath dbFPath = new MMPFPath(dbPath, 0);
        return mMeemCoreHandler.addHandler(new GetThumbnailDb(dbFPath, dbVersion, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) GenUtils.copyDbFileToDownloads("thumbnails.db", "thumbnails-down.db");
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean sendThumbnailDb(String dbPath, long dbSize, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        MMPFPath dbFPath = new MMPFPath(dbPath, dbSize);
        return mMeemCoreHandler.addHandler(new SendThumbnailDb(dbFPath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean deleteGenericData(ArrayList<MMPSingleFileSpec> specList, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new DeleteGenericData(specList, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                final Object resInfo = info;
                final Object resExtraInfo = extraInfo;

                return refreshModel(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return responseCallback.execute(result, resInfo, resExtraInfo);
                    }
                });
            }
        }));
    }

    @Override
    public boolean deleteCategory(MMPDeleteCategorySpec delSpec, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new DeleteCategory(delSpec, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean deleteVault(final String upid, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new DoDestroyVault(new MMPUpid(upid), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mCableModel.removeVault(upid);

                    if (upid.equals(mPhoneUpid.toString())) {
                        mDbg.trace("This phone's vault is deleted. Need to register again");
                        return registerPhone(responseCallback);
                    }

                    return refreshModel(new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            return responseCallback.execute(result, info, extraInfo);
                        }
                    });
                } else {
                    return responseCallback.execute(result, info, extraInfo);
                }
            }
        }));
    }

    @Override
    public boolean resetCable(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new DoFactoryReset(mPhoneUpid, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean updateFirmware(String fwPath, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new UpdateFirmware(fwPath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean getSingleFile(MMPSingleFileSpec fileSpec, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new GetSingleFile(fileSpec, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean getSingleSmartData(MMPSingleSmartDataSpec fileSpec, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new GetSingleSmartData(fileSpec, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean getSessionlessSmartData(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        Map<String, MeemVault> vaultsMap = mCableModel.getVaults();

        ArrayList<Byte> smartCats = new ArrayList<>();
        smartCats.add(MMPConstants.MMP_CATCODE_CONTACT);
        smartCats.add(MMPConstants.MMP_CATCODE_MESSAGE);
        smartCats.add(MMPConstants.MMP_CATCODE_CALENDER);

        AppLocalData appData = AppLocalData.getInstance();

        ArrayList<MMPSingleSmartDataSpec> specList = new ArrayList<>();

        for (String upid : vaultsMap.keySet()) {
            for (Byte cat : smartCats) {
                String tmpMirrPath = appData.getSmartCatTempFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), true);
                MMPFPath tmpMirrFPath = new MMPFPath(tmpMirrPath, 0);
                // archive current file - if any
                appData.archive(tmpMirrPath);

                String tmpPlusPath = appData.getSmartCatTempFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), false);
                MMPFPath tmpPlusFPath = new MMPFPath(tmpPlusPath, 0);
                // archive current file - if any
                appData.archive(tmpPlusPath);

                MMPSingleSmartDataSpec spec = new MMPSingleSmartDataSpec(new MMPUpid(upid), cat, tmpMirrFPath, tmpPlusFPath);
                specList.add(spec);
            }
        }

        return mMeemCoreHandler.addHandler(new GetSessionlessSmartData(specList, responseCallback));
    }

    @Override
    public boolean getSmartData(byte handle, SessionSmartDataInfo sesSmartInfo, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        mMeemCoreHandler.addHandler(new GetSmartData(handle, sesSmartInfo.getCatCodes(), sesSmartInfo.getCatsFilesMap(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
        return false;
    }

    @Override
    public boolean getCopySmartData(byte handle, SessionSmartDataInfo sesSmartInfo, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        mMeemCoreHandler.addHandler(new GetCopySmartData(handle, sesSmartInfo.getCatCodes(), sesSmartInfo.getCatsFilesMap(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
        return false;
    }

    @Override
    public boolean getDATD(MMPUpid upid, MMPUmid umid, int sessionType, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new GetDataDescriptor(upid, umid, sessionType, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean executeSession(byte handle, MMPFPath fpathSesd, MMPFPath fpathThumbDb, boolean isCopy, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new ExecuteSession(handle, fpathSesd, fpathThumbDb, isCopy, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return refreshModel(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return responseCallback.execute(result, info, extraInfo);
                    }
                });
            }
        }));
    }

    @Override
    public boolean closeSession(byte handle, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addHandler(new CloseSession(handle, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return refreshModel(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return responseCallback.execute(result, info, extraInfo);
                    }
                });
            }
        }));
    }

    @Override
    public boolean getSessionlessGenericData(ArrayList<MMPSingleFileSpec> specList, final ResponseCallback responseCallback) {
        mDbg.trace();
        return mMeemCoreHandler.addHandler(new GetSessionlessGenericData(specList, responseCallback));
    }

    /**
     * You must carefully go through comments in V1 app to understand why this method is not same as other methods.
     *
     * @return
     */
    @Override
    public boolean abortSession() {
        mDbg.trace();
        return mMeemCoreHandler.notifyAbortRequest();
    }

    @Override
    public void resetXfrStats() {
        MeemCore core = MeemCore.getInstance();
        core.resetXfrStat();
    }

    @Override
    public double getXfrStats() {
        MeemCore core = MeemCore.getInstance();
        return core.getXfrStat();
    }

    @Override
    public void sendAppQuit() {
        // Dummy
    }

    // ===============================================================================================================================
    // Private functions.
    // ===============================================================================================================================

    /**
     * The creation and populating of model objects begins,
     *
     * @param authMethod
     * @param serial     - unused.
     */
    private boolean onCableInitSucceeded(int authMethod, String serial, final ResponseCallback responseCallback) {
        mDbg.trace();

        mAuthMethod = authMethod;
        mCableModel = new MeemCable();

        if (mAuthMethod == MMPConstants.MMP_ERROR_PASSWORD_UNSET) {
            mDbg.trace("Virgin cable detected");
            // TODO: check if we need to set MCFG now.
            mListener.onVirginCableConnection();
        } else {
            mDbg.trace("Cable is not virgin. Checking for phone registration.");
            mMeemCoreHandler.addHandler(new RegistrationCheck(mPhoneUpid, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    if (result) {
                        mDbg.trace("Phone is already registered. Going to build model.");
                        refreshModel(new ResponseCallback() {
                            @Override
                            public boolean execute(boolean result, Object info, Object extraInfo) {
                                responseCallback.execute(result, info, extraInfo);
                                return false;
                            }
                        });
                    } else {
                        mDbg.trace("Phone is not registered. Must authenticate this phone.");
                        mListener.onUnregisteredPhoneConnection();
                    }

                    return true;
                }
            }));
        }
        return true;
    }

    private boolean onCableInitFailed(final ResponseCallback responseCallback, String errMsg) {
        mDbg.trace();
        responseCallback.execute(false, null, null);
        onCriticalError(errMsg);
        return false;
    }

    private boolean refreshModel(final ResponseCallback responseCallback) {
        mDbg.trace();
        return fetchMSTAT(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    private boolean setMCFG(final ResponseCallback responseCallback) {
        String mcfgPath = AppLocalData.getInstance().getMcfgPath();
        if (!mCableModel.createMCFG(mcfgPath)) {
            return responseCallback.execute(false, null, null);
        }

        File mcfgFile = new File(mcfgPath);
        MMPFPath mcfgFPath = new MMPFPath(mcfgPath, mcfgFile.length());

        return mMeemCoreHandler.addHandler(new SetMeemConfig(mcfgFPath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mDbg.trace("Reverting name change in cable model on error.");
                    mCableModel.revertName();
                }
                return responseCallback.execute(result, null, null);
            }
        }));
    }

    private boolean fetchMSTAT(final ResponseCallback responseCallback) {
        mDbg.trace();

        final String mstatFilePath = AppLocalData.getInstance().getMstatPath();
        return mMeemCoreHandler.addHandler(new GetMeemStatus(new MMPFPath(mstatFilePath, 0), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("MSTAT received.");
                    mCableModel.updateStatus(mstatFilePath);

                    // get all VSTATs
                    if (0 != mCableModel.getNumVaults()) {
                        fetchAllVSTATs(responseCallback);
                    } else {
                        mDbg.trace("No vaults in cable yet.");
                        responseCallback.execute(result, mCableModel, extraInfo);
                    }
                } else {
                    onCriticalError("Fetching MSTAT failed.");
                }
                return result;
            }
        }));
    }

    private void fetchAllVSTATs(final ResponseCallback responseCallback) {
        mDbg.trace();

        Map<String, MeemVault> vaultsMap = mCableModel.getVaults();

        ArrayList<MMPUpid> upids = new ArrayList<MMPUpid>();
        ArrayList<MMPFPath> fpaths = new ArrayList<MMPFPath>();

        for (String upid : vaultsMap.keySet()) {
            upids.add(new MMPUpid(upid));

            MeemVault vault = vaultsMap.get(upid);
            fpaths.add(new MMPFPath(vault.getVstatPath(), 0));
        }

        mMeemCoreHandler.addHandler(new GetAllVaultStatus(upids, fpaths, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("All VSTATs received.");
                    mCableModel.updateAllVaultStatus();
                } else {
                    onCriticalError("Fetching VSTATs failed.");
                }

                return responseCallback.execute(result, mCableModel, null);
            }
        }));
    }

    private void onCriticalError(String msg) {
        String errorMsg = "Cable driver: " + msg;
        mDbg.trace(errorMsg);
        mListener.onCriticalError(errorMsg);
    }

    @Override
    public String getFwVersion() {
        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return null;
        }

        return mCableModel.getFwVersion();
    }

    @Override
    public String getSerialNo() {
        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return null;
        }

        return "MEEM";
    }

    // ==================================================================
    // --- The following are design extensions required for V2,
    // propagated to V1 ---
    // ==================================================================

    @Override
    public boolean sendSmartData(String upid, final MMLSmartDataDesc desc, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean fetchSmartData(String upid, final MMLSmartDataDesc desc, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean sendGenericData(String upid, MMLGenericDataDesc desc, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean fetchGenericData(String upid, MMLGenericDataDesc desc, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean refreshAllDatabases(ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean cleanupCable(String upid, ArrayList<Byte> cats) {
        return false;
    }

    @Override
    public boolean updateVault(VaultInfo vaultInfo, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public void setExperimentalHwBufferSize(int bufSize) {
        return;
    }

    @Override
    public boolean changeModeOfMeem(byte newMode, ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    // ============= for network : not supported in V1
    @Override
    public boolean isRemoteCable() {
        return false;
    }

	@Override
    public boolean acquireBigCableLock(final ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean releaseBigCableLock(final ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean sendMessageToNetMaster(int msgCode, final ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    @Override
    public boolean savePolicyIsNetworkSharingEnabled(boolean enable, final ResponseCallback responseCallback) {
        if(null != responseCallback) {
            return responseCallback.execute(false, null, null);
        }

        return false;
    }

    // Arun: 05July2018 (1.0.63) : added for pin recovery feature
    public boolean validateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mDbg.trace("WARNING: PIN recovery is not supported in V1");
        return responseCallback.execute(false, null, null);
    }

    // Arun: Added 20Aug2018: No use for V1
    public int getFwDbStatus() {
        return 0;
    }

    // Arun: Added 20Aug2018: No use for V1
    public String getFwDelPendingUpid() {
        return "";
    }
}

