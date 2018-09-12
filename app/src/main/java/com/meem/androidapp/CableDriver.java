package com.meem.androidapp;

import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.cablemodel.MeemCable;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPDeleteCategorySpec;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPSingleFileSpec;
import com.meem.mmp.messages.MMPSingleSmartDataSpec;
import com.meem.mmp.messages.MMPUmid;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.utils.DebugTracer;
import com.meem.v2.cablemodel.PhoneDbModel;
import com.meem.v2.cablemodel.VaultDbModel;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * CableDriver class essentially manages a MeemCable instance and handles all MMP communication between cable and app. It will be used by
 * CablePresenter in such a way that all model objects are created and managed by CablePresenter.
 * Beware: You wont understand anything here unless you are familiar with app v1 design and implementation and ofcourse the MMP specs!
 * <p>
 * Created by arun on 22/2/16.
 * <p>
 * Major change 01May2017: Made this an abstract base class and moved all major implementations to CableDriverV1 class. This is to
 * facilitate the creation of CableDriverV2 class to handle V2 MEEM hardware (Ineda)
 */
abstract public class CableDriver {
    protected CableDriverListener mListener;
    protected DebugTracer mDbg;

    protected MeemCable mCableModel;
    protected int mAuthMethod = MMPConstants.MMP_ERROR_CABLE_LOCKED;

    protected boolean mIsConnected;

    public CableDriver(CableDriverListener listener) {
        mListener = listener;
    }

    public boolean isCableConnected() {
        return mIsConnected;
    }

    /**
     * The method to be used by cable presenter class - which shall create view-model objects representing the returned real model object
     *
     * @return Final instance of Cable Model.
     */
    public final MeemCable getCableModel() {
        mDbg.trace();

        if (!mIsConnected) {
            mDbg.trace("WARNING! Cable is dosconnected.");

            // Arun: 07Dec2016: Should not return null here.
            // Imagine a drag n dop happend for restore operation. Animations of drop takes about 0.5 seconds and user can disconnect the
            // cable. The onDisconnect will make the mIsConnect flag to false. Now, the onAnimationEnd finishes and eventually SessionManager
            // calls this method and it never expects a null model and boom!.

            // In short: Model objects in this design shall never be set to null.
        }

        return mCableModel;
    }

    abstract String getFwVersion();
    abstract String getSerialNo();

    // Arun: Added 20Aug2018: See usages.
    abstract int getFwDbStatus();
    abstract String getFwDelPendingUpid();


    /**
     * This method will ultimately initialize the cable and make it ready for operations. The logical sequence of operations are as
     * follows:
     * <p>
     * a) Get the instance of the meem core singleton object. b) Start it to work with the cable accessory object passed to us by Android.
     * c) Get the core handler object. d) Queue the CableIinit MMP handler to core handler. e) Probe for the cable (which will initiate the
     * handshake sequence) which will trigger the firmware to initiate the startup sequence, which starts with GET_TIME command, which will
     * be handled by the CableInit MMP Handler already queued in MeemCoreHandler.
     *
     * @param accessory
     *
     * @return
     */
    abstract public boolean onCableConnect(AccessoryInterface accessory, final ResponseCallback responseCallback);

    abstract public int getCableVersion();

    abstract public int getAuthMethod();

    abstract public boolean onCableDisconnect();

    /**
     * Phone is authenticated. Now register it and create model objects
     */
    abstract public boolean onPhoneAuthSucceeded(final ResponseCallback responseCallback);

    /**
     * Just a place holder. Should not be called normally.
     */
    abstract public void onPhoneAuthFailed();

    abstract public boolean onVirginCablePINSettingFinished(final ResponseCallback responseCallback);

    // ===============================================================================================================================
    // Misc cable operations
    // ===============================================================================================================================

    abstract public boolean setPIN(String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback);


    /**
     * For authenticating using PIN
     *
     * @param pin              User supplied PIN
     * @param responseCallback Result in response callback will always be true. the info argument is an Integer, the values are as follows:
     *                         0: If PIN matched, MMPConstants.MMP_ERROR_PIN_MISMATCH: Incorrect PIN, MMPConstants.MMP_ERROR_CABLE_LOCKED:
     *                         User reached max trials. Cable is locked now!
     *
     * @return
     */
    abstract public boolean performAuth(String pin, final ResponseCallback responseCallback);

    abstract public boolean setCableName(String name, final ResponseCallback responseCallback);

    abstract public boolean setVaultConfig(String upid, MMPFPath vcfgPath, final ResponseCallback responseCallback);

    abstract public boolean getThumbnailDb(String dbPath, int dbVersion, final ResponseCallback responseCallback);

    abstract public boolean sendThumbnailDb(String dbPath, long dbSize, final ResponseCallback responseCallback);

    abstract public boolean deleteGenericData(ArrayList<MMPSingleFileSpec> specList, final ResponseCallback responseCallback);

    abstract public boolean deleteCategory(MMPDeleteCategorySpec delSpec, final ResponseCallback responseCallback);

    abstract public boolean deleteVault(final String upid, final ResponseCallback responseCallback);

    abstract public boolean resetCable(final ResponseCallback responseCallback);

    abstract public boolean updateFirmware(String fwPath, final ResponseCallback responseCallback);

    abstract public boolean getSingleFile(MMPSingleFileSpec fileSpec, final ResponseCallback responseCallback);

    abstract public boolean getSingleSmartData(MMPSingleSmartDataSpec fileSpec, final ResponseCallback responseCallback);

    abstract public boolean getSessionlessSmartData(final ResponseCallback responseCallback);

    abstract public boolean getSmartData(byte handle, SessionSmartDataInfo sesSmartInfo, final ResponseCallback responseCallback);

    abstract public boolean getCopySmartData(byte handle, SessionSmartDataInfo sesSmartInfo, final ResponseCallback responseCallback);

    abstract public boolean getDATD(MMPUpid upid, MMPUmid umid, int sessionType, final ResponseCallback responseCallback);

    abstract public boolean executeSession(byte handle, MMPFPath fpathSesd, MMPFPath fpathThumbDb, boolean isCopy, final ResponseCallback responseCallback);

    abstract public boolean closeSession(byte handle, final ResponseCallback responseCallback);

    abstract public boolean getSessionlessGenericData(ArrayList<MMPSingleFileSpec> specList, final ResponseCallback responseCallback);

    /**
     * You must carefully go through comments in V1 app to understand why this method is not same as other methods.
     *
     * @return
     */
    abstract public boolean abortSession();

    abstract public void resetXfrStats();

    abstract public double getXfrStats();

    abstract public void sendAppQuit();

    // ==== Design extension for V2
    abstract public boolean sendSmartData(final String upid, final MMLSmartDataDesc desc, final ResponseCallback responseCallback);
    abstract public boolean fetchSmartData(final String upid, final MMLSmartDataDesc desc, final ResponseCallback responseCallback);
    abstract public boolean sendGenericData(String upid, MMLGenericDataDesc desc, final ResponseCallback responseCallback);
    abstract public boolean fetchGenericData(String upid, MMLGenericDataDesc desc, final ResponseCallback responseCallback);
    abstract public boolean refreshAllDatabases(ResponseCallback responseCallback);
    abstract public boolean cleanupCable(String upid, ArrayList<Byte> cats);
    abstract public boolean updateVault(VaultInfo vaultInfo, ResponseCallback responseCallback);

    abstract void setExperimentalHwBufferSize(int bufSize);

    abstract public boolean changeModeOfMeem(byte newMode, ResponseCallback responseCallback);

    /**
     * This interface is to be implemented by MainActivity. Invoking many of this interface methods will cause MainActivity to tell
     * CablePresenter to arrange for certain views which will get user inputs to proceed further.
     */
    public interface CableDriverListener {
        String getPhoneUpid();
        String getPhoneName();
        File createPinf();

        void onLockedCableConnected();
        void onMaxNumVaultsDetected();
        void onVirginCableConnection();
        void onUnregisteredPhoneConnection();
        void onCriticalError(String msg);

        // === For V2
        PhoneDbModel getThisPhoneDbModel();
        VaultDbModel getThisPhonesVaultDbModel();
        boolean earlyFirmwareHook(ResponseCallback responseCallback);
        void sendUiRefreshMessageToNetClients();

        void showToast(String msg); // TODO: Remove this.
    }

    // ===== Design extensions for Network
    public abstract boolean isRemoteCable();
    public abstract boolean acquireBigCableLock(final ResponseCallback responseCallback);
    public abstract boolean releaseBigCableLock(final ResponseCallback responseCallback);
    public abstract boolean sendMessageToNetMaster(int msgCode, final ResponseCallback responseCallback);
    public abstract boolean savePolicyIsNetworkSharingEnabled(boolean enabled, final ResponseCallback responseCallback);

    // Arun: 05July2018 (1.0.63) : added for pin recovery feature
    public abstract boolean validateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
}
