package com.meem.androidapp;

import android.database.sqlite.SQLiteDatabase;

import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPDeleteCategorySpec;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSingleFileSpec;
import com.meem.mmp.messages.MMPSingleSmartDataSpec;
import com.meem.mmp.messages.MMPUmid;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.phone.Storage;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.v2.cablemodel.ConfigDb;
import com.meem.v2.cablemodel.PhoneDbModel;
import com.meem.v2.cablemodel.SecureDb;
import com.meem.v2.cablemodel.VaultDbModel;
import com.meem.v2.core.MeemCoreV2;
import com.meem.v2.core.MeemCoreV2Handler;
import com.meem.v2.mmp.AuthPhoneV2;
import com.meem.v2.mmp.CableCleanupV2;
import com.meem.v2.mmp.CableInitV2;
import com.meem.v2.mmp.CableModeSwitchV2;
import com.meem.v2.mmp.CableResetV2;
import com.meem.v2.mmp.DeleteVaultV2;
import com.meem.v2.mmp.MMPV2Constants;
import com.meem.v2.mmp.MMPV2RecvFile;
import com.meem.v2.mmp.MMPV2SendFile;
import com.meem.v2.mmp.PinRecoveryV2;
import com.meem.v2.mmp.RebootV2;
import com.meem.v2.mmp.SendAppQuitV2;
import com.meem.v2.mmp.SetPinV2;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by arun on 11/5/17.
 */

public class CableDriverV2 extends CableDriver {
    private String mConfigDbPath, mSecureDbPath;
    private UiContext mUiCtxt = UiContext.getInstance();
    private Storage mStorage;
    private int mNumVaults;

    private int mExperimentalHwBuffSize = -1;

    private String mPhoneUpid;

    private MeemCoreV2Handler mMeemCoreHandler;

    private String mFwVersion = "0.0.0.0";
    private String mSerialNo = "MEEM";

    // Arun: 20Aug2018: Cable Disconnect in between DeleteVault may cause orphan entries in DB in cable.
    // Check for it - Added this flagging in FW by Barath - check mail on same date.
    private int mFwDbStatus = 0;
    private String mFwDelPendingUpid = "";

    private boolean mIsCableLocked = false;
    private boolean mIsCableVirgin = false;
    private boolean mIsCableRemote = false;

    private boolean mSecureDbCleanupPending;
    private String mSecureDbCleanupPendingUpid;

    public CableDriverV2(CableDriverListener listener) {
        super(listener);
        mDbg = new DebugTracer("CableDriverV2", "CableDriverV2.log");

        mConfigDbPath = AppLocalData.getInstance().getConfigDbFullPath();
        mSecureDbPath = AppLocalData.getInstance().getSecureDbFullPath();

        mStorage = new Storage();
    }

    @Override
    public boolean onCableConnect(final AccessoryInterface accessory, final ResponseCallback responseCallback) {
        mDbg.trace();

        mPhoneUpid = mListener.getPhoneUpid();
        mDbg.trace("Phone upid: " + mPhoneUpid);

        MeemCoreV2 core = MeemCoreV2.getInstance();
        if (mExperimentalHwBuffSize != -1) {
            mDbg.trace("#### EXPERIMENTAL #### HW BUFFER SIZE BEING SET TO: " + mExperimentalHwBuffSize);
            core.setInedaHwBufferSize(mExperimentalHwBuffSize);
        }

        mIsCableRemote = accessory.isRemote();

        core.start(accessory);
        mMeemCoreHandler = (MeemCoreV2Handler) core.getHandler();

        return doCableInit(accessory, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mMeemCoreHandler.releaseBigCableLock(mPhoneUpid, new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return responseCallback.execute(result, info, extraInfo);
                    }
                });

                return result;
            }
        });
    }

    private boolean doCableInit(AccessoryInterface accessory, final ResponseCallback responseCallback) {
        mDbg.trace();

        return mMeemCoreHandler.addRequest(new CableInitV2(accessory.isSlowSpeedMode(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                CableInitV2.InitSeqResponseParams params = (CableInitV2.InitSeqResponseParams) info;

                mSerialNo = params.serialNo;
                mFwVersion = params.fwVersion;
                final byte pinStatus = params.pinStatus;
                mFwDbStatus = params.dbStatus;
                mFwDelPendingUpid = params.delPendingUpid;

                mDbg.trace("Firmware version: " + mFwVersion + ", serial no: " + mSerialNo
                        + ", db status: " + Integer.toHexString(mFwDbStatus)
                        + ", del pending for upid: " + mFwDelPendingUpid);

                mIsConnected = true;

                return mListener.earlyFirmwareHook(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        switch (pinStatus) {
                            case MMPV2Constants.INIT_SEQ_PIN_STATUS_NOT_SET:
                                mDbg.trace("Virgin cable connected");
                                mIsCableVirgin = true;
                                mListener.onVirginCableConnection();
                                break;
                            case MMPV2Constants.INIT_SEQ_PIN_STATUS_LOCKED:
                                mDbg.trace("Cable is locked");
                                mIsCableLocked = true;
                                mIsConnected = false; // no further comm is allowed.
                                mListener.onLockedCableConnected();
                                break;
                            case MMPV2Constants.INIT_SEQ_PIN_STATUS_IS_SET:
                                mDbg.trace("Cable has pin, going to build model");
                                mAuthMethod = MMPV2Constants.MMP_CODE_PIN_AUTH;
                                refreshCableModel(true, responseCallback);
                                break;
                            default:
                                mListener.onCriticalError("Unknown pin status: " + pinStatus);
                                break;
                        }

                        return result;
                    }
                });
            }
        }));
    }

    private boolean refreshCableModel(final boolean needSecureDb, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return fetchConfigDb(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    mDbg.trace("Fetching configdb succeeded. Checking phone reg");
                } else {
                    mDbg.trace("Fetching configdb failed. Proceeding anyway with reg check");
                }

                return checkRegistrationAndGetSecureDb(needSecureDb, new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        return responseCallback.execute(result, info, extraInfo);
                    }
                });
            }
        });
    }

    private boolean checkRegistrationAndGetSecureDb(boolean needSecureDb, ResponseCallback responseCallback) {
        mDbg.trace();

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();

        mNumVaults = configDb.getNumVaults();
        boolean isRegisteredPhone = configDb.isVaultInfoAvailable(mPhoneUpid);

        configDb.closeDatabase();

        if (!isRegisteredPhone) {
            if (mNumVaults >= ProductSpecs.LIMIT_MAX_VAULTS) {
                // No go!
                mListener.onMaxNumVaultsDetected();
                return false;
            }
            mListener.onUnregisteredPhoneConnection();
        } else {
            if (needSecureDb) {
                return fetchSecureDb(responseCallback);
            } else {
                mDbg.trace("We don't need to fetch securedb now");
                return responseCallback.execute(true, null, null);
            }
        }

        return true;
    }

    private boolean fetchConfigDb(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new MMPV2RecvFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_CONFIG_DB_FROM_MEEM, (byte) 0, (byte) 0, mConfigDbPath, "configdb", GenUtils.dummyMD5(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("configdb received: " + result);

                if (!result) {
                    File configDbFileObj = new File(mConfigDbPath);
                    if (configDbFileObj != null && configDbFileObj.exists()) {
                        mDbg.trace("NOT removing existing configdb at: " + mConfigDbPath);
                        // SQLiteDatabase.deleteDatabase(new File(mConfigDbPath));
                    } else {
                        mDbg.trace("WTF: Configdb does not exist at: " + mConfigDbPath);
                    }
                } else {
                    GenUtils.copyDbFileToDownloads("config.db", "config-init.db");

                    // Arun: Added 05Sept2017: Since Fw can not delete tables all the time, we have to do it
                    ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
                    configDb.openDatabase();

                    mSecureDbCleanupPending = configDb.isCleanupPending();
                    mSecureDbCleanupPendingUpid = configDb.getCleanupPendingUpid();

                    configDb.closeDatabase();
                }

                return responseCallback.execute(result, null, null);
            }
        }));
    }

    private boolean sendConfigDb(boolean isCreateVault, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        // This is ugly!
        byte fType = isCreateVault ? MMPV2Constants.MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_CREATE_VAULT : MMPV2Constants.MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_SET_VCFG;

        return mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, fType, (byte) 0, (byte) 0, mConfigDbPath, "config.db", GenUtils.getFileMD5(mConfigDbPath, null), 0, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Configdb sent, result: " + result);
                return refreshCableModel(true, responseCallback);
            }
        }));
    }

    private boolean fetchSecureDb(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        String path = AppLocalData.getInstance().getSecureDbFullPath();

        return mMeemCoreHandler.addRequest(new MMPV2RecvFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_SECURE_DB_FROM_MEEM, (byte) 0, (byte) 0, path, "securedb", GenUtils.dummyMD5(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Fetch securedb, result: " + result);
                if (!result) {
                    mDbg.trace("Failed to get securedb. Removing any existing securedb and creating a fresh one");
                    SQLiteDatabase.deleteDatabase(new File(mSecureDbPath));
                } else {
                    mDbg.trace("Copying securedb to downloads");
                    GenUtils.copyDbFileToDownloads("secure.db", "secure-init.db");
                }

                // Arun: Added 05Sept2017: For dropping deleted vault's table, which causes OOM errors in FW.
                // Now, there is a fair chance we are dropping tables for this phone's data and immediately below going to create
                // them again... well!
                return performSecureDbPendingCleanup(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        // Whatever it may be, we must sanitize (create tables if they do not exist)
                        SecureDb secureDb = new SecureDb(mPhoneUpid);
                        secureDb.sanitize();
                        secureDb.close();

                        return responseCallback.execute(true, info, extraInfo);
                    }
                });
            }
        }));
    }


    // Arun: Added 05Sept2017: For dropping deleted vault's table, which causes OOM errors in FW.
    private boolean performSecureDbPendingCleanup(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mSecureDbCleanupPending) {
            mDbg.trace("No pending cleanups");
            return responseCallback.execute(false, null, null);
        }

        mDbg.trace("Performing pending cleanup for upid: " + mSecureDbCleanupPendingUpid);
        SecureDb secureDb = new SecureDb(mSecureDbCleanupPendingUpid);
        secureDb.dropAllTables();
        secureDb.close();

        mSecureDbCleanupPending = false;

        // clear the pending cleanup in configdb and send it back.
        // this is *the* most ugly aspect of current implementation - sending databases back and forth.
        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();
        configDb.clearCleanupPending();
        configDb.closeDatabase();

        // send the cleaned secureDb and then configdb (this will avoid issues due to disconnections in between)
        return sendSecureDb(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Securedb sent after cleanup, result: " + result + "\nSending configdb now.");
                return mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_SET_VCFG, (byte) 0, (byte) 0, mConfigDbPath, "config.db", GenUtils.getFileMD5(mConfigDbPath, null), 0, new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        mDbg.trace("Configdb sent after cleanup, result: " + result);
                        return responseCallback.execute(result, null, null);
                    }
                }));
            }
        });
    }

    private boolean sendSecureDb(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (responseCallback == null) {
            return synSendSecureDb();
        }

        String path = AppLocalData.getInstance().getSecureDbFullPath();

        // send the latest securedb to cable
        return mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_SECURE_DB_TO_MEEM, (byte) 0, (byte) 0, path, "secure.db", GenUtils.getFileMD5(path, null), 0, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Securedb sent, result: " + result);
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    private boolean synSendSecureDb() {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        String path = AppLocalData.getInstance().getSecureDbFullPath();
        final SynOp syn = new SynOp();

        // send the latest securedb to cable
        mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_SECURE_DB_TO_MEEM, (byte) 0, (byte) 0, path, "secure.db", GenUtils.getFileMD5(path, null), 0, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Securedb sent (syn), result: " + result);

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        boolean result = false;
        synchronized (syn.getMonitor()) {
            result = syn.waitForResult();
        }

        return result;
    }

    // =================================================================
    // ---------------------- public methods ---------------------------
    // =================================================================

    @Override
    public int getAuthMethod() {
        mDbg.trace();
        return MMPV2Constants.MMP_CODE_PIN_AUTH;
    }

    @Override
    public boolean onCableDisconnect() {
        mDbg.trace();

        mIsConnected = false;
        mIsCableRemote = false;

        return true;
    }

    @Override
    public boolean onPhoneAuthSucceeded(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        mDbg.trace("Registering phone by populating tables...");

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();

        PhoneDbModel phoneModel = mListener.getThisPhoneDbModel();
        VaultDbModel vaultModel = mListener.getThisPhonesVaultDbModel();

        configDb.insertIntoPinfoTable(phoneModel);
        configDb.insertIntoVInfoTable(vaultModel);

        configDb.closeDatabase();

        mDbg.trace("Sending config database (create vault)...");

        return sendConfigDb(true, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    mListener.onCriticalError("Vault creation failed!");
                }
                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    @Override
    public void onPhoneAuthFailed() {
        mDbg.trace();
    }

    @Override
    public boolean onVirginCablePINSettingFinished(ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase(); // internally, this will sanitize the db.
        configDb.closeDatabase();

        // we can directly go to register this phone.
        return onPhoneAuthSucceeded(responseCallback);
    }

    @Override
    public boolean setPIN(String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new SetPinV2(pin, recoveryAnswers, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    // initially mAuthMethod is initialized as "LOCKED".
                    mAuthMethod = MMPV2Constants.MMP_CODE_PIN_AUTH;
                }

                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean performAuth(String pin, ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new AuthPhoneV2(pin, responseCallback));
    }

    /**
     * Cable presenter is supposed to update configdb and then call this method to send the db to cable.
     */
    @Override
    public boolean setCableName(String name, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();
        configDb.setCableName(name);
        configDb.closeDatabase();

        return sendConfigDb(false, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    return refreshCableModel(false, responseCallback);
                }
                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    /**
     * Cable presenter is supposed to update configdb and then call this method to send the db to cable.
     */
    @Override
    public boolean setVaultConfig(String upid, MMPFPath vcfgPath, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return sendConfigDb(false, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (result) {
                    return refreshCableModel(false, responseCallback);
                }
                return responseCallback.execute(result, info, extraInfo);
            }
        });
    }

    /**
     * Not supported in V2
     */
    @Override
    public boolean getThumbnailDb(String dbPath, int dbVersion, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(false, null, null);
    }

    /**
     * Not supported in V2
     */
    @Override
    public boolean sendThumbnailDb(String dbPath, long dbSize, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(false, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean deleteGenericData(ArrayList<MMPSingleFileSpec> specList, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(false, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean deleteCategory(MMPDeleteCategorySpec delSpec, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    @Override
    public boolean deleteVault(final String upid, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new DeleteVaultV2(upid, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                responseCallback.execute(result, info, extraInfo);

                // we can not do this in cablepresenterv2 - because of a firmware bug related
                // to "out of memmory" errors during vault deletion. So, we will have to delete smart data from driver
                // and reboot immediately.
                if (result) {
                    mDbg.trace("Vault delete succeeded. Cleaning up securedb, deleting local smart data and rebooting cable.");
                    SecureDb secureDb = new SecureDb(mPhoneUpid);
                    secureDb.dropAllTables();
                    secureDb.close();

                    deleteAllSmartData(upid);
                } else {
                    mDbg.trace("Vault delete failed. Rebooting anyway");
                }

                return mMeemCoreHandler.addRequest(new RebootV2(new ResponseCallback() {
                    @Override
                    public boolean execute(boolean result, Object info, Object extraInfo) {
                        mDbg.trace("Reboot response received.");
                        return result;
                    }
                }));
            }
        }));
    }

    @Override
    public boolean resetCable(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new CableResetV2(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Cable reset result: " + result);
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    @Override
    public boolean updateFirmware(String fwPath, ResponseCallback responseCallback) {
        mDbg.trace();

        if (isRemoteCable()) {
            mDbg.trace("Remote firmware update is not recommended. Don't do that.");
            return responseCallback.execute(false, null, null);
        }

        return mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_FW_BINARY, (byte) 0, (byte) 0, fwPath,
                "fwupdate.dat", GenUtils.getFileMD5(fwPath, null), 0, responseCallback));
    }

    /**
     * Not used
     */
    @Override
    public boolean getSingleFile(MMPSingleFileSpec fileSpec, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean getSingleSmartData(MMPSingleSmartDataSpec fileSpec, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(false, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean getSessionlessSmartData(ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean getSmartData(byte handle, SessionSmartDataInfo sesSmartInfo, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean getCopySmartData(byte handle, SessionSmartDataInfo sesSmartInfo, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    /**
     * Not used
     */
    @Override
    public boolean getDATD(MMPUpid upid, MMPUmid umid, int sessionType, ResponseCallback responseCallback) {
        mDbg.trace();
        return responseCallback.execute(true, null, null);
    }

    /**
     * Used in V2 to send secureDb as apart of backup
     */
    @Override
    public boolean executeSession(byte handle, MMPFPath fpathSesd, MMPFPath fpathThumbDb, boolean isCopy, ResponseCallback responseCallback) {
        mDbg.trace("executeSession: SendSecureDb");

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return sendSecureDb(responseCallback);
    }

    /**
     * In V2, this is exactly an abort.
     */
    @Override
    public boolean closeSession(byte handle, ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        boolean result = abortSession();
        return responseCallback.execute(result, null, null);
    }

    @Override
    public boolean getSessionlessGenericData(ArrayList<MMPSingleFileSpec> specList, ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        for (MMPSingleFileSpec spec : specList) {
            MMLGenericDataDesc desc = new MMLGenericDataDesc();

            synFetchGenericData(spec.getUpid().toString(), desc);
        }

        return responseCallback.execute(true, null, null);
    }

    @Override
    public boolean abortSession() {
        mDbg.trace();
        return mMeemCoreHandler.notifyAbortRequest();
    }

    @Override
    public void resetXfrStats() {
        mDbg.trace();

        MeemCoreV2 core = MeemCoreV2.getInstance();
        core.resetXfrStat();
    }

    @Override
    public double getXfrStats() {
        /*mDbg.trace();*/
        MeemCoreV2 core = MeemCoreV2.getInstance();
        return core.getXfrStat();
    }

    @Override
    public void sendAppQuit() {
        mDbg.trace();
        mMeemCoreHandler.addRequest(new SendAppQuitV2(null));
    }

    @Override
    public int getCableVersion() {
        return ProductSpecs.HW_VERSION_2_INEDA;
    }

    /**
     * Very important function!!
     *
     * @param vaultInfo
     * @param responseCallback
     *
     * @return
     */
    public boolean updateVault(VaultInfo vaultInfo, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();
        boolean result = configDb.updateVaultInfo(vaultInfo);
        configDb.closeDatabase();

        GenUtils.copyDbFileToDownloads("config.db", "config-updt.db");

        if (result) {
            return sendConfigDb(false, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mDbg.trace("Configdb update result: " + result);
                    // Arun: 08June2017: Why do we refresh the models? All models and DBs are updated anyway.
                    /*return refreshCableModel(false, new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            return responseCallback.execute(result, info, extraInfo);
                        }
                    });*/
                    return responseCallback.execute(result, info, extraInfo);
                }
            });
        } else {
            return responseCallback.execute(result, null, null);
        }
    }

    // ================================================================================================================
    // ---- The most important stuff - data xfr! ---- Good that send and receive params are exactly the same.
    // ================================================================================================================
    private void handleLocalPathPrefix(MMLGenericDataDesc desc, boolean isSending) {
        mDbg.trace();

        if (desc.mIsTemp) {
            mDbg.trace("No path conversion for temp file: " + desc);
            return;
        }

        if (desc.onSdCard) {
            if (!isSending) {
                // TODO: Arun: Handle SDCARD new document framework fore writing in Android 6.0
                desc.mPath = mStorage.toSecExtPrivateAbsPath(desc.mPath);
            } else {
                desc.mPath = mStorage.toSecExtAbsPath(desc.mPath);
            }
        } else {
            // TODO: this is hack. Should have added platform code in desc.
            if (desc.mPath.contains("assets-library://")) {
                mDbg.trace("Sanitizing ios uri for receiving: " + desc.mPath);
                desc.mPath = GenUtils.sanitizeIOSAssetLibraryItemPath_MeemV2(desc.mPath);
                mDbg.trace("Sanitized path is: " + desc.mPath);
            }
            desc.mPath = mStorage.toPrimaryExtAbsPath(desc.mPath);
        }
    }

    @Override
    public boolean sendGenericData(String upid, MMLGenericDataDesc desc, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        handleLocalPathPrefix(desc, true);

        if (null == responseCallback) {
            return synSendGenericData(upid, desc);
        } else {
            return mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPath, desc.mMeemInternalPath, desc.mCSum, desc.mRowId, new ResponseCallback() {
                @Override // will be executed in ui thread context
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mDbg.trace("execute: send async (gendata): " + (String) info + ", result: " + result);
                    return responseCallback.execute(result, info, extraInfo);
                }
            }));
        }
    }

    private boolean synSendGenericData(String upid, MMLGenericDataDesc desc) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        final SynOp syn = new SynOp();

        mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPath, desc.mMeemInternalPath, desc.mCSum, desc.mRowId, new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: send syn (gendata): " + (String) info + ", result: " + result);

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileSent = new MeemEvent(EventCode.FILE_SENT_TO_MEEM, desc.mPath);
            mUiCtxt.postEvent(fileSent);
            return syn.getResult();
        }
    }

    @Override
    public boolean fetchGenericData(String upid, MMLGenericDataDesc desc, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        handleLocalPathPrefix(desc, false);

        if (null == responseCallback) {
            return synFetchGenericData(upid, desc);
        } else {
            return mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPath, desc.mMeemInternalPath, desc.mCSum, new ResponseCallback() {
                @Override // will be executed in ui thread context
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mDbg.trace("execute: recv async (gendata): " + (String) info + ", result: " + result);
                    return responseCallback.execute(result, info, extraInfo);
                }
            }));
        }
    }

    private boolean synFetchGenericData(String upid, MMLGenericDataDesc desc) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        final SynOp syn = new SynOp();

        mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPath, desc.mMeemInternalPath, desc.mCSum, new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: recv syn (gendata): " + (String) info + ", result: " + result);

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileRcvd = new MeemEvent(EventCode.FILE_RECEIVED_FROM_MEEM, desc.mPath);
            mUiCtxt.postEvent(fileRcvd);
            return syn.getResult();
        }
    }

    @Override
    public boolean sendSmartData(final String upid, final MMLSmartDataDesc desc, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (null == responseCallback) {
            return synSendSmartData(upid, desc);
        }
        return mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPaths[0], desc.mMeemPaths[0], desc.mCSums[0], 0, new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: send async (smartdata, mirror): " + (String) info + ", result: " + result);

                if (desc.mPaths[1] != null) {
                    mDbg.trace("Continuing send (smartdata, plus)");

                    return mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 1, desc.mCatCode, desc.mPaths[1], desc.mMeemPaths[1], desc.mCSums[1], 0, new ResponseCallback() {
                        @Override // will be executed in ui thread context
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            mDbg.trace("execute: send async (smartdata, plus): " + (String) info + ", result: " + result);
                            return responseCallback.execute(result, info, extraInfo);
                        }
                    }));
                } else {
                    return responseCallback.execute(result, info, extraInfo);
                }
            }
        }));
    }

    private boolean synSendSmartData(String upid, MMLSmartDataDesc desc) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        final SynOp syn = new SynOp();

        mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPaths[0], desc.mMeemPaths[0], desc.mCSums[0], 0, new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: send syn (smartdata, mirror): " + (String) info + ", result: " + result);

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileSent = new MeemEvent(EventCode.FILE_SENT_TO_MEEM, desc.mPaths[0]);
            mUiCtxt.postEvent(fileSent);
        }

        mDbg.trace("Wait #1 over");

        if (!checkConnection(null)) {
            return false;
        }

        mMeemCoreHandler.addRequest(new MMPV2SendFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 1, desc.mCatCode, desc.mPaths[1], desc.mMeemPaths[1], desc.mCSums[1], 0, new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: send syn (smartdata, plus): " + (String) info + ", result: " + result);

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileSent = new MeemEvent(EventCode.FILE_SENT_TO_MEEM, desc.mPaths[1]);
            mUiCtxt.postEvent(fileSent);
        }

        mDbg.trace("Wait #2 over");

        return syn.getResult();
    }

    @Override
    public boolean fetchSmartData(final String upid, final MMLSmartDataDesc desc, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (null == responseCallback) {
            return synFetchSmartData(upid, desc);
        }

        mDbg.trace("Fetching: " + desc.mPaths[0]);
        return mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPaths[0], desc.mMeemPaths[0], desc.mCSums[0], new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: recv async (smartdata, mirror): " + (String) info + ", result: " + result);

                if (!result) {
                    mDbg.trace("Recv failed. truncating smartdata file: " + desc.mPaths[0]);
                    AppLocalData.getInstance().truncate(desc.mPaths[0]);
                }

                if (desc.mPaths[1] != null) {
                    mDbg.trace("Continuing receive (smartdata, plus)");
                    mDbg.trace("Fetching: " + desc.mPaths[1]);

                    return mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 1, desc.mCatCode, desc.mPaths[1], desc.mMeemPaths[1], desc.mCSums[1], new ResponseCallback() {
                        @Override // will be executed in ui thread context
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            mDbg.trace("execute: recv async (smartdata, plus): " + (String) info + ", result: " + result);

                            if (!result) {
                                mDbg.trace("Recv failed. truncating smartdata file: " + desc.mPaths[1]);
                                AppLocalData.getInstance().truncate(desc.mPaths[1]);
                            }

                            return responseCallback.execute(result, info, extraInfo);
                        }
                    }));
                } else {
                    return responseCallback.execute(result, info, extraInfo);
                }
            }
        }));
    }

    private boolean synFetchSmartData(String upid, final MMLSmartDataDesc desc) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        final SynOp syn = new SynOp();

        mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 0, desc.mCatCode, desc.mPaths[0], desc.mMeemPaths[0], desc.mCSums[0], new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: recv syn (smartdata, mirror): " + (String) info + ", result: " + result);

                if (!result) {
                    mDbg.trace("Recv failed. truncating smartdata file: " + desc.mPaths[0]);
                    AppLocalData.getInstance().truncate(desc.mPaths[0]);
                }

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileRcvd = new MeemEvent(EventCode.FILE_RECEIVED_FROM_MEEM, desc.mPaths[0]);
            mUiCtxt.postEvent(fileRcvd);
        }

        mDbg.trace("Wait #1 over");

        if (!checkConnection(null)) {
            return false;
        }

        mMeemCoreHandler.addRequest(new MMPV2RecvFile(upid, MMPV2Constants.MMP_FILE_XFR_TYPE_DATA, (byte) 1, desc.mCatCode, desc.mPaths[1], desc.mMeemPaths[1], desc.mCSums[1], new ResponseCallback() {
            @Override // will be executed in ui thread context
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("execute: recv syn (smartdata, plus): " + (String) info + ", result: " + result);

                if (!result) {
                    mDbg.trace("Recv failed. truncating smartdata file: " + desc.mPaths[1]);
                    AppLocalData.getInstance().truncate(desc.mPaths[1]);
                }

                synchronized (syn.getMonitor()) {
                    syn.setResult(result, info, extraInfo);
                    syn.notifyResult();
                }

                return result;
            }
        }));

        synchronized (syn.getMonitor()) {
            syn.waitForResult();

            MeemEvent fileRcvd = new MeemEvent(EventCode.FILE_RECEIVED_FROM_MEEM, desc.mPaths[1]);
            mUiCtxt.postEvent(fileRcvd);
        }

        mDbg.trace("Wait #2 over");

        return syn.getResult();
    }

    @Override
    public String getFwVersion() {
        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return null;
        }

        return mFwVersion;
    }

    @Override
    public String getSerialNo() {
        if (!mIsConnected) {
            mDbg.trace("Rejected. Cable is disconnected");
            return null;
        }

        return mSerialNo;
    }

    @Override
    public boolean refreshAllDatabases(ResponseCallback responseCallback) {
        if (!checkConnection(responseCallback)) {
            return false;
        }

        return refreshCableModel(true, responseCallback);
    }

    /**
     * This is a synchronous call! Shall not be called from ui thread
     *
     * @param upid
     * @param cats
     *
     * @return
     */
    @Override
    public boolean cleanupCable(String upid, ArrayList<Byte> cats) {
        mDbg.trace();

        if (!checkConnection(null)) {
            return false;
        }

        final SynOp syn = new SynOp();

        for (Byte cat : cats) {
            mMeemCoreHandler.addRequest(new CableCleanupV2(upid, cat, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    synchronized (syn.getMonitor()) {
                        syn.setResult(result, info, extraInfo);
                        syn.notifyResult();
                    }

                    return result;
                }
            }));

            synchronized (syn.getMonitor()) {
                syn.waitForResult();
            }

            if (!checkConnection(null)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkConnection(ResponseCallback responseCallback) {
        if (!mIsConnected) {
            if (null != responseCallback) {
                responseCallback.execute(false, null, null);
            }
            return false;
        } else {
            return true;
        }
    }

    private void deleteAllSmartData(final String upid) {
        mDbg.trace();

        AppLocalData appData = AppLocalData.getInstance();
        byte[] cats = MMLCategory.getAllSmartCatCodes();

        for (byte cat : cats) {
            SQLiteDatabase.deleteDatabase(new File(appData.getSmartDataV2DatabasePath(upid, cat, true)));
            SQLiteDatabase.deleteDatabase(new File(appData.getSmartDataV2DatabasePath(upid, cat, false)));
        }
    }

    /**
     * ONLY FOR DEBUGGING AND EXPERIMENTS
     *
     * @param bufSize
     */
    @Override
    public void setExperimentalHwBufferSize(int bufSize) {
        mExperimentalHwBuffSize = bufSize;
    }

    // ===================================
    // ---- supporting synchronous xfr
    // ===================================
    private class SynOp {
        public Object mMonitor;
        public boolean mResultRcvd;

        public boolean mResult;
        public Object mInfo;
        public Object mExtraInfo;

        public SynOp() {
            mMonitor = new Object();
            mResultRcvd = false;
        }

        public Object getMonitor() {
            return mMonitor;
        }

        /**
         * Note that this method will reset the resultRcvd to false before returning.
         * Otherwise a clear bug will happen if someone reuse the object
         *
         * @return
         */
        public boolean waitForResult() {
            boolean result = true;

            mDbg.trace("SynOp: Waiting for result");

            while (!mResultRcvd) {
                try {
                    mMonitor.wait();
                } catch (Exception e) {
                    mDbg.trace("Exception while waiting for send resp: " + e.getMessage());
                    result = false;
                }
            }

            mDbg.trace("SynOp: Wait ended");

            mResultRcvd = false;
            return result;
        }

        public void setResult(boolean result, Object info, Object extraInfo) {
            mResult = result;
            mInfo = info;
            mExtraInfo = extraInfo;
            mResultRcvd = true;
        }

        public void notifyResult() {
            mMonitor.notifyAll();
        }

        public boolean getResult() {
            return mResult;
        }
    }

    // ================== For MEEM Desktop
    @Override
    public boolean changeModeOfMeem(byte newMode, ResponseCallback responseCallback) {
        return mMeemCoreHandler.addRequest(new CableModeSwitchV2(newMode, responseCallback));
    }

    // ================== For network
    @Override
    public boolean isRemoteCable() {
        mDbg.trace();

        if (!isCableConnected()) {
            return false;
        }

        return mIsCableRemote;
    }

    @Override
    public boolean acquireBigCableLock(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (null != mMeemCoreHandler) {
            mMeemCoreHandler.acquireBigCableLock(mPhoneUpid, responseCallback);
        }

        return true;
    }

    @Override
    public boolean releaseBigCableLock(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (null != mMeemCoreHandler) {
            mMeemCoreHandler.releaseBigCableLock(mPhoneUpid, responseCallback);
        }

        return true;
    }

    @Override
    public boolean sendMessageToNetMaster(int msgCode, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        if (null != mMeemCoreHandler) {
            mMeemCoreHandler.sendMessageToNetMaster(msgCode, responseCallback);
        }

        return false;
    }

    @Override
    public boolean savePolicyIsNetworkSharingEnabled(boolean enable, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        ConfigDb configDb = new ConfigDb(mPhoneUpid, mConfigDbPath);
        configDb.openDatabase();
        configDb.setPolicyIsNetworkSharingEnabled(enable);
        configDb.closeDatabase();

        return mMeemCoreHandler.addRequest(new MMPV2SendFile(mPhoneUpid, MMPV2Constants.MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_SET_VCFG, (byte) 0, (byte) 0, mConfigDbPath, "config.db", GenUtils.getFileMD5(mConfigDbPath, null), 0, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Configdb sent to save updated network policy, result: " + result);

                if (result) {
                    mDbg.trace("Sending ui refresh to all clients upon policy update");
                    mListener.sendUiRefreshMessageToNetClients();
                }

                return responseCallback.execute(result, null, null);
            }
        }));
    }

    // Arun: 05July2018 (1.0.63) : added for pin recovery feature
    public boolean validateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!checkConnection(responseCallback)) {
            return false;
        }

        return mMeemCoreHandler.addRequest(new PinRecoveryV2(recoveryAnswers, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                String msg = "Pin recovery answer validation result: " + result;
                /*mListener.showToast(msg); // TODO: Remove.*/
                mDbg.trace(msg);
                return responseCallback.execute(result, info, extraInfo);
            }
        }));
    }

    // Arun: Added 20Aug2018: See usages.
    public int getFwDbStatus() {
        return mFwDbStatus;
    }

    // Arun: Added 20Aug2018: See usages.
    public String getFwDelPendingUpid() {
        return mFwDelPendingUpid;
    }
}
