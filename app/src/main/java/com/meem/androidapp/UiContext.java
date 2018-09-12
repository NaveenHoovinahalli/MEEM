package com.meem.androidapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.meem.events.MeemEvent;
import com.meem.ui.GuiSpec;

import java.lang.reflect.InvocationTargetException;

/**
 * This singleton class keeps the application context and an instance of UI thread's Handler object. It keeps the UI related information
 * like screen size and provides methods to convert the dimensions specified in the MEEM GUI spec. This class is to be used by the modules
 * for all UI related dimension calculations in the app implementation. This class is heavily used by back-end modules (that uses threads
 * exclusively) to interact with other modules in the app that are working in the UI thread context.
 *
 * @author arun
 */

public class UiContext {
    public static final int MIN_LOG_LEVEL = 1; // DEBUG onwards
    public static final int TOAST = 0;
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARNING = 3;
    public static final int ERROR = 4;
    public static final int EXCEPTION = 5;
    public static final int LOG = 1;
    public static final int EVENT = 2;
    public static final int STATUS = 3;

    private static final String TAG = "UiContext";
    private static UiContext mThis = new UiContext();
    DisplayMetrics mDisplayMetrics;
    Point mUsableScreenSizePix, mRealScreenSizePix;
    int mStatusBarHeight, mNavigationBarHeight, mActionBarHeight;
    private Handler mHandler;
    private Context mContext;

    private String mPhoneUpid;

    private UiContext() {
        // no!
    }

    public static UiContext getInstance() {
        return mThis;
    }

    /**
     * This function must be called to initialize this singleton class - ONLY ONCE, from onCreate, ONLY FROM MAIN ACTIVITY
     *
     * @param context The application context
     * @param handler The Ui thread handler
     */
    public void setContextAndHandler(Context context, Handler handler) {
        if (null != mContext) {
            Log.wtf(TAG, "Changing previously set context");
        }

        mContext = context;

        if (null != mHandler) {
            Log.wtf(TAG, "Changing previously set handler");
        }

        mHandler = handler;

        Resources mResources = mContext.getResources();
        mDisplayMetrics = mResources.getDisplayMetrics();

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        mUsableScreenSizePix = new Point();
        display.getSize(mUsableScreenSizePix);

        /* Get the status bar height. */
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }

        /*
        Calculate the navigation bar height (This is the most reliable way).
        Using "navigation_bar_height" identifier won't work with Samsung devices.
        They will return valid numbers even though they do not have soft navigation bar!
        */
        mRealScreenSizePix = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(mRealScreenSizePix);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                mRealScreenSizePix.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                mRealScreenSizePix.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }

        /* navigation bar on the right */
        if (mUsableScreenSizePix.x < mRealScreenSizePix.x) {
            mNavigationBarHeight = mUsableScreenSizePix.y;
        }

        /* navigation bar at the bottom */
        if (mUsableScreenSizePix.y < mRealScreenSizePix.y) {
            mNavigationBarHeight = mRealScreenSizePix.y - mUsableScreenSizePix.y;
        }

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, mDisplayMetrics);
        }

        Log.d(TAG, "Usable screen size: " + mUsableScreenSizePix + " | Status bar: " + mStatusBarHeight + " | Nav bar: " + mNavigationBarHeight + " | Action bar: " + mActionBarHeight);
    }

    public float getDensity() {
        return mDisplayMetrics.density;
    }

    public float getDispalyMetrics() {
        return mDisplayMetrics.widthPixels;
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    public int getNavigationBarHeight() {
        return mNavigationBarHeight;
    }

    /**
     * In every phone, usable area includes status bar height. Returns the usable screen height excluding the status bar. This is the value
     * assumed in meem specifications.
     * <p/>
     * <p/>
     * Note: We should never consider navigation bar height for any screen height calculations in Android (not only for MEEM app).
     *
     * @return
     */
    public int getScreenHeightPix() {
        return mUsableScreenSizePix.y - mStatusBarHeight - mActionBarHeight;
    }

    public int getScreenWidthPix() {
        return mUsableScreenSizePix.x;
    }

    /**
     * To convert dimensions as per GUI specification which is based on a device screen of 1080 x 1845 dimensions. This is a simple
     * calculation. Most important is to use the Math.ceil method on results.
     *
     * @param type    Specifies whether the spec value passed is width or height.
     * @param specVal The value of the GUI spec parameter as defined in GuiSpec class. This is entirely based upon the MEEM GUI
     *                specification version 28 from Stefanija Najdovska (stefi@meemsl.com) by 2nd week of Feb 2016.
     *
     * @return
     */
    public float specToPix(int type, float specVal) {

        switch (type) {
            case GuiSpec.TYPE_WIDTH:
                return (float) Math.ceil(mUsableScreenSizePix.x * (specVal / 1080f));
            case GuiSpec.TYPE_HEIGHT:
                /**
                 * Note: We should never consider navigation bar height for
                 * any screen height calculations in Android (not only for MEEM app).
                 */
                return (float) Math.ceil((mUsableScreenSizePix.y - mStatusBarHeight - mActionBarHeight) * (specVal / 1845f));
            case GuiSpec.TYPE_FONTSIZE:
                return (float) Math.ceil(mUsableScreenSizePix.x * (specVal / 1080f));
            default:
                throw new IllegalArgumentException("Invalid conversion type: " + type);
        }
    }

    public float getMappingYforX(float x) {
        return x * (mRealScreenSizePix.y / mRealScreenSizePix.x) * mDisplayMetrics.density;
    }

    public final Context getAppContext() {
        return mContext;
    }

    // Arun: 07Aug2017: to workaround a silly bug in studio (it insist on using the name getApplicationContext for certain scenarios (e.g. getSystemService)!
    public final Context getApplicationContext() {
        return mContext;
    }

    /**
     * Any code that is using this class shall not be using this method anymore. Discuss with Arun (arun@meemsl.com)
     */
    @Deprecated
    public float dipToPix(Context context, int dip) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, mDisplayMetrics);
    }

    /**
     * Any code that is using this class shall not be using this method anymore. Discuss with Arun (arun@meemsl.com)
     */
    @Deprecated
    public float pixToDip(Context context, int pix) {
        return pix / (mDisplayMetrics.densityDpi / 160f);
    }

    /**
     * To set keyboard special key height width according to device specification its necessary to use dptopx TODO: remove this.
     */
    public float dpToPx(float dp) {

        if (mDisplayMetrics.widthPixels == 480) {
            dp = dp * (320.0f / 360.0f);
        }

        if (mDisplayMetrics.widthPixels == 768) {
            dp = dp * (384.0f / 360.0f);
        }

        if (mDisplayMetrics.density == 3.5f) {
            dp = dp * (1.1428571429f);
        }
        float px = dp * mDisplayMetrics.density;
        return px;
    }

    /**
     * Function to be used to make the UI log some messages. This function shall be used only for logging non-business-logic information.
     * So, this function is kept developer friendly by not forcing the user to create new UiLog object and all. According to the type
     * argument, the UI is free to implement these message rendering in any possible way.
     *
     * @param type Can be TOAST, DEBUG, INFO... etc defined as public static members of this UiContext class.
     * @param str  The message to be logged.
     */
    public void log(int type, String str) {
        if (null == mHandler) {
            return;
        }

        // Log filtering.
        if (type < MIN_LOG_LEVEL) {
            return;
        }

        Message msg = mHandler.obtainMessage();
        msg.arg1 = LOG;
        msg.arg2 = type;
        msg.obj = str;

        mHandler.sendMessage(msg);
    }

    /**
     * Function to be used to make the UI show status messages. This function shall be used only for logging business-logic information. So,
     * this function is kept developer friendly by not forcing the user to create new UiLog object and all. According to the type argument,
     * the UI is free to implement these message rendering in any possible way.
     *
     * @param type   Can be STATUS_FLASH or STATUS_FIXED as defined as public static members of this UiContext class.
     * @param status The message to be shown as status.
     */
    public void status(int type, String status) {
        if (null == mHandler) {
            return;
        }
        Message msg = mHandler.obtainMessage();
        msg.arg1 = STATUS;
        msg.arg2 = type;
        msg.obj = status;

        mHandler.sendMessage(msg);
    }

    /**
     * The most important function that must be used by any component other than the main activity (UI thread) to post events to the UI so
     * that user interaction / further processing is made possible.
     *
     * @param event MeemEvent Object describing the event.
     */
    public void postEvent(MeemEvent event) {
        if (null == mHandler) {
            return;
        }

        Message msg = mHandler.obtainMessage();
        msg.arg1 = EVENT;
        msg.arg2 = 0;
        msg.obj = event;

        mHandler.sendMessage(msg);
    }

    // Arun: 28Jun2017: Added
    public String getPhoneUpid() {
        if (mPhoneUpid == null) return "deadbaeb";
        return mPhoneUpid;
    }

    public void setPhoneUpid(String phoneUpid) {
        mPhoneUpid = phoneUpid;
    }
}
