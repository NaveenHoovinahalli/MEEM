package com.meem.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.ProductSpecs;
import com.meem.mmp.messages.MMPConstants;
import com.meem.utils.GenUtils;

public class AccessoryFragment extends android.support.v4.app.Fragment {
    private static final String tag = "AccessoryFragment";

    Accessory mAccessory;
    OnAccessoryConnectionCallback mCallback;
    UsbManager mUsbManager;
    Activity activity;

    public AccessoryFragment() {
        dbgTrace();
    }

    private static void dbgTrace() {
        GenUtils.logMethodToFile("AccessoryFragment.log");
    }

    private static void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("AccessoryFragment.log", trace);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dbgTrace();
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        dbgTrace();
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dbgTrace();

    }

    public boolean usbConnection(Activity mActivity) {
        dbgTrace();
        boolean result = true;

        this.activity = mActivity;

        mCallback = ((OnAccessoryConnectionCallback) activity);

        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();

        if (accessoryList != null && accessoryList.length == 1) {
            UsbAccessory accessory = accessoryList[0];
            dbgTrace("Accesory found in list: " + accessory);

            if(!accessory.getManufacturer().toLowerCase().contains("meem")) {
                dbgTrace("Unknown accessory: manufacturer is not MEEM");
                return false;
            }

            mAccessory = new Accessory(mCallback, mUsbManager, accessory, activity.getApplicationContext());
            onMeemAccessoryConnection(accessory);

            if (mUsbManager.hasPermission(accessory)) {
                dbgTrace("We have permission");
                mAccessory.connectedAccessory();
            } else {
                dbgTrace("Requesting permission");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 0, new Intent(Accessory.ACTION_USB_PERMISSION), 0);
                mUsbManager.requestPermission(accessory, pendingIntent);
            }
        } else {
            if (accessoryList == null) {
                dbgTrace("Null accessory list!");
            } else {
                dbgTrace("Accessory list is assumed to have one entry. But it has " + accessoryList.length + " entries now!");
            }

            result = false;
        }

        return result;
    }

    /**
     * This function specifically checks to see if the MEEM FW has set the buffer size to be used for USB communication. This is very
     * important functionality for non-standard Android phones - e.g. One+2.
     * <p/>
     * Note: By default, the USB packet size for MMP is 2048 (MMPConstants.MMP_PKT_SIZE)
     *
     * @param accessory
     */
    private void onMeemAccessoryConnection(UsbAccessory accessory) {
        dbgTrace();

        if (accessory == null) {
            return;
        }

        String mf = accessory.getManufacturer().toLowerCase();
        if (!mf.contains("meem")) {
            dbgTrace("Accessory is not made by meem: " + mf);
            return;
        }

        String serial = accessory.getSerial();
        String version = accessory.getVersion();
        dbgTrace("Serial number: " + serial + ", version: " + version);

        if (ProductSpecs.USB_BUFFER_SIZE_UNDER_FW_CONTROL) {
            int i = serial.lastIndexOf('#');
            if (i >= 0) {
                try {
                    String pktSizeStr = serial.substring(i + 1);
                    dbgTrace("Packet size: " + pktSizeStr);
                    Long pktSize = Long.parseLong(pktSizeStr);
                    if (pktSize != null) {
                        MMPConstants.MMP_PKT_SIZE = pktSize.intValue();
                        dbgTrace("Under firmware control, setting USB packet size to: " + MMPConstants.MMP_PKT_SIZE);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    dbgTrace("Error: Unable to find packet size from serial number: " + serial);
                }
            }
        } else {
            dbgTrace("[New impl] FW does not control USB pkt size");

            // Arun: 08Dec2017: Unfortunately OnePlus5 and OnePlus5T models gives null as serial number.
            // So, if serial is null, we are going to check version which will be set to 1.5 for 16GB TI cables by firmware version
            // 1.1.265.0 (08Dec2017). For all other TI and Ineda cables, accessory version will be 1.0. See changes in accessory_filter.xml
            if(serial == null) {
                dbgTrace("Warning: Serial number is null. This is an Android bug. Checking accessory version: " + version);
                if(version != null) {
                    if(version.contains("1.5")) {
                        dbgTrace("Voila! TI cable!");
                        mAccessory.setHwVersion(ProductSpecs.HW_VERSION_1_TI);
                    } else {
                        dbgTrace("Voila! Ineda cable!");
                        mAccessory.setHwVersion(ProductSpecs.HW_VERSION_2_INEDA);
                    }
                } else {
                    dbgTrace("Warning: Serial number and version is null. Strange scenario. Assuming Ineda hardware");
                    mAccessory.setHwVersion(ProductSpecs.HW_VERSION_2_INEDA);
                }
            } else {
                int i = serial.lastIndexOf('#');
                if (i >= 0) {
                    try {
                        String pktSizeStr = serial.substring(i + 1);
                        dbgTrace("Packet size parsed from accessory serial: " + pktSizeStr);
                        Long pktSize = Long.parseLong(pktSizeStr);
                        if (pktSize != null) {
                            if (pktSize.intValue() >= 32000) {
                                dbgTrace("MEEM V2 detected using accessory serial: " + serial);
                                mAccessory.setHwVersion(ProductSpecs.HW_VERSION_2_INEDA);
                            }
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        dbgTrace("Error: Unable to find packet size from serial number: " + serial);
                    }
                }
            }

            // The following stuff - especially MMPConstants.MMP_PKT_SIZE is used only for TI cables.
            dbgTrace("Checking phone characteristics...");

            if (mCallback.isBlackListedSlowPhone()) {
                dbgTrace("Blacklisted slow speed phone.");
                MMPConstants.MMP_PKT_SIZE = MMPConstants.MMP_LEGACY_PKT_SIZE;
                mAccessory.setSlowSpeedMode(true);
            } else {
                dbgTrace("Full speed phone.");
                MMPConstants.MMP_PKT_SIZE = MMPConstants.MMP_FULLSPEED_PKT_SIZE;
                mAccessory.setSlowSpeedMode(false);
            }
        }

        dbgTrace("USB packet size set to (ignored for V2): " + MMPConstants.MMP_PKT_SIZE);
    }

    @Override
    public void onResume() {
        dbgTrace();
        super.onResume();
    }

    @Override
    public void onPause() {
        dbgTrace();
        super.onPause();
    }

    @Override
    public void onDetach() {
        dbgTrace();
        super.onDetach();
        if (mAccessory != null) {
            mAccessory.appDestroy();
        }
    }

    public interface OnAccessoryConnectionCallback {
        void onAccessoryConnected(AccessoryInterface accesory);
        boolean isBlackListedSlowPhone();
        void onAccessoryDisconnected(UsbAccessory accessory);
    }

    public interface OnAccessoryCallbackPicker {
        OnAccessoryConnectionCallback getOnAccessoryCallback();
    }
}
