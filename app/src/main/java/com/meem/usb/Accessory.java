package com.meem.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.ProductSpecs;
import com.meem.usb.AccessoryFragment.OnAccessoryConnectionCallback;
import com.meem.utils.GenUtils;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Accessory implements Closeable, AccessoryInterface {
    static final String TAG = "Accessory";
    static final String ACTION_USB_PERMISSION = "com.meem.usb.USB_PERMISSION";
    Handler mLocalHandler;
    ParcelFileDescriptor mParcelFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    OnAccessoryConnectionCallback callback;
    UsbAccessory accessory;
    UsbManager usbManager;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            dbgTrace("BroadcastReceiver: onReceive");

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                dbgTrace("BroadcastReceiver: onReceive: ACTION_USB_PERMISSION");
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        dbgTrace("BroadcastReceiver: user granted permission to open accessory");
                        if (accessory != null) {
                            dbgTrace("BroadcastReceiver: opening accessory");
                            onPermission(accessory);
                        } else {
                            dbgTrace("BroadcastReceiver: accessory to be opened is null!");
                        }
                    } else {
                        dbgTrace("BroadcastReceiver: user denied permission to open accessory!");
                    }
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                dbgTrace("BroadcastReceiver: onReceive: Disconnect (Ignored. MeemCore will take care of this)");
            }
        }
    };
    int mHwVersion = 1;
    boolean forceSlowSpeedMode;
    Context mContext;
    IntentFilter mIntentFilter;

    public Accessory(OnAccessoryConnectionCallback mcallback, UsbManager musbManager, UsbAccessory maccessory, Context context) {
        dbgTrace("Constructor");

        this.accessory = maccessory;
        this.callback = mcallback;
        this.usbManager = musbManager;
        this.mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbReceiver, filter);

        mHwVersion = ProductSpecs.HW_VERSION_1_TI;
    }

    private static void dbgTrace(String trace) {
        GenUtils.logCat(TAG, trace);
        GenUtils.logMessageToFile("Accessory.log", trace);
    }

    private static void dbgTrace() {
        GenUtils.logMethodToFile("Accessory.log");
    }

    private void onPermission(UsbAccessory acc) {
        this.accessory = acc;
        connectedAccessory();
    }

    public void connectedAccessory() {
        dbgTrace();

        mParcelFileDescriptor = usbManager.openAccessory(accessory);
        if (mParcelFileDescriptor == null) {
            dbgTrace("Accessory open failed");
            return;
        }
        FileDescriptor fd = mParcelFileDescriptor.getFileDescriptor();
        mInputStream = new FileInputStream(fd);
        mOutputStream = new FileOutputStream(fd);

        new LooperThread().start();

        callback.onAccessoryConnected(this);
    }

    public void appDestroy() {
        mContext.unregisterReceiver(mUsbReceiver);
    }

    public void write(byte... data) throws IOException {
        mOutputStream.write(data);
    }

    public boolean isConnected() {
        return mLocalHandler != null && mLocalHandler.getLooper().getThread().isAlive();
    }

    @Override
    public void close() throws IOException {
        dbgTrace();
        try {
            if (mParcelFileDescriptor != null) {
                mParcelFileDescriptor.close();
                dbgTrace("accessory innner close");
            }
            if (mLocalHandler != null) {
                mLocalHandler.getLooper().quit();
                mLocalHandler = null;
            }
        } catch (IOException e) {
            dbgTrace("Exception in close method: " + e.getMessage());
        } finally {
        }
    }

    @Override
    public void closeAccessory() {
        try {
            close();
        } catch(IOException e) {
            dbgTrace("Exception in close accessory method: " + e.getMessage());
        }
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * @return the inputStream
     */
    @Override
    public InputStream getInputStream() {
        return mInputStream;
    }

    /**
     * @return the outputStream
     */
    @Override
    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    @Override
    public boolean isSlowSpeedMode() {
        return forceSlowSpeedMode;
    }

    @Override
    public void setSlowSpeedMode(boolean slow) {
        forceSlowSpeedMode = slow;
    }

    @Override
    public int getHwVersion() {
        return mHwVersion;
    }

    @Override
    public void setHwVersion(int hwVersion) {
        mHwVersion = hwVersion;
    }

    class LooperThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            mLocalHandler = new Handler();
            Looper.loop();
        }
    }
}
