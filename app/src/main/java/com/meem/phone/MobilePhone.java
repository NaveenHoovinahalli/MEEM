package com.meem.phone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPUpid;
import com.meem.utils.GenUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import meem.org.apache.commons.lang3.StringUtils;

/**
 * Class representing the mobile phone on which this app is running.
 *
 * @author Arun T A
 */

@SuppressLint("DefaultLocale")
public class MobilePhone {
    private static final int UPID_LEN = 15;

    boolean mRegistred = false;
    boolean mBackedUp = false;
    private String mTime;
    private String mName;
    private String mOperator;
    private String mLanguage;
    private String mPlatform;
    private String mVersion;
    private String mBrand;
    private String mModelName;
    private String mModelNumber;
    private String mMacID;
    private String mBluetoothID;
    private String mSerialNumber;
    private String mMemSize;
    private MMPUpid mUpid;
    private Context mContext;
    private AppLocalData mAppData = AppLocalData.getInstance();
    private UiContext mUiCtxt = UiContext.getInstance();

    public MobilePhone(Context context) {
        mContext = context;

        String upid = getUniqueId();

        mUpid = new MMPUpid(upid);

        mTime = GenUtils.readableDateYYYYMMDDhhmmss();
        String name = Build.MANUFACTURER;
        // Capitalize the first letter. (TODO: I18N)
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        mName = name;
        mOperator = "Operator";
        mLanguage = Locale.getDefault().getDisplayLanguage();
        mPlatform = "Android";
        mVersion = Build.VERSION.RELEASE;
        mBrand = Build.BRAND;
        mModelName = Build.MODEL;
        mModelNumber = Build.DEVICE;

        mMacID = "00-00-00-00-00-00";
        // Arun: 17Feb2016: This is not needed. Also, Android 6 is coming up
        // with restrictions on permissions.
        /*
         * WifiManager wifiMgr = (WifiManager)
		 * mContext.getSystemService(Context.WIFI_SERVICE); if (null != wifiMgr)
		 * { WifiInfo wInfo = wifiMgr.getConnectionInfo(); if (null != wInfo) {
		 * mMacID = wInfo.getMacAddress(); } }
		 */

        mBluetoothID = "00-00-00-00-00-00";
        // Arun: 17Feb2016: This is not needed. Also, Android 6 is coming up
        // with restrictions on permissions.
        /*
         * BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter(); if
		 * (null != btAdapter) { mBluetoothID = btAdapter.getAddress(); }
		 */

        mSerialNumber = Build.SERIAL;

        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new MemoryInfo();
        if (null != activityManager) {
            activityManager.getMemoryInfo(memoryInfo);
        }
        mMemSize = String.valueOf(memoryInfo.availMem);
    }

    public String getOperator() {
        return mOperator;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getMemSize() {
        return mMemSize;
    }

    public String toString() {
        String infoStr = "Upid: " + mUpid.toString() +
                "\nManufacturer: " + Build.MANUFACTURER +
                "\nBrand: " + Build.BRAND +
                "\nModel name: " + Build.MODEL +
                "\nModel number: " + Build.DEVICE +
                "\nAndroid release: " + Build.VERSION.RELEASE +
                "\nOS info: " + System.getProperty("os.version") +
                "\nLanguage: " + mLanguage +
                "\nTime zone: " + TimeZone.getDefault();


        return infoStr;
    }

    public boolean isBackedUp() {
        return mBackedUp;
    }

    public void setBackedUp(boolean mBackedUp) {
        this.mBackedUp = mBackedUp;
    }

    public boolean isRegistred() {
        return mRegistred;
    }

    public void setRegistred(boolean status) {
        this.mRegistred = status;
    }

    public String getUpid() {
        return mUpid.toString();
    }

    public MMPUpid getMMPUpid() {
        return mUpid;
    }

    public boolean createPinf() {
        String pinfPath = mAppData.getPinfPath();

        try {
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(pinfPath, false));

            bufWriter.append("<?mml version=\"1.0\"?>");
            bufWriter.append("<phone>");

            bufWriter.append("<time>" + GenUtils.sanitizeXml(mTime) + "</time>");

            bufWriter.append("<inf>");

            bufWriter.append("<name>" + GenUtils.sanitizeXml(mName) + "</name>");
            bufWriter.append("<operator>" + GenUtils.sanitizeXml(mOperator) + "</operator>");
            bufWriter.append("<language>" + GenUtils.sanitizeXml(mLanguage) + "</language>");
            bufWriter.append("<platform>" + GenUtils.sanitizeXml(mPlatform) + "</platform>");
            bufWriter.append("<version>" + GenUtils.sanitizeXml(mVersion) + "</version>");
            bufWriter.append("<brand>" + GenUtils.sanitizeXml(mBrand) + "</brand>");
            bufWriter.append("<model-name>" + GenUtils.sanitizeXml(mModelName) + "</model-name>");
            bufWriter.append("<model-number>" + GenUtils.sanitizeXml(mModelNumber) + "</model-number>");
            bufWriter.append("<mac-id>" + GenUtils.sanitizeXml(mMacID) + "</mac-id>");
            bufWriter.append("<bt-id>" + GenUtils.sanitizeXml(mBluetoothID) + "</bt-id>");
            bufWriter.append("<serial>" + GenUtils.sanitizeXml(mSerialNumber) + "</serial>");

            bufWriter.append("<memory>");
            bufWriter.append("<size>" + mMemSize + "</size>");
            bufWriter.append("</memory>");

            bufWriter.append("</inf>");
            bufWriter.append("</phone>");

            bufWriter.newLine();

            bufWriter.close();
        } catch (Exception ex) {
            mUiCtxt.log(UiContext.EXCEPTION, "Pinf creation failed: " + ex.getMessage());
            return false;
        }

        File fpinf = new File(pinfPath);
        mUiCtxt.log(UiContext.DEBUG, "Pinf created: " + pinfPath + ": " + String.valueOf(fpinf.length()));

        return true;
    }

    public final String getName() {
        if (null != mName) {
            return mName;
        } else {
            return "";
        }
    }

    public final String getModel() {
        if (null != mModelName) {
            return mModelName;
        } else {
            return "";
        }
    }

    public final String getModelNumber() {
        if (null != mModelNumber) {
            return mModelNumber;
        } else {
            return "";
        }
    }

    public final String getBrand() {
        if (null != mBrand && !mBrand.isEmpty()) {
            if (!Character.isUpperCase(mBrand.charAt(0))) {
                mBrand = Character.toString(Character.toUpperCase(mBrand.charAt(0))) + mBrand.substring(1);
            }
            return mBrand;
        } else {
            return "";
        }
    }

    /**
     * Seeing this 2013 code in 2016 December... Well man, backup operation was supposed to be started by phone object!!!!
     *
     * @return
     */
    public boolean prepareForBackup() {
        // TODO: call smart data cats' prepare & generic data list prepare
        return true;
    }

    public boolean startBackup() {
        return true;
    }

    public String getUsernameDeprecated() {
        AccountManager manager = AccountManager.get(mUiCtxt.getAppContext());
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type
            // values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");
            if (parts.length > 0 && parts[0] != null) return parts[0];
            else return "User";
        } else {
            return "User";
        }
    }

    public List<String> getUserEmailIDs() {
        AccountManager manager = AccountManager.get(mUiCtxt.getAppContext());
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type
            // values.
            possibleEmails.add(account.name);
        }

        return possibleEmails;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mUiCtxt.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    /**
     * get 15 character long unique id of this phone.
     * first, try to get IMEI. if not, try to get Android ID, else generate a unique string.
     */
    private String getUniqueId() {
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String upid = telMgr.getDeviceId();

        if (upid == null) {
            mUiCtxt.log(UiContext.WARNING, "IMEI is null. Trying Android ID");
            String androidId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
            if (androidId == null || (androidId != null && "9774d56d682e549c".equals(androidId))) {
                mUiCtxt.log(UiContext.WARNING, "Invalid Android ID, using Build.MODEL MD5 hash");
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(Build.MODEL.getBytes());
                    upid = String.format("%032x", new BigInteger(1, md5.digest()));
                    upid = upid.substring(0, UPID_LEN);
                } catch (Exception e) {
                    mUiCtxt.log(UiContext.WARNING, "Could not do Build.MODEL MD5 hash, using last resort fixed number!");
                    upid = "123456789123456";
                }
            } else {
                mUiCtxt.log(UiContext.WARNING, "Android ID: " + androidId);
                // trim or pad it.
                if (androidId.length() > UPID_LEN) {
                    upid = androidId.substring(0, UPID_LEN);
                } else {
                    upid = StringUtils.leftPad(androidId, UPID_LEN, '0');
                }
            }
        } else {
            mUiCtxt.log(UiContext.INFO, "IMEI available");
        }

        mUiCtxt.log(UiContext.WARNING, "Chosen upid: " + upid);
        return upid;
    }
}
