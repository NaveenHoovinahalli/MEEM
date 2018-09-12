package com.meem.fwupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.utils.AsyncDownload;
import com.meem.utils.GenUtils;
import com.meem.utils.VersionString;

import java.io.File;
import java.util.ArrayList;

/**
 * Removed the NetworkStatusReceiver by arun 01Oct2016: It uses depricated and high security APIs. Now firmware update check is only done on
 * cable connection and check for connectivity is made at that time itself.
 * <p>
 * TODO: Update: This class is not used anymore in app. Remove it.
 */

public class FwUpdateManager {
    private static final String mThisToolVersion = "0.0.1.2";
    private static final int MANIFEST_DOWNLOAD_ID = 1111;
    private static final int FIRMWARE_DOWNLOAD_ID = 3333;

    private final BroadcastReceiver mNetworkStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dbgTrace();

            String action = intent.getAction();
            dbgTrace("Network status broadcast received: " + action);

            if (isOnline(context)) {
                dbgTrace("We are online and ready to launch check for firmware updates");
                // TODO: call manager
            } else {
                dbgTrace("We are offline.");
                // TODO: call manager
                return;
            }
        }
    };
    ArrayList<UpdateInfo> mToDoList = new ArrayList<UpdateInfo>();
    ArrayList<UpdateInfo> mCurrentUpdateInfoList = new ArrayList<UpdateInfo>();
    ArrayList<UpdateInfo> mApplicableUpdateInfoList = new ArrayList<UpdateInfo>();
    // example: "http://nand.silvansync.com/test_2/update.mml";
    private String mFwUpdateManifestUrl;
    private UiContext mUiCtxt = UiContext.getInstance();
    private AppLocalData mAppData = AppLocalData.getInstance();
    private String mManifestFileLocalPath = mAppData.getFwManifestFilePath();
    private AsyncDownload mManifestDownloader, mFwDownloader;
    private FwStoreDatabase mFwStoreDb;
    private boolean isOnline = false;
    private boolean mIsBusy = false;

    private MeemFwStatus mMeemFwStatus;

    public FwUpdateManager() {
        mFwStoreDb = new FwStoreDatabase();
        mFwUpdateManifestUrl = getFwUpdateManifestUrl();
    }

    private boolean isOnline(Context context) {
        dbgTrace();

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            dbgTrace("ConnectionManager instance is null!");
            return false;
        }

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        // should check null because in air plain mode it will be null
        return (netInfo != null && netInfo.isConnected() && netInfo.isAvailable());
    }

    public void onCreate() {
        dbgTrace();
        mFwStoreDb = new FwStoreDatabase();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        // when we register, we will get a callback instantaneously
        // with present status of connectivity. So no need to do anything further here.
        try {
            mUiCtxt.getAppContext().registerReceiver(mNetworkStatusReceiver, filter);
        } catch (Exception ex) {
            dbgTrace("Exception during network status receiver registration: " + ex.getMessage());
        }
    }

    public boolean onMobileDeviceOnline() {
        dbgTrace();

        isOnline = true;

        if (mIsBusy) {
            dbgTrace("Firmware update download is already in progress");
            return true;
        }

        if (timeToStartNewFwCheck()) {
            dbgTrace("We are online now & its time for a new fw availability check");
            startManifestDownload();
        } else {
            dbgTrace("We are online now, but we recently completed downloading all avalable FW. Doing nothing now.");
        }
        return true;
    }

    /**
     * If all available FW versions are downloaded, wait till next day for next check.
     *
     * @return boolean
     */
    private boolean timeToStartNewFwCheck() {
        long now = System.currentTimeMillis();
        long last = mFwStoreDb.getAllAvailableFwDownloadedTime();

        if (now >= last + ProductSpecs.FIRMWARE_CHECK_INTERVAL_MS) {
            return true;
        } else {
            long remains = (last + ProductSpecs.FIRMWARE_CHECK_INTERVAL_MS) - now;
            dbgTrace("Next check is scheduled after: " + GenUtils.millisToString(remains));
        }

        return false;
    }

    public boolean onMobileDeviceOffline() {
        dbgTrace();

        isOnline = false;

        if (mIsBusy) {
            cleanup();
            mIsBusy = false;
        }

        return true;
    }

    public void onMSTATReceived(Context context) {
        dbgTrace();

        if (mIsBusy) {
            dbgTrace("Cable is connected. An update check is already in progress. Let it finish");
            return;
        }

        if (isOnline(context)) {
            if (timeToStartNewFwCheck()) {
                dbgTrace("Cable is connected. We are online. No fw check in progress. Starting a new fw check.");
                startManifestDownload();
            } else {
                dbgTrace("Cable is connected. We are online. No fw check in progress. But, we recently completed downloading all avalable FW.");
                /**
                 * 18Nov2015: Process the MSTAT file and keep the manager's fw status info updated.
                 */
                dbgTrace("Anyway, processing the MSTAT file to keep the manager's fw status info updated");
                mMeemFwStatus = new MeemFwStatus();
                if (false == mMeemFwStatus.processMSTAT(mAppData.getMstatPath())) {
                    dbgTrace("Processing the MSTAT file failed!");
                    mMeemFwStatus = null;
                }
            }
        } else {
            dbgTrace("Cable is connected. We are offline. Processing existing manifest file if any.");
            processManifestFile();
        }
    }

    private boolean startManifestDownload() {
        dbgTrace("Starting manifest download...");

        mManifestDownloader = new AsyncDownload();
        mManifestDownloader.setExtraParams(mManifestFileLocalPath, MANIFEST_DOWNLOAD_ID);
        mManifestDownloader.execute(mFwUpdateManifestUrl);

        mIsBusy = true;

        return true;
    }

    private boolean startFwDownload() {
        // take first item in the to-do list and download
        UpdateInfo info = mToDoList.get(0);
        if (info != null) {
            dbgTrace("Starting download of new firmware update: " + info.toDescription());

            // Prepare local file path from version and start download
            info.mFwUpdateLocalFile = mAppData.getFwUpdateFilePath(info.mFwNewVersion);

            mFwDownloader = new AsyncDownload();
            mFwDownloader.setExtraParams(info.mFwUpdateLocalFile, FIRMWARE_DOWNLOAD_ID);
            mFwDownloader.execute(info.mFwUpdateUrl);

            return true;
        }

        return false;
    }

    public void onDownloadSucceeded(int id) {
        dbgTrace();

        if (id == MANIFEST_DOWNLOAD_ID) {
            mManifestDownloader = null;

            mMeemFwStatus = new MeemFwStatus();

            if (false == mMeemFwStatus.processMSTAT(mAppData.getMstatPath())) {
                dbgTrace("MSTAT processing failed!. Will wait till next network state change for further checks.");
                mIsBusy = false;
                return;
            }

            dbgTrace("Got firmware manifest file. processing...");
            processManifestFile();

            if (mToDoList.isEmpty()) {
                dbgTrace("No new firmware to be downloaded. Will wait till next network state change for further checks.");
                mIsBusy = false;
            } else {
                startFwDownload();
            }

        } else if (id == FIRMWARE_DOWNLOAD_ID) {
            mFwDownloader = null;

            if (!mToDoList.isEmpty()) {
                // add the downloaded firmware info into database
                mFwStoreDb.add(mToDoList.get(0));

                // remove it from TODO list
                mToDoList.remove(0);
            }

            if (!mToDoList.isEmpty()) {
                dbgTrace("We have more firmware versions to download.");
                startFwDownload();
            } else {
                dbgTrace("All available firmware versions downloaded.");
                mIsBusy = false;

                // Store the time in db
                mFwStoreDb.setAllAvailableFwDownloadedTime(System.currentTimeMillis());
            }
        }
    }

    public void ondownloadFailed(int id) {
        dbgTrace();

        if (id == MANIFEST_DOWNLOAD_ID) {
            dbgTrace("Manifest file download failed");
            mManifestDownloader = null;
        } else if (id == FIRMWARE_DOWNLOAD_ID) {
            dbgTrace("Firmware file download failed");
            mFwDownloader = null;
        }

        mIsBusy = false;
    }

    private boolean processManifestFile() {
        dbgTrace();
        boolean result = false;

        UpdateManifest currentUpdateManifest = new UpdateManifest(mManifestFileLocalPath);
        mCurrentUpdateInfoList.clear();

        try {
            mCurrentUpdateInfoList = currentUpdateManifest.process();
        } catch (Exception ex) {
            dbgTrace("Failed to process current manifest: " + ex.getMessage());
            return result;
        }

        // now, check with database to see what are the firmware
        // update files already downloaded and to be downloaded now.
        // add the to be downloaded ones to to-do list.
        ArrayList<UpdateInfo> storedUpdateInfoList = mFwStoreDb.getList();
        for (UpdateInfo currentInfo : mCurrentUpdateInfoList) {
            boolean newUpdateFound = true;
            for (UpdateInfo storedInfo : storedUpdateInfoList) {
                if (storedInfo.mFwCSumValue.equals(currentInfo.mFwCSumValue)) {
                    newUpdateFound = false;
                }
            }
            if (newUpdateFound) {
                if (!mToDoList.contains(currentInfo)) {
                    dbgTrace("Adding new firmware download to todo list");
                    mToDoList.add(currentInfo);
                } else {
                    dbgTrace("New firmware item is aleady present in todo list");
                }

                result = true;
            }
        }
        return result;
    }

    private boolean populateApplicableUpdatesList() {
        dbgTrace();

        FwUpdateDescision updateDecision = FwUpdateDescision.ERROR_LOCAL_AVAILABILITY;
        boolean result = false;

        int i = 0;

        if (mMeemFwStatus == null || mCurrentUpdateInfoList == null) {
            if (mApplicableUpdateInfoList != null) {
                mApplicableUpdateInfoList.clear();
            }

            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Current firmware version details:-\n");
        sb.append("Date: ").append(mMeemFwStatus.mMeemFwDate).append("\n");
        sb.append("Version: ").append(mMeemFwStatus.mMeemCurrFwVersion).append("\n");

        dbgTrace(sb.toString());

        VersionString thisToolVersion = new VersionString(mThisToolVersion);

        dbgTrace("Total number of updates in manifest: " + String.valueOf(mCurrentUpdateInfoList.size()));

        if (mApplicableUpdateInfoList != null) {
            mApplicableUpdateInfoList.clear();
        }

        for (i = 0; i < mCurrentUpdateInfoList.size(); i++) {
            UpdateInfo updtInfo = mCurrentUpdateInfoList.get(i);

            VersionString currVer = new VersionString(mMeemFwStatus.mMeemCurrFwVersion);
            VersionString updtVer = new VersionString(updtInfo.mFwNewVersion);
            VersionString reqdVer = new VersionString(updtInfo.mFwReqMeemVersion);
            VersionString toolVer = new VersionString(updtInfo.mFwToolVersion);

            dbgTrace("Update details: Current version: " + mMeemFwStatus.mMeemCurrFwVersion + " And required minimum version is: " + updtInfo.mFwReqMeemVersion + " to update to: " + updtInfo.mFwNewVersion);

            if (currVer.isEqual(updtVer) || currVer.isGreaterThan(updtVer)) {
                updateDecision = FwUpdateDescision.HAS_LATEST;
            } else if ((currVer.isEqual(reqdVer) || currVer.isGreaterThan(reqdVer)) && updtVer.isGreaterThan(currVer)) {
                if (toolVer.isEqual(thisToolVersion) || thisToolVersion.isGreaterThan(toolVer)) {
                    dbgTrace("One entry in the manifest found applicable. Checking for local availability");
                    // Arun: The real change is here: We need to see if this version is
                    // already there in our downloaded FW database.
                    String localFwFile = getLocalFwFilePath(updtVer);
                    if (localFwFile != null) {
                        updtInfo.mFwUpdateLocalFile = localFwFile;
                        mApplicableUpdateInfoList.add(updtInfo);
                        updateDecision = FwUpdateDescision.UPDATES_AVAILABLE;
                        result = true;
                    } else {
                        dbgTrace("Useless! the available update is yet to be downloaded / not present in phone now");
                        updateDecision = FwUpdateDescision.ERROR_LOCAL_AVAILABILITY;
                    }
                } else {
                    dbgTrace("A newer version of firmware update tool is required.");
                    updateDecision = FwUpdateDescision.ERROR_OBSOLETE_TOOL;
                }
            } else {
                dbgTrace("MEEM firmware is too old.");
                updateDecision = FwUpdateDescision.ERROR_OBSOLETE_FW;
            }
        }

        dbgTrace("Update decision is: " + updateDecision);
        return result;
    }

    private String getLocalFwFilePath(VersionString updtVer) {
        dbgTrace();

        String updtVerStr = updtVer.getVersionString();

        // check with database first
        ArrayList<UpdateInfo> locallyStoredVersionInfoList = mFwStoreDb.getList();

        for (UpdateInfo locallyStoredVersionInfo : locallyStoredVersionInfoList) {
            if (locallyStoredVersionInfo.mFwNewVersion.equals(updtVerStr)) {
                String localFwFilePath = locallyStoredVersionInfo.getmFwUpdateLocalFile();
                File localFwFile = new File(localFwFilePath);
                if (localFwFile.exists() && localFwFile.canRead()) {
                    dbgTrace("Version: " + updtVerStr + " : is available locally");
                    return localFwFilePath;
                } else {
                    dbgTrace("Version: " + updtVerStr + " : was downloaded, but apparantly deleted. Removing it from db");
                    mFwStoreDb.delete(locallyStoredVersionInfo.mFwCSumValue);
                }
            }
        }

        dbgTrace("Version: " + updtVerStr + " is not downloaded yet.");
        return null;
    }

    // NOTE: In the modified firmware update framework, there is no need for this
    // callback.
    public void onCableFirmwareUpdateSucceeded(UpdateInfo info) {
        dbgTrace();
    }

    public void cleanup() {
        dbgTrace();

        if (mManifestDownloader != null && !mManifestDownloader.isCancelled()) {
            mManifestDownloader.cancel(true);
        }

        if (mFwDownloader != null && !mFwDownloader.isCancelled()) {
            mFwDownloader.cancel(true);
        }

        try {
            mUiCtxt.getAppContext().unregisterReceiver(mNetworkStatusReceiver);
        } catch (Exception ex) {
            dbgTrace("Exception while unregistering broadcast receiver: " + ex.getMessage());
        }
    }

    public ArrayList<UpdateInfo> getUpdateList() {
        if (ProductSpecs.FW_UPDATE_USING_LOCAL_FOLDER) {
            dbgTrace("WARNING: Using local folder to list available firmware versions - to be only used for FW tesing");

            String localFolder = mAppData.getRootFolderPath() + File.separator + ProductSpecs.FW_UPDATE_APP_LOCAL_FOLDER;
            File localFwUpdateFolder = new File(localFolder);

            ArrayList<UpdateInfo> localUpdates = new ArrayList<UpdateInfo>();

            if (localFwUpdateFolder.exists()) {
                for (String localFwUpdateFileName : localFwUpdateFolder.list()) {
                    String localFwUpdateFilePath = localFolder + File.separator + localFwUpdateFileName;

                    dbgTrace("Local fw update file complete path: " + localFwUpdateFilePath);

                    File localFwUpdateFile = new File(localFwUpdateFilePath);
                    UpdateInfo updateInfo = new UpdateInfo();

                    // Fill in strings that is used by UI
                    updateInfo.mFwSize = String.valueOf(localFwUpdateFile.length());
                    updateInfo.mFwDate = "Today";
                    updateInfo.mFwDescText = "If you are seeing this and if you are not a MEEM developer, contact them!";
                    updateInfo.mFwNewVersion = localFwUpdateFileName; // nice idea! as we are not using update.mml...

                    // This is the only parameter actually needed by MMP
                    updateInfo.mFwUpdateLocalFile = localFwUpdateFile.getAbsolutePath();

                    localUpdates.add(updateInfo);
                }
            } else {
                dbgTrace("Local folder for FW updates: " + localFolder + ": does not exist!");
            }

            dbgTrace("Found " + localUpdates.size() + " local update files");
            return localUpdates;
        } else {
            populateApplicableUpdatesList();
            return mApplicableUpdateInfoList;
        }
    }

    // create the manifest url according to product spec.
    private String getFwUpdateManifestUrl() {
        dbgTrace();

        String manifestUrl = "";

        if (ProductSpecs.FW_UPDATE_USING_FTP) {
            if ((null != ProductSpecs.FW_UPDATE_SERVER_USERNAME) && (null != ProductSpecs.FW_UPDATE_SERVER_PASSWORD)) {
                // ftp://username:password@host/path
                manifestUrl += "ftp://" + ProductSpecs.FW_UPDATE_SERVER_USERNAME + ":" + ProductSpecs.FW_UPDATE_SERVER_PASSWORD + "@" + ProductSpecs.FW_UPDATE_SERVER_HOSTNAME + ProductSpecs.FW_UPDATE_MANIFEST_FILE_PATH;
            } else {
                manifestUrl += "ftp://" + ProductSpecs.FW_UPDATE_SERVER_HOSTNAME + ProductSpecs.FW_UPDATE_MANIFEST_FILE_PATH;
            }
        } else {
            manifestUrl += "http://" + ProductSpecs.FW_UPDATE_SERVER_HOSTNAME + ProductSpecs.FW_UPDATE_MANIFEST_FILE_PATH;
        }

        dbgTrace("Manifest URL: " + manifestUrl);
        return manifestUrl;
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat("FwUpdateManager", trace);
        GenUtils.logMessageToFile("FwUpdateManager.log", trace);
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    public enum FwUpdateDescision {
        HAS_LATEST, UPDATES_AVAILABLE, ERROR_OBSOLETE_TOOL, ERROR_OBSOLETE_FW, ERROR_LOCAL_AVAILABILITY
    }
}
