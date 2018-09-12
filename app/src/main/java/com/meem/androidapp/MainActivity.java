package com.meem.androidapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.hardware.usb.UsbAccessory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.meem.businesslogic.DummySession;
import com.meem.businesslogic.GenericDataThumbnailDatabase;
import com.meem.cablemodel.MeemVault;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.fwupdate.FwUpdateManager;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.phone.Calenders;
import com.meem.phone.Contacts;
import com.meem.phone.Messages;
import com.meem.phone.MobilePhone;
import com.meem.phone.SmartDataSpec;
import com.meem.ui.AboutFragment;
import com.meem.ui.DetailsFragment;
import com.meem.ui.GuiSpec;
import com.meem.ui.HelpFragment;
import com.meem.ui.HomeFragment;
import com.meem.ui.LegalFragment;
import com.meem.ui.MeemCableMode;
import com.meem.ui.OutOfMemorySetting;
import com.meem.ui.SettingsFragment;
import com.meem.ui.utils.BackupStatusBroadcastReceiver;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemMessageBox;
import com.meem.ui.utils.MeemTextView;
import com.meem.usb.AccessoryFragment;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.utils.VersionString;
import com.meem.v2.cablemodel.PhoneDbModel;
import com.meem.v2.cablemodel.VaultDbModel;
import com.meem.v2.core.MeemCoreV2;
import com.meem.v2.net.MNetClientHandler;
import com.meem.v2.net.MNetConstants;
import com.meem.v2.net.MeemNetClient;
import com.meem.v2.net.MeemNetService;
import com.meem.v2.phone.CalendersV2;
import com.meem.v2.phone.ContactsV2;
import com.meem.v2.phone.MessagesV2;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.PhoneInfo;
import com.meem.viewmodel.VaultInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLSocket;

import me.leolin.shortcutbadger.ShortcutBadger;

import static com.meem.androidapp.SessionManager.SessionType.RESTORE;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, AccessoryFragment.OnAccessoryConnectionCallback, CableDriver.CableDriverListener, CablePresenterHelper, SessionManager.SessionManagementHelper, MeemNetService.MeemNetServiceListener {
    public static final String TAG = "MainActivity";
    private static final String TAG_ACCESSORY_RETAIN_FRAGMENT = "accessory_retain_fragment";

    int mCurrentAppVersionCode = 0;
    boolean mIsFreshInstallOfApp = false;
    boolean mIsTheFirstRunOfThisAppVersion = false;

    public DrawerLayout drawer;
    public boolean isPopupNeeded;

    DebugTracer mDbg = new DebugTracer(TAG, "MainActivity.log");
    UiContext mUiCtxt = UiContext.getInstance();
    ProgressBar mBar;
    FrameLayout mainContainer;
    TextView mToolBarRightTV;
    TextView mToolbarTVSave;

    HomeFragment mHomeFragment;
    ActionBarDrawerToggle mDrawerToggle;
    boolean isMenu = false;
    boolean isBackPressed = false;
    boolean isAllMenuItemVisible = false;
    Toolbar toolbar;
    boolean isOptionMenuClickable = true;
    FragmentManager fragmentManager = getSupportFragmentManager();
    SettingsFragment mSettingsFragment;
    DetailsFragment mDetailsFragment;
    HelpFragment mHelpFragment;
    MeemCableMode mMeemCableMode;
    LegalFragment mLegalFragment;
    AboutFragment mAboutFragment;
    ProgressBar mPbarBackup;
    NavigationView navigationView;
    ImageView mBackButton;
    ToolBarItemClickListener mListener;
    int menuBackStactCount = 0; //to keep track of stackcount of menu
    boolean mShowOptionsMenu = false;
    boolean mShowRestore, mShowShare, mShowDelete;
    boolean isUIVisible;
    boolean mDisableBackPress = false;
    TextView mAppLayoutTitle;

    ApplistBackupListener mListenerApp;

    View mLineSep;
    MeemTextView mSettingTV, mDetailsTV, mSelectIcon, mAbort, mMeemcablemodeTV, mNWDisconnectTV,mBackupTV;

    RelativeLayout mHomeMenu, mDetailsMenu, mSettingMenu, mAboutMenu, mHelpMenu, mMeemCableModeSetting, mMeemNWDisconnect;
    TextView mCopyRightText;

    EventHandler mEventHandler;

    /**
     * <--------------- Arun -------------------
     */
    AppLocalData mAppData = AppLocalData.getInstance();
    MobilePhone mMobilePhone;
    PhoneInfo mPhoneInfo;

    AccessoryFragment mAccessoryFragment;
    boolean mAccFragConnectAccNow = false;

    CableDriver mCableDriver;
    CablePresenter mCablePresenter;

    SessionManager mSessionManager;
    DummySession mDummySession;

    FwUpdateManager mFwUpdateMgr;
    AlertDialog mFirstAgreementAlert, mForcedFwUpdateAlert, mGenericAlert;
    String mDefaultSmsApp;
    Runnable mRunnableAfterSmsMgmtPermission;
    ProgressDialog mProgressPopup;
    String mLastSessionPrepComment, mLastSessionXfrComment;

    // ------------------------------------------------------
    // ---------------- Arun: For V2 ------------------------
    // ------------------------------------------------------
    int mCableHardwareVersion = ProductSpecs.HW_VERSION_1_TI;
    HashMap<Byte, String> mPrettyCatStrings = new HashMap<>();

    int mExperementalHwBufSize = -1;

    // Arun: For Meem Network: Start
    private MeemNetService mNetService; // master (server) mode
    private MeemNetClient mNetClient; // client mode
    private boolean mIsBoundToMNetService;

    private ArrayList<MNetClientHandler> mConnectedClients = new ArrayList<>();
    // Arun: For Meem Network: End

    // Note: Use dummy mode to run app without cable (a dummy CableInfo will be used). See ProductSpec to enable this mode.
    boolean mDummyOkToSend = true;

    private HashMap<String, Typeface> mTypeFaces = new HashMap<>();
    private ArrayList<JsonToSqlAsync> smartDataAsyncTaskList = new ArrayList<>();

    boolean mDetailsFragmentRefreshPending = false;

    private Runnable sessionPrepCommentaryRunnable = new Runnable() {
        @Override
        public void run() {
            double estTimeSecs = 0;
            if (mSessionManager != null && mSessionManager.isSessionLive() && !mSessionManager.isAbortPending()) {
                String title = getString(R.string.confirming_data);
                String subTitle;

                if (ProductSpecs.HW_VERSION_2_INEDA == getCableVersion()) {
                    subTitle = mLastSessionPrepComment;
                } else {
                    subTitle = null;
                }

                setAppTitle(title, subTitle);

                mHandler.removeCallbacks(sessionPrepCommentaryRunnable);
                mHandler.postDelayed(sessionPrepCommentaryRunnable, 1000);
            }
        }
    };

    private Runnable sessionXfrCommentaryRunnable = new Runnable() {
        @Override
        public void run() {
            double estTimeSecs = 0;
            if (mSessionManager != null && mSessionManager.isSessionLive() && !mSessionManager.isAbortPending()) {
                estTimeSecs = mSessionManager.getEstimatedTime();
            } else {
                if (ProductSpecs.DUMMY_CABLE_MODE) {
                    estTimeSecs = mDummySession.getEstimatedTimeSecs();

                    // simulate a data transfer
                    if (null != mCableDriver && mCableDriver.isCableConnected()) {
                        if (mDummyOkToSend) {
                            mDummyOkToSend = false;
                            mCableDriver.sendAppQuit();
                        }
                    }

                    if (estTimeSecs < 0) {
                        notifySessionResult(true, "Timer detected session end");
                    }
                } else {
                    return;
                }
            }

            int hrs = 0;
            int minute = 0;
            int seconds = 0;

            String str_hrs = "";
            String str_minute = "";
            String str_seconds = "";

            if (estTimeSecs >= 3600) {
                hrs = (int) (estTimeSecs / 3600);
                minute = (int) ((estTimeSecs % 3600) / 60);
                seconds = (int) ((estTimeSecs % 3600) % 60);
            } else if (estTimeSecs >= 60) {
                minute = (int) ((estTimeSecs) / 60);
                seconds = (int) ((estTimeSecs) % 60);
            } else {
                seconds = (int) estTimeSecs;
            }

            // TODO: This is hackish! see the actual problem.
            if (minute != 0) {
                str_hrs = String.valueOf(hrs);
                str_minute = String.valueOf(minute);
                str_seconds = String.valueOf(seconds);

                String remainingTimeStr = str_hrs + getString(R.string.hr) + " " + str_minute + getString(R.string.min) + " " + getString(R.string.left);

                String title = getString(R.string.transferring_date);
                String subTitle;

                if (ProductSpecs.HW_VERSION_2_INEDA == getCableVersion()) {
                    if (mLastSessionXfrComment != null) {
                        subTitle = mLastSessionXfrComment + " (" + remainingTimeStr + ")";
                    } else {
                        subTitle = remainingTimeStr;
                    }
                } else {
                    subTitle = remainingTimeStr;
                }

                setAppTitle(title, subTitle);
            }

            mHandler.removeCallbacks(sessionXfrCommentaryRunnable);
            mHandler.postDelayed(sessionXfrCommentaryRunnable, 1000);
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case UiContext.LOG:
                    if (msg.obj != null) {
                        mDbg.trace((String) msg.obj);
                    }
                    break;
                case UiContext.EVENT:
                    mEventHandler.handleEvent((MeemEvent) msg.obj);
                    break;
                default:
                    mDbg.trace("Unknown message type to handle");
                    break;
            }
            // in any case, call super.
            super.handleMessage(msg);
        }
    };

    public Typeface getTypeface(String fontFamily) {
        return mTypeFaces.get(fontFamily);
    }

    public FwUpdateManager getFwUpdateMgr() {
        return mFwUpdateMgr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDbg.trace();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (intent != null) {
            mExperementalHwBufSize = intent.getIntExtra("CoreBufSize", -1);
            if (mExperementalHwBufSize != -1) {
                mDbg.trace("### Experimental HW Buf size: " + mExperementalHwBufSize);
            }
        }

        // most important initializations here.
        init();

        // setup the accessory fragment (which is a retained fragment).
        // all these are to deal with Android quirks related to accessory + activity interactions.
        FragmentManager fm = getSupportFragmentManager();
        mAccessoryFragment = (AccessoryFragment) fm.findFragmentByTag(TAG_ACCESSORY_RETAIN_FRAGMENT);
        if (mAccessoryFragment == null) {
            mDbg.trace("Creating accessory fragment");
            mAccessoryFragment = new AccessoryFragment();
            fm.beginTransaction().add(mAccessoryFragment, TAG_ACCESSORY_RETAIN_FRAGMENT).commit();
            mAccFragConnectAccNow = true;
        } else {
            mDbg.trace("Accessory fragment exists");
            mAccFragConnectAccNow = true;
        }

        gatherVersionDetails();

        if (mIsFreshInstallOfApp || !didUserAgreementComplete()) {
            showFirstAgreement();
        }

        if (mIsTheFirstRunOfThisAppVersion) {
            showWhatsNew();
        }
        // We will proceed from onResume.
    }

    private void showFirstAgreement() {
        mDbg.trace();

        String agreement = getString(R.string.congratulation) + " " + getString(R.string.on_buying_your_meem) + "\n\n" + getString(R.string.welcome_note);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(agreement).setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // update the shared preferences with the current version code
                SharedPreferences prefs = getSharedPreferences(AppPreferences.GENERAL_USAGE, MODE_PRIVATE);
                prefs.edit().putInt(AppPreferences.KEY_FIRST_AGREEMENT_DONE, 1).apply();

                startCommWithAccessory();
            }
        });

        mFirstAgreementAlert = builder.create();
        mFirstAgreementAlert.show();
    }

    // As described here: http://crashlytics.com/blog/3-key-ways-to-increase-android-support-library-stability
    // "use onResumeFragment(), which is the recommended approach to interact with fragments in their proper state."
    @Override
    public void onResume() {
        mDbg.trace();

        super.onResume();
        isUIVisible = true;

        if (isPopupNeeded) updateUIOnUIVisible();

        if (didUserAgreementComplete()) {
            // Most important to do this.
            startCommWithAccessory();
        }

        setMenuCopyWriteTV();
    }

    private void setMenuCopyWriteTV() {
        mCopyRightText.setText("© MEEM Memory Ltd. \n  2017 , Ver:" + mCurrentAppVersionCode);
    }

    private void startCommWithAccessory() {
        mDbg.trace();

        if (mAccFragConnectAccNow) {
            mAccFragConnectAccNow = false;
            FragmentManager fm = getSupportFragmentManager();
            mAccessoryFragment = (AccessoryFragment) fm.findFragmentByTag(TAG_ACCESSORY_RETAIN_FRAGMENT);
            if (mAccessoryFragment == null) {
                mDbg.trace("Null accessory fragment. Creating it and starting usb connection");
                mAccessoryFragment = new AccessoryFragment();
                fm.beginTransaction().add(mAccessoryFragment, TAG_ACCESSORY_RETAIN_FRAGMENT).commit();
            } else {
                mDbg.trace("Accessory fragment exists. Starting usb connection");
            }

            if (!mAccessoryFragment.usbConnection(this)) {
                mDbg.trace("Local accessory connection failed. Trying network.");
                startMeemNetClient();
            }

        } else {
            mDbg.trace("No need to initiate usb connection now (usual onResume lifecycle callback)");
        }
    }

    private void startMeemNetClient() {
        mDbg.trace();

        if (!isNetworkFeatureEnabled()) {
            mDbg.trace("Network feature is not enabled. Doing nothing in network.");
            return;
        }

        if (mNetClient == null) {
            mNetClient = new MeemNetClient(mMobilePhone.getUpid());
        }

        mDbg.trace("Network feature is enabled. Starting search for master...");
        mHomeFragment.animateNetworkSearch(true);
        mNetClient.startSearchForMaster(new Runnable() {
            @Override
            public void run() {
                mDbg.trace("Stopping wifi animation");
                if (null != mHomeFragment) mHomeFragment.animateNetworkSearch(false);
            }
        });
    }

    // this will start search even if network feature is disabled.
    public void onMeemNetworkServerSearchRequest() {
        mDbg.trace();

        if (mNetClient == null) {
            mNetClient = new MeemNetClient(mMobilePhone.getUpid());
        }

        mDbg.trace("Starting search for master...");
        mHomeFragment.animateNetworkSearch(true);
        mNetClient.startSearchForMaster(new Runnable() {
            @Override
            public void run() {
                mDbg.trace("Stopping wifi animation");
                if (null != mHomeFragment) mHomeFragment.animateNetworkSearch(false);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isUIVisible = false;
        isPopupNeeded = false;
    }

    /**
     * this is most important for accessory management. when app is already running, since it is single instance activity, android will give
     * the accessory connected notification as a new intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        mDbg.trace();
        super.onNewIntent(intent);

        //Naveen:Incase app killed from OS when drawer was LOCKED
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        String action = intent.getAction();

        if (action == null) {
            mDbg.trace("Hmm... new intet action: null (may be new fw update notification)");
            return;
        }

        mDbg.trace("On new intent: " + action);

        // this is most important for accessory management. when app is already running, since it is single instance activity,
        // android will give the accessory connected notification as a new intent.
        if (action.equals("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
            mAccFragConnectAccNow = true;
            // accessory will be taken care of in onResume.
            return;
        }

        if ((action.equalsIgnoreCase(Intent.ACTION_SEND)) || (action.equalsIgnoreCase(Intent.ACTION_SENDTO)) ||
                (action.equalsIgnoreCase(Intent.CATEGORY_DEFAULT)) || (action.equalsIgnoreCase(Intent.CATEGORY_BROWSABLE))) {
            if (mSessionManager != null && mSessionManager.isSessionLive()) {
                mDbg.trace("Session in progress, wait till it completes");
                MeemMessageBox mbox = new MeemMessageBox(this);
                // TODO: Translation
                mbox.setMessage(/*getResources().getString(R.string.restore_sms)*/"MEEM is processing your messages. Please wait.");
                mbox.show();
            } else {
                mDbg.trace("Please check your system configuration to select the default messaging app");
                MeemMessageBox mbox = new MeemMessageBox(this);
                // TODO: Translation
                mbox.setMessage(/*getResources().getString(R.string.interrupted_sms)*/"Please check your system configuration to select the default messaging app");
                mbox.show();
            }
        }
    }

    private void appKilledFromSystemResetUI() {


        if (fragmentManager == null) {
            return;
        }

        for (int i = 0; i <= fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }

        setBackPressDisable(false);
        setStopButtonProperties(false, false, false);


    }

    /**
     * called from onCreate.
     */
    private void init() {
        fetchViewId();
        setClickListner();
        setDefaultMenu();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setNavigation(toolbar);
        setTextFontType();
        Context appCtxt = getApplicationContext();
        SetBadgeAlarm();

        appKilledFromSystemResetUI();//To handle the fragment issues

        SharedPreferences prefs = getSharedPreferences("badge_number", MODE_PRIVATE);
        int badgeCount = prefs.getInt("count", 0);

        // The following calls are very important. Do not change order.
        mUiCtxt.setContextAndHandler(appCtxt, mHandler);
        mAppData.setContext(appCtxt);
        mEventHandler = new EventHandler(MainActivity.this, appCtxt);

        HouseKeeping houseKeeper = new HouseKeeping(appCtxt);
        houseKeeper.cleanupLeftovers(); // Take special note of this.

        mMobilePhone = new MobilePhone(appCtxt);
        mDbg.trace("Phone information: \n" + mMobilePhone.toString());
        mUiCtxt.setPhoneUpid(mMobilePhone.getUpid());

        mPhoneInfo = new PhoneInfo();
        mPhoneInfo.setmUpid(mMobilePhone.getUpid());
        mPhoneInfo.setmBaseModel(getPhoneName()); // Beautified brand name.

        mFwUpdateMgr = new FwUpdateManager();
        mFwUpdateMgr.onCreate();

        GenericDataThumbnailDatabase thumbDb = new GenericDataThumbnailDatabase(mMobilePhone.getUpid());
        int thumbDbVersion = thumbDb.getThumbnailDbVersion();
        mDbg.trace("Thumbnail datatase version: " + thumbDbVersion);

        mTypeFaces.put("GMC-1", Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/GMC-1.ttf"));
        mTypeFaces.put("GMC-2", Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/GMC-2.ttf"));
        mTypeFaces.put("DM", Typeface.createFromAsset(mUiCtxt.getAppContext().getAssets(), "fonts/DM.ttf"));

        // TODO: Register a battery status receiver.

        mHomeFragment = new HomeFragment();
        setHomeFragment(isMenu);

        // keep a preallocated hashmap of category names which are frequently accessed during session updates
        byte[] smartCats = MMLCategory.getAllSmartCatCodes();
        byte[] genCats = MMLCategory.getAllGenCatCodes();

        for (byte cat : smartCats) {
            mPrettyCatStrings.put(cat, MMLCategory.toSmartCatPrettyString(cat));
        }

        for (byte cat : genCats) {
            mPrettyCatStrings.put(cat, MMLCategory.toGenericCatPrettyString(cat));
        }
    }

    public void SetBadgeAlarm() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(AppPreferences.BACKUP_NOTIFICATION_STATUS, true)) {
            SharedPreferences prefs = getSharedPreferences("badge_number", MODE_PRIVATE);
            int badgeCount = prefs.getInt("count", 0);
            if (badgeCount > 0) {
                ShortcutBadger.applyCount(this, badgeCount);
                return;
            }
        }

        Intent intent = new Intent(this, BackupStatusBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + BackupStatusBroadcastReceiver.ONE_DAY, pendingIntent);
    }


    public void removeBadge() {
        ShortcutBadger.removeCount(this);
    }

    public void resetAlaramForBadge() {
        SharedPreferences.Editor editor = getSharedPreferences("badge_number", MODE_PRIVATE).edit();
        editor.putInt("count", 0);
        editor.commit();

        SetBadgeAlarm();
    }

    private void setTextFontType() {
        setTextFont(mAppLayoutTitle);
    }

    public CablePresenter getCablePresenter() {
        return mCablePresenter;
    }

    private void setDefaultMenu() {
        mDetailsMenu.setClickable(false);
        mSettingMenu.setClickable(false);
        mMeemCableModeSetting.setClickable(false);
        mMeemNWDisconnect.setClickable(false);
    }

    private void setClickListner() {
        mHomeMenu.setOnClickListener(this);
        mDetailsMenu.setOnClickListener(this);
        mSettingMenu.setOnClickListener(this);
        mAboutMenu.setOnClickListener(this);
        mHelpMenu.setOnClickListener(this);
        mMeemCableModeSetting.setOnClickListener(this);
        mMeemNWDisconnect.setOnClickListener(this);
        mToolBarRightTV.setOnClickListener(this);
        mSelectIcon.setOnClickListener(this);
        mToolbarTVSave.setOnClickListener(this);
        mAbort.setOnClickListener(this);
        mBackupTV.setOnClickListener(this);


    }

    public void setEventListner(ToolBarItemClickListener listener) {
        mListener = listener;
    }

    private void showAllMenuItems(boolean b) {
        if (b) {

            if (getCableVersion() == 2) {
                mMeemCableModeSetting.setVisibility(View.VISIBLE);
                if (isMeemConnectedOverNetwork()) {
                    mLineSep.setVisibility(View.VISIBLE);
                    mMeemNWDisconnect.setVisibility(View.VISIBLE);
                } else {
                    mLineSep.setVisibility(View.GONE);
                    mMeemNWDisconnect.setVisibility(View.GONE);
                }
            }
            isAllMenuItemVisible = true;
            mDetailsMenu.setClickable(true);
            mSettingMenu.setClickable(true);
            mMeemCableModeSetting.setClickable(true);
            mMeemNWDisconnect.setClickable(true);
            mDetailsTV.setTextColor(getResources().getColor(R.color.meemWhite));
            mSettingTV.setTextColor(getResources().getColor(R.color.meemWhite));
            mMeemcablemodeTV.setTextColor(getResources().getColor(R.color.meemWhite));
            mNWDisconnectTV.setTextColor(getResources().getColor(R.color.meemWhite));

        } else {
            isAllMenuItemVisible = false;
            mDetailsMenu.setClickable(false);
            mSettingMenu.setClickable(false);
            mMeemCableModeSetting.setClickable(false);
            mMeemNWDisconnect.setClickable(false);
            mDetailsTV.setTextColor(getResources().getColor(R.color.meemBlack50));
            mSettingTV.setTextColor(getResources().getColor(R.color.meemBlack50));
            mMeemcablemodeTV.setTextColor(getResources().getColor(R.color.meemBlack50));
            mNWDisconnectTV.setTextColor(getResources().getColor(R.color.meemBlack50));


        }
    }

    private void setHomeFragment(boolean isMenu) {
//        menuBackStactCount++;
        menuBackStactCount = 1;//changed for testing
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        fragmentTransaction.replace(R.id.mainContainer, mHomeFragment, "home");
        fragmentTransaction.addToBackStack("homeFragment");
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {

        if (!isUIVisible)
            return;

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        } else if (mDisableBackPress) {
            if (mListener != null) mListener.onBackButtonPressed();
            return;
        } else if (fragmentManager.getBackStackEntryCount() == 1) {
            moveTaskToBack(true);
            return;

        } else if (fragmentManager.getBackStackEntryCount() == menuBackStactCount) {
            menuBackStactCount = 1;
            isMenu = false;
            showMenuHideBackIcon();
            setTextViewVisibility(false);
            fragmentManager.popBackStack("homeFragment", 0);
            return;
        } else {
            if (fragmentManager.getBackStackEntryCount() == menuBackStactCount + 1) {
                showMenuHideBackIcon();
            }
            super.onBackPressed();
        }
    }

    private void fetchViewId() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mBar = (ProgressBar) findViewById(R.id.progressBar);
        mPbarBackup = (ProgressBar) findViewById(R.id.pBarPause);
        mainContainer = (FrameLayout) findViewById(R.id.mainContainer);
        mToolBarRightTV = (TextView) findViewById(R.id.toolbarRightTV);
        mToolbarTVSave = (TextView) findViewById(R.id.toolbarTVSave);
        mBackButton = (ImageView) findViewById(R.id.backButtonIV);
        mAppLayoutTitle = (TextView) findViewById(R.id.layoutTitleTV);
        mAbort = (MeemTextView) findViewById(R.id.abort);
        mSelectIcon = (MeemTextView) findViewById(R.id.toolbarSelectTV);
        mBackupTV = (MeemTextView) findViewById(R.id.backup);

        mHomeMenu = (RelativeLayout) findViewById(R.id.home);
        mDetailsMenu = (RelativeLayout) findViewById(R.id.details);
        mSettingMenu = (RelativeLayout) findViewById(R.id.settings);
        mAboutMenu = (RelativeLayout) findViewById(R.id.about);
        mHelpMenu = (RelativeLayout) findViewById(R.id.help);
        mMeemCableModeSetting = (RelativeLayout) findViewById(R.id.meemcablemode);
        mMeemNWDisconnect = (RelativeLayout) findViewById(R.id.nwDisconnect);

        mLineSep = findViewById(R.id.viewId);


        mSettingTV = (MeemTextView) findViewById(R.id.settingText);
        mDetailsTV = (MeemTextView) findViewById(R.id.detailsText);
        mMeemcablemodeTV = (MeemTextView) findViewById(R.id.meemcablemodeTV);
        mNWDisconnectTV = (MeemTextView) findViewById(R.id.nwDisconnectTV);

        mCopyRightText = (TextView) findViewById(R.id.copywrit);
    }

    private void setNavigation(Toolbar toolbar) {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) super.onDrawerStateChanged(newState);
            }
        };

        drawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return true;
    }

    public void OnToolBarRightTVClick(View view) {

        if (mToolBarRightTV.getText().equals(getResources().getString(R.string.edit))) {
            mListener.onEditOrCancelClickListner(getResources().getString(R.string.edit));
            mToolBarRightTV.setText(getResources().getString(R.string.cancel));
            mToolbarTVSave.setVisibility(View.VISIBLE);
        } else {
//            if(!isOutOfMemoryFragment)
            mToolBarRightTV.setText(getResources().getString(R.string.edit));
            mListener.onEditOrCancelClickListner(getResources().getString(R.string.cancel));
            mToolbarTVSave.setVisibility(View.GONE);
        }
    }

    public void OnToolBarSaveTVClicked(View view) {
        setToolbarText(getResources().getString(R.string.edit));
        mToolbarTVSave.setVisibility(View.GONE);
        mListener.onSaveClickListner();


    }

    public void OnToolBarShareIconClick(View view) {
        mListener.onShareIconClickListener();
    }

    public void OnToolBarRestoreIconClick(View view) {
        mListener.onRestoreIconListener();
    }

    public void OnToolBarSelectTVClick(View view) {
        mListener.onSelectIconClickListener();
    }

    public void enableToolbarClick(boolean b) {
        mToolBarRightTV.setClickable(b);
        mSelectIcon.setClickable(b);
        mToolbarTVSave.setClickable(b);
    }

    public void setOptionMenuClickable(boolean b) {
        isOptionMenuClickable = b;
    }

    public void showShareandRestoreIconsHideSelect() {
        mSelectIcon.setVisibility(View.GONE);

    }

    public void showSelectIconsHideShareRestore() {
        mSelectIcon.setVisibility(View.VISIBLE);

    }

    public void hideToolbarIcons() {
        mSelectIcon.setVisibility(View.GONE);
    }

    public void setSelectandBackIconeClickable(boolean isClickable) {
        if (isClickable) {
            mSelectIcon.setClickable(true);
            mBackButton.setClickable(true);

        } else {
            mSelectIcon.setClickable(false);
            mBackButton.setClickable(false);
        }
    }

    public void setOptionMenuContent(boolean showOptionsMenu, boolean showShare, boolean showRestore, boolean showDelete) {

        if (isPopupNeeded) showOptionsMenu = false;
        mShowOptionsMenu = showOptionsMenu;
        mShowShare = showShare;
        mShowRestore = showRestore;
//        for hiding delete
        mShowDelete = false;
        invalidateOptionsMenu();
    }

    public void showMenuFragment(Fragment fragment, String tag) {

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        fragmentTransaction.replace(R.id.mainContainer, fragment);
        fragmentTransaction.addToBackStack(tag);
        fragmentTransaction.commitAllowingStateLoss();

    }

    public void showFragment(Fragment fragment, boolean isBackStack, boolean isMenu) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (isBackPressed) {
            isBackPressed = false;//this condition will never come as we are sending the homefragment to backstack,
            fragmentTransaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        } else {
            fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        }

        fragmentTransaction.replace(R.id.mainContainer, fragment);
        if (isBackStack) {
            fragmentTransaction.addToBackStack(null);
            hideMenuShowBackIcon();
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void hideSoftKey() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void onClickBackButton(View view) {
        hideSoftKey();
        onBackPressed();
    }

    public void setTextViewVisibility(boolean b) {
        if (b) {
            mToolBarRightTV.setVisibility(View.VISIBLE);
            if (mToolBarRightTV.getText().equals(getResources().getString(R.string.cancel))) {
                mToolbarTVSave.setVisibility(View.VISIBLE);
            }
        } else {
            mToolBarRightTV.setVisibility(View.GONE);
            mToolbarTVSave.setVisibility(View.GONE);
        }
    }

    public void setToolbarText(String text) {
        if (text.equals(getResources().getString(R.string.cancel)))
            mToolbarTVSave.setVisibility(View.VISIBLE);
        else mToolbarTVSave.setVisibility(View.GONE);

        mToolBarRightTV.setText(text);
    }

    private void hideMenuShowBackIcon() {
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mBackButton.setVisibility(View.VISIBLE);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void hideMenuIcon() {
        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    public void showMenuIcon() {
        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    private void showMenuHideBackIcon() {
        mBackButton.setVisibility(View.GONE);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public void hideSaveandCancelButton() {
        mToolBarRightTV.setVisibility(View.GONE);
        mToolbarTVSave.setVisibility(View.GONE);
    }

    public void setBackPressDisable(boolean b) {
        mDisableBackPress = b;
    }

    public void setAppTitle(String string) {
        mAppLayoutTitle.setText(string);
    }

    public void setAppTitle(String title, String subTitle) {
        // To handle legacy "Confirming data", "Transferring data" etc. of V1.
        if (null == subTitle) {
            mAppLayoutTitle.setText(title);
            return;
        }

        if (subTitle.isEmpty() || subTitle.equals("null")) {
            mAppLayoutTitle.setText(title);
            return;
        }

        String finalString = title + "\n" + subTitle;
        Spannable sb = new SpannableString(finalString);

        int mSize = getResources().getDimensionPixelSize(R.dimen.subtitle);

        // TODO: Text size 20 pixels works very well for XXHDPI. Need to add dimens to ahndle other resolutions.
        sb.setSpan(new AbsoluteSizeSpan(mSize), finalString.indexOf(subTitle), finalString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        mAppLayoutTitle.setText(sb);
    }

    public void showShareAndRestoreIcon(boolean b) {
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.toolbarRightTV) {
            OnToolBarRightTVClick(v);
            return;
        }
        if (id == R.id.toolbarSelectTV) {
            OnToolBarSelectTVClick(v);
            return;
        }
        if (id == R.id.toolbarTVSave) {
            OnToolBarSaveTVClicked(v);
            return;

        }
        if (id == R.id.abort) {
            setStopButtonProperties(false, true, false);
            return;
        }


        setTextViewVisibility(false);
        menuBackStactCount++;

        if (id == R.id.home) {
            callBackHomeFragment();

        } else if (id == R.id.details) {
            if (mDetailsFragment == null) {
                mDetailsFragment = new DetailsFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("FROMSETTING", false);
                mDetailsFragment.setArguments(bundle);
            }
            showMenuFragment(mDetailsFragment, "detailsFragment");

        } else if (id == R.id.settings) {
            if (mSettingsFragment == null) mSettingsFragment = new SettingsFragment();
            showMenuFragment(mSettingsFragment, "settingFragment");

        } else if (id == R.id.legal) {
            if (mLegalFragment == null) mLegalFragment = new LegalFragment();
            showMenuFragment(mLegalFragment, "legalFragment");

        } else if (id == R.id.about) {
            if (mAboutFragment == null) mAboutFragment = new AboutFragment();
            showMenuFragment(mAboutFragment, "aboutFragment");

        } else if (id == R.id.help) {
            if (mHelpFragment == null) mHelpFragment = new HelpFragment();
            showMenuFragment(mHelpFragment, "helpFragment");
        } else if (id == R.id.meemcablemode) {
            mMeemCableMode = new MeemCableMode();
            showMenuFragment(mMeemCableMode, "meemcablemode");

        } else if (id == R.id.nwDisconnect) {
            onDisconnectFromNWClick();
        }

        drawer.closeDrawer(GravityCompat.START);
    }

    private void onDisconnectFromNWClick() {
        mDbg.trace();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Disconnect from MEEM network?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();

                stopMeemNetClient();
                onAccessoryDisconnected(null);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        mGenericAlert = builder.create();
        mGenericAlert.show();
    }

    private void callBackHomeFragment() {
        menuBackStactCount = 1;
        fragmentManager.popBackStack("homeFragment", 0);
    }

    public void setTextFont(TextView textView) {
        Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/GMC-1.ttf");
        textView.setTypeface(tf);
    }

    private void setStopButtonProperties(boolean visibility, boolean allowClick, boolean withProgress) {
        mDbg.trace();

        if (visibility) {
            mAbort.setVisibility(View.VISIBLE);
            if (withProgress) mPbarBackup.setVisibility(View.VISIBLE);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        } else {
            if (mAbort.getVisibility() == View.VISIBLE) {
                if (allowClick) {
                    if (null != mSessionManager && mSessionManager.isSessionLive()) {
                        mDbg.trace("User requests to abort the session");
                        mHomeFragment.onUserAbortRequest();
                        return; // TODO: Naveen: Convoluted logic. This function needs cleanup.
                    } else if (ProductSpecs.DUMMY_CABLE_MODE && null != mDummySession && mDummySession.isSessionLive()) {
                        mDummySession.stop();
                        notifySessionResult(false, "Aborted");
                    } else {
                        mDbg.trace("User requests to stop auto-backup");
                        mHomeFragment.removeAutoBackupCountDown();
                    }
                }
                mAbort.setVisibility(View.GONE);
                mPbarBackup.setVisibility(View.GONE);
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mShowOptionsMenu) {
            getMenuInflater().inflate(R.menu.main, menu);
            MenuItem shareItem = menu.findItem(R.id.share);
            MenuItem restoreItem = menu.findItem(R.id.restore);
            MenuItem deleteItem = menu.findItem(R.id.delete);

            if (mShowShare) {
                shareItem.setVisible(true);
            } else shareItem.setVisible(false);

            if (mShowRestore) {
                restoreItem.setVisible(true);
            } else restoreItem.setVisible(false);

            if (mShowDelete) {
                deleteItem.setVisible(true);
            } else deleteItem.setVisible(false);

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isOptionMenuClickable) return true;

        int id = item.getItemId();

        if (id == R.id.share) {
            mListener.onShareIconClickListener();
            return true;
        } else if (id == R.id.restore) {
            mListener.onRestoreIconListener();
            return true;
        } else if (id == R.id.delete) {
            mListener.onDeleteIconClickListener();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateHomeFragmentOnCableUpdate() {
        mDbg.trace();

        if (mHomeFragment != null && mCablePresenter != null) {
            CableInfo cableInfo;
            if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
                cableInfo = mCablePresenter.getCableViewModel();
            } else {
                // Fresh model creation is costly for v2. Logically, we are updating the model in presenter everytime is is changed anyway. 
                cableInfo = mCablePresenter.getCableInfo();
            }

            mHomeFragment.update(mPhoneInfo, cableInfo);
        }
    }

    public  void  onBackupTVClicked(View view){

        mListenerApp.onBackupTVClickListner();
    }
    @Override
    public boolean isBlackListedSlowPhone() {
        mDbg.trace();

        // TODO: Blacklisting based on Models needs to be abstracted to another class.
        String phoneModel = mMobilePhone.getModel();
        if (phoneModel.equalsIgnoreCase("GT-I8190N")) { // S3 Mini
            // S3 mini.
            mDbg.trace("Blacklisted slow speed device detected:" + mMobilePhone.getModel());
            return true;
        }

        return false;
    }

    @Override
    public void onAccessoryConnected(final AccessoryInterface accesory) {
        mDbg.trace();

        // NOTE: To handle "Don't keep activities" in developer options which
        // causes the onAttach of fragments being called before onCreate of
        // activity.
        if (mUiCtxt == null) {
            mUiCtxt = UiContext.getInstance();
            mUiCtxt.setContextAndHandler(getApplicationContext(), mHandler);
        }

        if (mNetClient != null) {
            mDbg.trace("Cancelling search for master...");
            mNetClient.stopSearchForMaster();
        }

        mCableHardwareVersion = accesory.getHwVersion();
        mDbg.trace("Cable hardware version: " + mCableHardwareVersion);

        // ask cable driver to take care of the connected cable.
        /*if (null == mCableDriver) {*/ // Arun: 16June23: Removed this check from v1.0.50 onwards.
        if (ProductSpecs.HW_VERSION_1_TI == mCableHardwareVersion) {
            mCableDriver = new CableDriverV1(this);
            mCablePresenter = new CablePresenterV1(mMobilePhone.getUpid(), mCableDriver, this);
            mSessionManager = new SessionManagerV1(mMobilePhone.getUpid(), this);
        } else if (ProductSpecs.HW_VERSION_2_INEDA == mCableHardwareVersion) {
            mCableDriver = new CableDriverV2(this);
            // This is only used for buffer size experements.
            if (mExperementalHwBufSize != -1) {
                showToast("Hardware buffer size set to: " + mExperementalHwBufSize);
                mCableDriver.setExperimentalHwBufferSize(mExperementalHwBufSize);
            }
            mCablePresenter = new CablePresenterV2(mMobilePhone.getUpid(), mCableDriver, this);
            mSessionManager = new SessionManagerV2(mMobilePhone.getUpid(), this);
        } else {
            String msg = "Unsupported cable hardware version: " + mCableHardwareVersion;
            mDbg.trace("!BUG! " + msg);
            showAlertMessage(msg, null);
            return;
        }
        /*}*/

        // Arun: 02June2017: if there was a firmware upgrade, a progress bar would have been running.
        hideProgressPopup();

        // Naveen: On connection of cable when the app is in help/about fragment
        if (fragmentManager.getBackStackEntryCount() > 1) {
            callBackHomeFragment();
            showMenuHideBackIcon();
            showAllMenuItems(false);
        }

        // Driver.onCableConnect will take quite some time. So, showing user early that cable is connected.
        mHomeFragment.showCableConnected();

        //Naveen: Disabling the slide MENU view
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        // Instantiate the presenter. Important Note: Presenter is not ready fully for UI
        // until we get response from Driver.onCableConnect()
        mHomeFragment.setEventListener(mCablePresenter);

        // The following can take several seconds (for V2, securedb alone can be 50MB if data is about 15GB)
        mHomeFragment.showCableInitProgressBar();

        mCableDriver.onCableConnect(accesory, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mDbg.trace("Cable driver connect finished with result: " + result);

                if (result) {
                    // From here, it goes to places.
                    updateFirmwareAsNeeded();
                } else {
                    onCriticalError("Cable initializing failed. See diagnostic section in help menu");
                }

                return result;
            }
        });
    }

    // Legacy TI hardware logic flow requires a split, namely earlyFirmwareHook for Ineda hw.
    private boolean updateFirmwareAsNeeded() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("WTF: Cable disconnected while app is launching. Must be a *tester*");
            return false;
        }

        String fwVersion = mCableDriver.getFwVersion();

        VersionString curVersion = new VersionString(fwVersion);
        VersionString minVersion = new VersionString(ProductSpecs.MIN_FW_VERSION_FOR_TI_HW);

        boolean updateNotNeeded = curVersion.isEqual(minVersion) || curVersion.isGreaterThan(minVersion);

        if (updateNotNeeded) {
            mDbg.trace("Firmware update is not needed: " + curVersion.getVersionString());
            refreshRequiredDatabasesAfterCableInit();
            return true;
        }

        mDbg.trace("Forcing firmware update for: " + curVersion.getVersionString());
        showFirmwareUpdateNotice();

        return false;
    }

    private void showFirmwareUpdateNotice() {
        mDbg.trace();

        mHomeFragment.hideCableInitProgressBar();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String fwMsg = getString(R.string.firmware_update_required) + "\n\n" + getString(R.string.firmware_warning);

        builder.setMessage(fwMsg).setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (!doBundledFirmwareUpdate()) {
                    showAlertMessage("Firmware upgrade failed. Please see diagnostics section in app's help menu.", null);
                }
            }
        });

        mForcedFwUpdateAlert = builder.create();
        mForcedFwUpdateAlert.show();
    }

    /**
     * This function is a one way ticket - either to automatic disconnect cable OR killing the app.
     *
     * @return
     */
    private boolean doBundledFirmwareUpdate() {
        mDbg.trace();

        AssetManager assManager = getApplicationContext().getAssets();
        InputStream is;

        String bundledFwVersion;

        if (ProductSpecs.HW_VERSION_1_TI == mCableHardwareVersion) {
            bundledFwVersion = ProductSpecs.BUNDLED_FW_VERSION_FOR_TI_HW;
        } else {
            bundledFwVersion = ProductSpecs.BUNDLED_FW_VERSION_FOR_INEDA_HW;
        }

        try {
            is = assManager.open("fw/update_" + bundledFwVersion + "_android.dat");
        } catch (IOException e) {
            mDbg.trace("Error: Unable to access bundled FW: " + e.getMessage());
            return false;
        }

        String fwUpdateFilePath;

        if (ProductSpecs.HW_VERSION_1_TI == mCableHardwareVersion) {
            fwUpdateFilePath = mAppData.getFwUpdateFilePath("bundled-" + bundledFwVersion);
        } else {
            fwUpdateFilePath = mAppData.getFwUpdateV2FilePath(bundledFwVersion);
        }

        File fwFile = new File(fwUpdateFilePath);

        if (fwFile.exists()) {
            fwFile.delete();
        }

        try {
            FileOutputStream fo = new FileOutputStream(fwFile);

            // Copy it to local file at the fixed path by AppData class
            byte[] buffer = new byte[2048];
            for (int n = is.read(buffer); n >= 0; n = is.read(buffer)) {
                try {
                    fo.write(buffer, 0, n);
                } catch (Exception e) {
                    mDbg.trace("Could not copy bundled firmware to local file: " + e.getMessage());
                    is.close();
                    fo.close();
                    return false;
                }
            }

            fo.close();
        } catch (Exception e) {
            mDbg.trace("Could not open local firmware file to write: " + e.getMessage());
            return false;
        }

        try {
            is.close();
        } catch (Exception e) {
            return false;
        }

        showProgressPopup("Please wait while firmware is upgraded...");

        return mCableDriver.updateFirmware(fwUpdateFilePath, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                if (!result) {
                    hideProgressPopup();
                    showAlertMessage("Firmware upgrade failed. Please see diagnostics section in app's help menu.", null);
                }

                return result;
            }
        });
    }

    private void refreshRequiredDatabasesAfterCableInit() {
        mDbg.trace();

        if (mCableHardwareVersion == ProductSpecs.HW_VERSION_2_INEDA) {
            mDbg.trace("V2 hardware do not need any refreshing of db. Here we go for downloading the smartdata databases.");
            fetchAllSmartDataDatabases(new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {

                    if (!mCableDriver.isCableConnected()) {
                        mDbg.trace("Cable disconnected while fetching smartdata databases!");
                        return false;
                    }

                    mDbg.trace("Fetching smartdata databases finished with result: " + result + ", its showtime!");
                    updateHomeFragmentOnCableConnection();

                    return result;
                }
            });
        } else {
            mDbg.trace("V1 hardware requires downloading the thumbnail database and process it.");
            refreshAllRequiredDatabasesAndRefreshViewModel(new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {

                    if (!mCableDriver.isCableConnected()) {
                        mDbg.trace("Cable disconnected during thumbnail processing!");
                        return false;
                    }

                    mDbg.trace("Refreshing thumbnail databases finished with result: " + result + ", its showtime!");

                    updateHomeFragmentOnCableConnection();

                    /*    GenUtils.copyDbFileToDownloads("thumbnails.db");
                    GenUtils.copyDbFileToDownloads("Contacts.db");
                    GenUtils.copyDbFileToDownloads("Messages.db");
                    GenUtils.copyDbFileToDownloads("Calendar.db");*/

                    return result;
                }
            });
        }
    }

    private void updateHomeFragmentOnCableConnection() {
        mDbg.trace();
        mHomeFragment.removeDisconnectedStateUi();
    }

    @Override
    public void onDisconnectedStateUiFinished() {
        mDbg.trace();

        // Arun: 28June2017: Moved the followng two lines from showAllHomeFragmentSegmentsOnCableConnection to here - because
        // the Fresh cable info creation in Cable presenter can take a long time is securedb is big. So we need the init progress bar
        // which is available for us now. Else I will have to show it twice which is ugly.

        // VERY IMPORTANT: to call this to update the home fragment.
        CableInfo cableInfo = getFreshCableInfo();

        // we also need PhoneInfo view model for home fragment. instantiate it here in MainActivity.
        mHomeFragment.update(mPhoneInfo, cableInfo);

        // phew! long way passed...
        mHomeFragment.hideCableInitProgressBar();

        setStopButtonProperties(true, true, false);
        mHomeFragment.showAutoBackupCountDown();
    }

    @Override
    public void onAutoBackupCountDownEnd() {
        mDbg.trace();

        setStopButtonProperties(false, false, false);
        // This event would have been noted by Viewcontroller and it will start backup
        // when segment animation is over.
        mHomeFragment.removeAutoBackupCountDown();
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            return;
        }

        showAllHomeFragmentSegmentsOnCableConnection();
    }

    /**
     * This is perhaps the MOST IMPORTANT method in the logic flow.
     */
    private void showAllHomeFragmentSegmentsOnCableConnection() {
        mDbg.trace();

        mHomeFragment.showSegmentsOnCableConnection();
        showAllMenuItems(true);

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            hideMenuIcon();
        }

        if (isNetworkFeatureEnabled() && !mCableDriver.isRemoteCable()) {
            mDbg.trace("Starting meem network service");
            startMeemNetServer();
        } else {
            mDbg.trace("Network feature is disabled. Not starting server");
        }

        if (mCableDriver.isRemoteCable()) {
            mDbg.trace("Ready to operate as as network client");
            mHomeFragment.onRemoteClientStart(null);
        }
    }

    /**
     * This is a very important method to refresh the view model. This is just a call to a new instance of view model from presenter. This
     * is given a special status because this is the practical core of the model-view-presneter. Also, whenever ViewModel is updated, you
     * must remember that the VaultInfo object reference inside PhoneInfo member in MainActivity has become invalid.
     *
     * @return
     */
    private CableInfo getFreshCableInfo() {
        mDbg.trace();

        // freshly instantiate view model for cable (CableInfo) using Cable Presenter.
        CableInfo cableInfo = mCablePresenter.getCableViewModel();
        mPhoneInfo.setVaultInfo(cableInfo.getVaultInfo(mPhoneInfo.getmUpid()));

        return cableInfo;
    }

    private boolean refreshAllRequiredDatabasesAndRefreshViewModel(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("Cable is disconnected!");
            return responseCallback.execute(false, null, null);
        }

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            return responseCallback.execute(true, null, null);
        }

        if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
            // first get current humbnail db version
            GenericDataThumbnailDatabase genThumbDb = new GenericDataThumbnailDatabase(mMobilePhone.getUpid());
            int genThumbDbVersion = genThumbDb.getThumbnailDbVersion();

            return mCableDriver.getThumbnailDb(mAppData.getGenDataThumbnailDbFullPath(), genThumbDbVersion, new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    if (!result) {
                        mDbg.trace("Thumbnail database retrieval failed. Proceeding anyway.");
                    }

                    mDbg.trace("Thumbnail database retrieval finished. Sanitizing...");
                    GenericDataThumbnailDatabase genThumbDb = new GenericDataThumbnailDatabase(mMobilePhone.getUpid());
                    genThumbDb.sanitizeDownloadedDb();

                    mDbg.trace("Fetching all smart data files...");

                    return mCableDriver.getSessionlessSmartData(new ResponseCallback() {
                        @Override
                        public boolean execute(boolean result, Object info, Object extraInfo) {
                            mDbg.trace("Fetching all smart data files finished with result: " + result + ", processing...");
                            return processSessionlessSmartData(responseCallback);
                        }
                    });
                }
            });
        } else {
            mDbg.trace("Refreshing all databases by downloading them again...");
            return mCableDriver.refreshAllDatabases(new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mDbg.trace("Downloding config and secure db result: " + result);

                    GenUtils.copyDbFileToDownloads("config.db", "config-post.db");
                    GenUtils.copyDbFileToDownloads("secure.db", "secure-post.db");

                    mDbg.trace("Fetching all smartdata databases...");
                    return fetchAllSmartDataDatabases(responseCallback);
                }
            });
        }
    }

    /**
     * This method implements the processing of all smart data files downloaded during (1) cable connection and (2) end of backup session.
     * Each file of sessionless smart data will have the name in the following format: <p> upid-category-mirror-tmp.json OR
     * upid-category-plus-tmp.json </p> <p> Since conversion of json files to db is time consuming, each smart data file is checksum
     * compared with existing sessionless smart data files downloaded last time. </p>
     *
     * @return
     */

    private boolean processSessionlessSmartData(final ResponseCallback responseCallback) {
        mDbg.trace();

        ArrayList<SmartDataSpec> smartDataSpecsList = getShortListedSessionlessSmartDataSpecs();
        mDbg.trace("Need to refresh smart data database for the following categories: " + smartDataSpecsList);

        for (SmartDataSpec smartDataSpec : smartDataSpecsList) {
            JsonToSqlAsync mJsonToSqlAsync = new JsonToSqlAsync(smartDataSpec, responseCallback);
            smartDataAsyncTaskList.add(mJsonToSqlAsync);
            mJsonToSqlAsync.execute();
        }

        return true;
    }

    private void abortAllJsonToSqlAsyncTasks() {
        mDbg.trace();
        for (JsonToSqlAsync asyncTask : smartDataAsyncTaskList) {
            switch (asyncTask.getSmartDataSpec().mCatCode) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    Contacts aCon = asyncTask.getContacts();
                    if (null != aCon) aCon.abort();
                    mDbg.trace("Contacts JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    Calenders aCal = asyncTask.getCalenders();
                    if (null != aCal) aCal.abort();
                    mDbg.trace("Calenders JsonToSql processing Aborted");
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    Messages aMsg = asyncTask.getMessages();
                    if (null != aMsg) aMsg.abort();
                    mDbg.trace("Messages JsonToSql processing Aborted");
                    break;
                default:
                    mDbg.trace("onCancelled :In-Valid Category Code");

            }
        }

    }

    /**
     * This method does the size/checksum comparison of current smart data files (that were generated in last backup) and the just
     * downloaded sessionless smart data files.
     *
     * @return
     */
    private ArrayList<SmartDataSpec> getShortListedSessionlessSmartDataSpecs() {
        mDbg.trace();

        Map<String, MeemVault> vaultsMap = mCableDriver.getCableModel().getVaults();

        ArrayList<Byte> smartCats = new ArrayList<>();
        smartCats.add(MMPConstants.MMP_CATCODE_CONTACT);
        smartCats.add(MMPConstants.MMP_CATCODE_MESSAGE);
        smartCats.add(MMPConstants.MMP_CATCODE_CALENDER);

        AppLocalData appData = AppLocalData.getInstance();

        ArrayList<SmartDataSpec> shortList = new ArrayList<>();

        for (String upid : vaultsMap.keySet()) {
            for (Byte cat : smartCats) {
                boolean addMirr = false;
                boolean addPlus = false;

                String tempMirrPath = appData.getSmartCatTempFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), true);
                String archMirrPath = appData.getSmartCatArchFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), true);

                String tempPlusPath = appData.getSmartCatTempFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), false);
                String archPlusPath = appData.getSmartCatArchFilePath(upid, MMLCategory.toSmartCatString(cat.intValue()), false);

                String tempMirrCSum = GenUtils.getFileMD5(tempMirrPath, null);
                String archMirrCSum = GenUtils.getFileMD5(archMirrPath, null);
                if (!tempMirrCSum.equals(archMirrCSum)) {
                    File tmpMirrFile = new File(tempMirrPath);
                    if (tmpMirrFile.exists()) {
                        addMirr = true;
                    }
                } else if (tempMirrCSum.equals(GenUtils.EMPTY_INPUT_MD5SUM) && archMirrCSum.equals(GenUtils.EMPTY_INPUT_MD5SUM)) {
                    addMirr = true;
                }

                String tempPlusCSum = GenUtils.getFileMD5(tempPlusPath, null);
                String archPlusCSum = GenUtils.getFileMD5(archPlusPath, null);
                if (!tempPlusCSum.equals(archPlusCSum)) {
                    File tmpPlusFile = new File(tempPlusPath);
                    if (tmpPlusFile.exists()) {
                        addPlus = true;
                    }
                } else if (tempPlusCSum.equals(GenUtils.EMPTY_INPUT_MD5SUM) && archPlusCSum.equals(GenUtils.EMPTY_INPUT_MD5SUM)) {
                    addPlus = true;
                }

                SmartDataSpec spec = new SmartDataSpec(upid, cat, addMirr ? tempMirrPath : null, addPlus ? tempPlusPath : null);

                shortList.add(spec);
            }
        }

        return shortList;
    }

    // Important: Accessory reference passed to this method can be null.
    @Override
    public void onAccessoryDisconnected(UsbAccessory accessory) {
        mDbg.trace();

        // Dismiss any dialogs
        if (mFirstAgreementAlert != null) {
            mFirstAgreementAlert.dismiss();
        }

        if (mForcedFwUpdateAlert != null) {
            mForcedFwUpdateAlert.dismiss();
        }

        if (mGenericAlert != null) {
            mGenericAlert.dismiss();
        }

        if(mProgressPopup != null) {
            mProgressPopup.dismiss();
        }

//        BugId:247 ,Not getting the response of delete vault in DetailFragment so unable to dismiss the progressBar,
// Trying to dismiss from MainActivity, Need to find the actual solution
        if(mSettingsFragment!=null && mSettingsFragment.getDetailFragmentObj() !=null && mSettingsFragment.getDetailFragmentObj().mInitProgress!=null){
            mDbg.trace("Dismiss progressBar when cable disconnected");
            mSettingsFragment.getDetailFragmentObj().mInitProgress.dismiss();
        }

        // Inform cable driver that we are disconnected
        if (mCableDriver != null) {
            mCableDriver.onCableDisconnect();
        }

        if (null != mSessionManager && mSessionManager.isSessionLive()) {
            mDbg.trace("We have an active session. Force failing it");
            mSessionManager.onCableDisconnect();
        }

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            if (null != mDummySession) {
                mDummySession.stop();
            }

            notifySessionResult(false, "Cable disconnect");
        }

        //Cancel All AsyncTaks if running
        abortAllJsonToSqlAsyncTasks();

        // cancel Individual restore sync Task
        if (null != mCablePresenter) {
            mCablePresenter.cancelSelectiveRestoreAsyncTasks();
        }

        // Cancel any pending runnables we might have posted in our handler.
        mHandler.removeCallbacks(mRunnableAfterSmsMgmtPermission);
        mHandler.removeCallbacks(sessionXfrCommentaryRunnable);
        mHandler.removeCallbacks(sessionPrepCommentaryRunnable);

        // Do the GUI stuff with home fragment.
        updateUIonCableDisconnect();

        // Arun: MNet: 23June2017
        stopMeemNetServer();

        mDummyOkToSend = true;
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Accessory related functions -----------------------
    // ---------------------------------------------------------------------------


    // ---------------------------------------------------------------------------
    // ------------------ Start: Cable driver helper functions -------------------
    // ---------------------------------------------------------------------------

    private void updateUIonCableDisconnect() {
        mDbg.trace();

        if (isUIVisible) updateUIOnUIVisible();
        else isPopupNeeded = true;
    }

    private void updateUIOnUIVisible() {
        mDbg.trace();

        setBackPressDisable(false);
        setStopButtonProperties(false, false, false);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        hideSoftKey();

        // TODO: If home fragment is not the one presently on screen.
        if (fragmentManager.getBackStackEntryCount() > 1) callBackHomeFragment();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHomeFragment.onCableDisconnection();
            }
        }, GuiSpec.ANIM_TEST_DURATION_FAST);

        setAppTitle(getString(R.string.app_name));
        showMenuHideBackIcon();

        hideToolbarIcons();
        showAllMenuItems(false);
        setOptionMenuContent(false, false, false, false);
    }

    @Override
    public String getPhoneUpid() {
        mDbg.trace();
        return mMobilePhone.getUpid();
    }

    @Override
    public String getPhoneName() {
        mDbg.trace();

        String phoneBrand = mMobilePhone.getBrand();
        String firstLetter = String.valueOf(phoneBrand.charAt(0));
        phoneBrand = firstLetter.toUpperCase() + phoneBrand.substring(1);

        return phoneBrand;
    }

    @Override
    public File createPinf() {
        mMobilePhone.createPinf();
        String pinfPath = mAppData.getPinfPath();
        return new File(pinfPath);
    }

    @Override
    public void onVirginCableConnection() {
        mDbg.trace();
        mHomeFragment.hideCableInitProgressBar();
        mHomeFragment.onVirginCableConnection();
    }

    @Override
    public void onMaxNumVaultsDetected() {
        mHomeFragment.hideCableInitProgressBar();
        showAlertMessage("You can backup only upto 3 phones with a MEEM cable!", null); // TODO: Translation
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Cable driver helper functions ---------------------
    // ---------------------------------------------------------------------------

    @Override
    public void onUnregisteredPhoneConnection() {
        mDbg.trace();
        mHomeFragment.hideCableInitProgressBar();
        mHomeFragment.onUnregisteredPhoneConnection();
    }

    @Override
    public void onCriticalError(String msg) {
        if (null != mHomeFragment) {
            mHomeFragment.hideCableInitProgressBar();
        }

        String critMsg = "Critical error: " + msg;

        // TODO: Minor hack for showing locked cable condition. Make it better.
        if (critMsg.contains("Cable is locked")) {
            showToast(msg);
        }

        mDbg.trace(critMsg);

        /*new AlertDialog.Builder(this).setTitle("Critical error").setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setIcon(android.R.drawable.ic_dialog_alert).show();*/
    }

    // ---------------------------------------------------------------------------
    // ------------------ Start: Cable presenter helper functions ----------------
    // ---------------------------------------------------------------------------
    @Override
    public void startBackup(VaultInfo vault, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("Cable disconnected!");
            return;
        }

        setStopButtonProperties(true, false, true);
        setAppTitle(getString(R.string.confirming_data));

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            mSessionManager = null;
            mDummySession = new DummySession(DummySession.DummySessionType.DUMMY_BACKUP);
            mDummySession.start();
            mHandler.postDelayed(sessionXfrCommentaryRunnable, 5000);
        } else {
            mSessionManager.setDriverInstance(mCableDriver);
            if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
                mSessionManager.startBackup(mCablePresenter.getCableViewModel(), catInfos);
            } else {
                // Fresh model creation is costly for v2. Logically, we are updating the model in presenter everytime is is changed anyway. 
                mSessionManager.startBackup(mCablePresenter.getCableInfo(), catInfos);
            }
        }

        mHomeFragment.onSessionStart(vault, catInfos);
        mHandler.postDelayed(sessionPrepCommentaryRunnable, 5000);
    }

    @Override
    public void startRestore(VaultInfo vaultInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        setStopButtonProperties(true, false, true);
        setAppTitle(getString(R.string.confirming_data));

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            mSessionManager = null;
            mDummySession = new DummySession(DummySession.DummySessionType.DUMMY_RESTORE);
            mDummySession.start();
            mHandler.postDelayed(sessionXfrCommentaryRunnable, 5000);
        } else {
            mSessionManager.setDriverInstance(mCableDriver);

            if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
                mSessionManager.startRestore(mCablePresenter.getCableViewModel(), catInfos);
            } else {
                // Fresh model creation is costly for v2. Logically, we are updating the model in presenter everytime is is changed anyway. 
                mSessionManager.startRestore(mCablePresenter.getCableInfo(), catInfos);
            }
        }

        mHomeFragment.onSessionStart(vaultInfo, catInfos);
        mHandler.postDelayed(sessionPrepCommentaryRunnable, 5000);
    }

    @Override
    public void startCopy(VaultInfo vaultInfo, ArrayList<CategoryInfo> catInfos) {
        mDbg.trace();

        setStopButtonProperties(true, false, true);
        setAppTitle(getString(R.string.confirming_data));

        mSessionManager.setDriverInstance(mCableDriver);

        if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
            mSessionManager.startCopy(mCablePresenter.getCableViewModel(), vaultInfo, catInfos);
        } else {
            // Fresh model creation is costly for v2. Logically, we are updating the model in presenter everytime is is changed anyway. 
            mSessionManager.startCopy(mCablePresenter.getCableInfo(), vaultInfo, catInfos);
        }

        mHomeFragment.onSessionStart(vaultInfo, catInfos);
        mHandler.postDelayed(sessionPrepCommentaryRunnable, 5000);
    }

    @Override
    public void abortSession() {
        mDbg.trace();

        String msg = "Aborting...";
        switch (mSessionManager.getSessionType()) {
            case BACKUP:
                msg = getString(R.string.aborting_backup);
                break;
            case RESTORE:
                msg = getString(R.string.aborting_restore);
                break;
            case COPY:
                msg = getString(R.string.aborting_copy);
                break;
        }

        setAppTitle(msg);

        mSessionManager.abort();
    }

    @Override
    public void onSoundUpdate(boolean isOn) {
        mDbg.trace();
        // TODO
    }

    @Override
    public void onCatModeChanged(VaultInfo vaultInfo) {
        mDbg.trace();
        updateHomeFragmentOnCableUpdate();
        showToast("List of categories to backup are updated");
    }

    @Override
    public void onVaultNameChanged(VaultInfo vaultInfo) {
        mDbg.trace();
        updateHomeFragmentOnCableUpdate();
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Cable presenter helper functions ----------
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // ------------------ Start: Session related functions -----------------------
    // ---------------------------------------------------------------------------

    @Override
    public void onVirginCablePinSetupComplete() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            return;
        }

        mHomeFragment.showCableInitProgressBar();

        mCableDriver.onVirginCablePINSettingFinished(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                updateFirmwareAsNeeded(); // if not, refresh thumbnail db will be done internally.
                return result;
            }
        });
    }

    @Override
    public void onUnregisteredPhoneAuthComplete() {
        mDbg.trace();

        if (!mCableDriver.isCableConnected()) {
            return;
        }

        mHomeFragment.showCableInitProgressBar();

        mCableDriver.onPhoneAuthSucceeded(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                updateFirmwareAsNeeded(); // if not, refresh thumbnail db will be done internally.

                if (result && mCableDriver.isRemoteCable()) {
                    // we are operating in net client mode. send master a message to refresh ui so that the new vault will show up
                    // in its ui (master will relay it to others)
                    mDbg.trace("Net client, phone registration complete, sending refresh ui message");
                    mCableDriver.sendMessageToNetMaster(MNetConstants.MNET_MSG_UI_REFRESH, null);
                }

                return result;
            }
        });
    }

    @Override
    @SuppressLint("NewApi")
    public void requestSmsManagementPermission() {
        mDbg.trace();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            mDbg.trace("Android version is not KITKAT or above, doing nothing.");
            mSessionManager.accessForSMSManagement(true);
            return;
        }

        final String meemPackageName = getPackageName();
        String defSmsPackageName = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());

        if(null == defSmsPackageName) {
            mDbg.trace("No default sms app. Probably no SMS support. Marking access denied.");
            mSessionManager.accessForSMSManagement(false);
            return;
        }

        if (!defSmsPackageName.equals(meemPackageName)) {
            mDefaultSmsApp = defSmsPackageName;

            if (null != mDefaultSmsApp) {
                mDbg.trace("Default SMS app is: " + mDefaultSmsApp);

                // Arun: 03Aug2014: Removed the info pop-up (well, in my opinion, that was a better thing to do...)
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, meemPackageName);
                        startActivity(intent);

                        Thread defaultSmsProviderCheckerThread = new Thread() {
                            public void run() {
                                Calendar cal = Calendar.getInstance();
                                long startTime = cal.getTimeInMillis();

                                while (true) {
                                    String defApp = Telephony.Sms.getDefaultSmsPackage(mUiCtxt.getAppContext());
                                    if (defApp != null && defApp.equals(meemPackageName)) {
                                        mDbg.trace("Got permission for being default messaging app!");
                                        break;
                                    }
                                    try {
                                        mDbg.trace("Waiting for being default messaging app permission");
                                        Thread.sleep(500);

                                        Calendar cal1 = Calendar.getInstance();
                                        long now = cal1.getTimeInMillis();

                                        if ((now - startTime) > ProductSpecs.WAIT_TIMEOUT_FOR_DEFAULT_SMS_APP) {
                                            mDbg.trace("Warning: Wait for permission to become default messaging app timed out!");
                                            mDefaultSmsApp = null;
                                            break;
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    if (mSessionManager != null) {
                                        if (mSessionManager.isAbortPending()) {
                                            mDbg.trace("Warning: user requested abort while waiting for default SMS app permission");
                                            break;
                                        }
                                    }
                                }

                                mRunnableAfterSmsMgmtPermission = new Runnable() {
                                    public void run() {
                                        if (mSessionManager != null)
                                            mSessionManager.accessForSMSManagement(true);
                                    }
                                };

                                mHandler.post(mRunnableAfterSmsMgmtPermission);
                            }
                        };

                        defaultSmsProviderCheckerThread.start();
                    }
                });
            } else {
                mDbg.trace("There is no default SMS app");
                mSessionManager.accessForSMSManagement(true);
            }
        } else {
            mDbg.trace("Meem is the default SMS app: " + meemPackageName);
            mSessionManager.accessForSMSManagement(true);
        }

    }

    @SuppressLint("NewApi")
    public void requestSMSManagementPermission(boolean noSession) {
        mDbg.trace();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            mDbg.trace("Android version is not KITKAT or above, doing nothing.");
            if (!noSession) mSessionManager.accessForSMSManagement(true);
            return;
        }

        if (!noSession) {
            mDbg.trace("Redirecting request to session");
            requestSmsManagementPermission();
            return;
        }

        String meemPackageName = getPackageName();
        String defSmsPackageName = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());

        if(null == defSmsPackageName) {
            mDbg.trace("Default sms package is null. Perhaps no sms support");
            return;
        }

        if (!defSmsPackageName.equals(meemPackageName)) {
            mDefaultSmsApp = defSmsPackageName;
            mDbg.trace("Default SMS app is: " + mDefaultSmsApp);

            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, meemPackageName);
            startActivity(intent);
        } else {
            mDbg.trace("Meem is already the default SMS app: " + meemPackageName);
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void dropSmsManagementPermission() {
        mDbg.trace();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            mDbg.trace("Android version is not KITKAT or above, doing nothing.");
            return;
        }

        String packageName = getPackageName();
        String defSmsPackageName = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());

        if(null == defSmsPackageName) {
            mDbg.trace("Null default sms package. Perhaps no sms support");
            return;
        }

        if (defSmsPackageName.equals(packageName)) {
            mDbg.trace("We are the current default SMS app");

            // ideally, we should be having this info in our member variable. if not, something went wrong and activity restarted.
            if (mDefaultSmsApp == null) {
                mDbg.trace("We don't know who the previous guy was (app might have crashed)!");
                showAlertMessage("Please check your default SMS app in phone settings", null);
                return;
            }

            mDbg.trace("Resetting default SMS app to: " + mDefaultSmsApp);
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, mDefaultSmsApp);
            startActivity(intent);
        } else {
            mDbg.trace("We are NOT the current default SMS app. It is: " + mDefaultSmsApp + ", Doing nothing now.");
        }
    }

    @Override
    public void notifyInsufficientCableStorage(long conSize, long msgSize, long calSize, long phoSize, long vidSize, long musSize, long docSize, long availableSize) {
        mDbg.trace();
        showAlertMessage(getString(R.string.there_is_no_enough_memory_in_the_device_for_backup_operation), new Runnable() {
            @Override
            public void run() {
                setAppTitle("Out of memory");
            }
        });

        if (mCablePresenter != null) {
            CableInfo cableInfo;
            if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
                cableInfo = mCablePresenter.getCableViewModel();
            } else {
                // Fresh model creation is costly for v2. Logically, we are updating the model in presenter everytime is is changed anyway. 
                cableInfo = mCablePresenter.getCableInfo();
            }
            if (cableInfo != null) {
                VaultInfo vaultInfo = cableInfo.getVaultInfo(mMobilePhone.getUpid());
                showOutOfMemofyFragment(vaultInfo, conSize, msgSize, calSize, phoSize, vidSize, musSize, docSize, availableSize);
            }
        }
    }

    @Override
    public void notifyInsufficientPhoneStorage() {
        mDbg.trace();

        String msg;
        SessionManager.SessionType sessType = mSessionManager.getSessionType();

        if (sessType == RESTORE) {
            msg = getString(R.string.there_is_no_enough_memory_in_the_device_for_restore_operation);
        } else {
            msg = getString(R.string.there_is_no_enough_memory_in_the_device_for_copy_operation);
        }

        showAlertMessage(msg, null);
    }

    @Override
    public void updateSessionDataXfrProgress(long total, long sofar) {
        mDbg.trace();
        setAppTitle(getString(R.string.transferring_date));
    }

    @Override
    public void notifySessionResult(final boolean result, final String message) {
        mDbg.trace();
        mDbg.trace("Session result message is: " + message);

        if (!mCableDriver.isCableConnected()) {
            return;
        }

        // Stop the session commentary runnables.
        mHandler.removeCallbacks(sessionXfrCommentaryRunnable);
        mHandler.removeCallbacks(sessionPrepCommentaryRunnable);

        if ((mSessionManager != null) && (mSessionManager.getSessionType() != SessionManager.SessionType.BACKUP)) {
            mDbg.trace("No need to refresh database unless session is for backup");
            mHomeFragment.onSessionEnd(result, message);

            setStopButtonProperties(false, false, false);
            setAppTitle(getString(R.string.home));

            if (ProductSpecs.DUMMY_CABLE_MODE) {
                hideMenuIcon();
            }

            return;
        }

        String msg = "Finalizing...";
        int sessType = 0;

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            sessType = mDummySession.getDummySessionType().ordinal();
        } else {
            sessType = mSessionManager.getSessionType().ordinal();
        }

        switch (sessType) {
            case 1:
                msg = getString(R.string.finalizing_backup);
                break;
            case 2:
                msg = getString(R.string.finalizing_restore);
                break;
            case 3:
                msg = getString(R.string.finalizing_copy);
                break;
        }

        setAppTitle(msg);

        refreshAllRequiredDatabasesAndRefreshViewModel(new ResponseCallback() {
            @Override
            public boolean execute(boolean res, Object info, Object extraInfo) {

                if (result) {
                    // very important to call this to update the view model.
                    CableInfo cableInfo = getFreshCableInfo();
                    mHomeFragment.update(mPhoneInfo, cableInfo);
                }

                mHomeFragment.onSessionEnd(result, message);

                setStopButtonProperties(false, false, false);
                setAppTitle(getString(R.string.home));

                if (ProductSpecs.DUMMY_CABLE_MODE) {
                    hideMenuIcon();
                }

                if (mCableHardwareVersion == ProductSpecs.HW_VERSION_2_INEDA) {
                    if (mCableDriver.isRemoteCable()) {
                        mDbg.trace("Informing meem network master that a backup is completed in this phone.");
                        mCableDriver.sendMessageToNetMaster(MNetConstants.MNET_MSG_UI_REFRESH, null);
                    } else {
                        if (isNetworkFeatureEnabled()) {
                            mDbg.trace("Informing meem network clients that a backup is completed in this master phone.");
                            // inform connected clients (there will be clients here only if cable is connected to this phone)
                            for (MNetClientHandler client : mConnectedClients) {
                                // TODO: Desktop app expects UI refresh first and then auto copy message. Wrong ideas...
                                client.sendUiRefreshMsg(null);
                                client.sendStartAutoCopyMsg(mMobilePhone.getUpid());
                            }
                        } else {
                            mDbg.trace("Network feature is disabled. No need to broacast backup complete event in this phone");
                        }
                    }
                }

                return result;
            }
        });

        //onBackup completed reset the badge
        if (result) {
            removeBadge();
            resetAlaramForBadge();
            setBackupTime();
        }
    }

    private void setBackupTime() {
        mDbg.trace();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putLong(AppPreferences.LAST_BACKUP_TIME, System.currentTimeMillis()).commit();
    }

    private void showOutOfMemofyFragment(VaultInfo vaultInfo, long conSize, long msgSize, long calSize, long phoSize, long vidSize, long musSize, long docSize, long availableSize) {
        mDbg.trace();

        OutOfMemorySetting outOfMemorySetting = new OutOfMemorySetting();
        outOfMemorySetting.setValues(vaultInfo, conSize, msgSize, calSize, phoSize, vidSize, musSize, docSize, availableSize);

        showMenuFragment(outOfMemorySetting, "outofmemmory");

        hideMenuIcon();
        setToolBarforOutOfMemory();
    }

    public void setToolBarforOutOfMemory() {
        mToolBarRightTV.setVisibility(View.VISIBLE);
        mToolBarRightTV.setText(R.string.cancel);
    }

    public void setSaveButton(boolean mVisible) {
        if (mVisible) {
            mToolbarTVSave.setVisibility(View.VISIBLE);
        } else {
            mToolbarTVSave.setVisibility(View.GONE);

        }
    }

    @Override
    public void updateMediaLibrary(String filePath) {
        mDbg.trace();

        // dont do this for our private file formats
        if (filePath != null) {
            if (filePath.endsWith(".db") || filePath.endsWith(".mml")) {
                return;
            }
        }

        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{filePath}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                mDbg.trace("Media scan completion callback for: " + path);
            }
        });
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Session related functions -------------------------
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // ------------------ Start: Legacy event handling functions -----------------
    // ---------------------------------------------------------------------------
    //
    public void onSessionPrepCompletedEvent(boolean result) {
        mDbg.trace();

        mHandler.removeCallbacks(sessionPrepCommentaryRunnable);

        GenUtils.copyDbFileToDownloads("config.db", "config-prep.db");
        GenUtils.copyDbFileToDownloads("secure.db", "secure-prep.db");

        if (result) {
            mDbg.trace("Session preperation succeeded");
            mSessionManager.onSessionPreperationSucceeded();
        } else {
            mDbg.trace("Session preperation failed!");
            mSessionManager.onSessionPreperationFailed();
        }
    }

    public void onSessionXfrStartedEvent() {
        mDbg.trace();

        mSessionManager.onSessionXfrStarted();

        // Start updating title with estimated time remaining.
        mHandler.postDelayed(sessionXfrCommentaryRunnable, 1000);
    }

    public void onFileReceivedEvent(String path) {
        mDbg.trace("Received file: " + path);

        if (mSessionManager != null && mSessionManager.isSessionLive()) {
            mSessionManager.onFileReceivedFromMeem(path);
        } else {
            updateMediaLibrary(path);
        }
    }

    public void onFileSentEvent(String path) {
        mDbg.trace("Sent file: " + path);

        if (mSessionManager != null && mSessionManager.isSessionLive()) {
            mSessionManager.onFileSentToMeem(path);
        }
    }

    public void onSessionProgressUpdateEvent(MMPSessionStatusInfo sessionStatusInfo) {
        mDbg.trace();
        mHomeFragment.onSessionProgressUpdate(sessionStatusInfo);
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Legacy Event handling functions -------------------
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // ------------------ Start: Non-gui related functions -----------------------
    // ---------------------------------------------------------------------------

    /**
     * Note: call this only once from MainActivity. Others must use the info from it using the member variables:
     * boolean mIsFreshInstallOfApp and mIsTheFirstRunOfThisAppVersion.
     */
    private void gatherVersionDetails() {
        mDbg.trace();

        mIsFreshInstallOfApp = false;
        mIsTheFirstRunOfThisAppVersion = false;

        // get current version code
        mCurrentAppVersionCode = 0;
        try {
            // can also use BuildConfig.VERSION_CODE
            mCurrentAppVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            mDbg.trace("App version: " + mCurrentAppVersionCode);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            // well...
        }

        // get saved version code
        SharedPreferences prefs = getSharedPreferences(AppPreferences.GENERAL_USAGE, MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(AppPreferences.KEY_INSTALLED_VERSION, AppPreferences.ERROR_DOESNT_EXIST);

        // check for first run or upgrade
        if (mCurrentAppVersionCode == savedVersionCode) {
            // this is just a normal run
            return;
        }

        if (savedVersionCode == AppPreferences.ERROR_DOESNT_EXIST) {
            mIsFreshInstallOfApp = true;
            mIsTheFirstRunOfThisAppVersion = true;
        } else if (mCurrentAppVersionCode > savedVersionCode) {
            mIsTheFirstRunOfThisAppVersion = true;
        }

        // update the shared preferences with the current version code
        prefs.edit().putInt(AppPreferences.KEY_INSTALLED_VERSION, mCurrentAppVersionCode).commit();
    }

    private boolean didUserAgreementComplete() {
        mDbg.trace();

        SharedPreferences prefs = getSharedPreferences(AppPreferences.GENERAL_USAGE, MODE_PRIVATE);
        int firstAgreementOk = prefs.getInt(AppPreferences.KEY_FIRST_AGREEMENT_DONE, AppPreferences.ERROR_DOESNT_EXIST);

        if (firstAgreementOk == AppPreferences.ERROR_DOESNT_EXIST || firstAgreementOk != 1) {
            return false;
        }

        return true;
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------------------
    // ------------------ End: Non-gui related functions -----------------------
    // ---------------------------------------------------------------------------

    // ---------------------------------------------------------------------------
    // ------------------ Helper functions ---------------------------------------
    // ---------------------------------------------------------------------------

    void showAlertMessage(String msg, final Runnable continueRun) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(msg).setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (continueRun != null) {
                    continueRun.run();
                }

                dialog.dismiss();
            }
        });

        mGenericAlert = builder.create();
        mGenericAlert.show();
    }


    @Override
    protected void onStop() {

        hideSoftKey();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(AppPreferences.BACKUP_NOTIFICATION_STATUS, true)) {

            SharedPreferences prefs = getSharedPreferences("badge_number", MODE_PRIVATE);
            int badgeCount = prefs.getInt("count", 0);
            if (badgeCount > 0) {
                ShortcutBadger.applyCount(this, badgeCount);
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mDbg.trace();

        if (mHomeFragment != null) {
            mHomeFragment.onCableDisconnection();
        }

        if (mSessionManager != null && mSessionManager.isSessionLive()) {
            mSessionManager.abort();
        }

        // MNet: Arun: 23June2017: Cleanup if server mode
        stopMeemNetServer();

        // MNet: Arun: 11Aug2017: Cleanup if client mode
        stopMeemNetClient();

        super.onDestroy();
    }

    // TODO: What we are doing here is a bit nasty... accessing MeemCoreV2 directly like this!
    private void stopMeemNetClient() {
        mDbg.trace();

        // Cleanup if client is running.
        if (null != mNetClient) {
            mNetClient.stopSearchForMaster();
        }

        // This will make sure MeemCoreV2RemoteProxy instance is stopped.
        MeemCoreV2.getInstance().stop();
    }

    public int getCableVersion() {
        return mCableHardwareVersion;
    }

    public boolean isMeemConnectedOverNetwork() {
        mDbg.trace();

        if (mCableDriver == null) {
            return false;
        }
        return mCableDriver.isRemoteCable();
    }

    // By default, network is disabled.
    public boolean isNetworkFeatureEnabled() {
        mDbg.trace();

        if(mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
            return false;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(AppPreferences.NW_SETTING_STATUS, false);
    }

    public boolean isMasterOfNetwork() {
        mDbg.trace();

        boolean nw = isNetworkFeatureEnabled();
        boolean local = !mCableDriver.isRemoteCable();

        return (nw && local);
    }

    private void showWhatsNew() {
        mDbg.trace();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.whatsnewtitle);
        builder.setMessage(R.string.whatsNew).setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mGenericAlert.dismiss();
            }
        });

        mGenericAlert = builder.create();
        mGenericAlert.show();

/*        JustifiedTextAlertDialog whatsNewDlg = new JustifiedTextAlertDialog();
        whatsNewDlg.create(this, getString(R.string.whatsnewtitle),getString(R.string.whatsNew), null);
        mGenericAlert = whatsNewDlg.getAlertDialogObject();
        mGenericAlert.show();*/
    }

    private void showWhatsNewHtml() {
        String BASE_URL = "file:///android_asset/html/";

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.whatsnew, null);
        dialogBuilder.setView(dialogView);

        WebView webView = (WebView) dialogView.findViewById(R.id.webNew);
        webView.loadUrl(BASE_URL + "whatsnew.html");
        Button okButton = (Button) dialogView.findViewById(R.id.okButton);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    /**
     * Show a progress dialog with given message. You can pass a custom dialog
     *
     * @param percent
     * @param progressDialog
     * @param s
     */
    public void showSimpleProgressBar(int percent, ProgressDialog progressDialog, String s) {
        progressDialog.setMessage("");
        progressDialog.setTitle("");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setProgress(percent);
        progressDialog.show();
    }

    @Override
    public void onLockedCableConnected() {
        mHomeFragment.hideCableInitProgressBar();
        showAlertMessage("This cable is locked because of too many wrong PIN attempts", null); // TODO: Translation
    }

    private boolean fetchAllSmartDataDatabases(final ResponseCallback responseCallback) {
        mDbg.trace();

        if (null == mCablePresenter) {
            mDbg.trace("WTF: Cable presenter null!");
            return responseCallback.execute(false, null, null);
        }

        if (!mCableDriver.isCableConnected()) {
            mDbg.trace("Cable is disconnected!");
            return responseCallback.execute(false, null, null);
        }

        Thread smartDataFetcherThread = new Thread(new Runnable() {
            Object monitor = new Object();
            boolean oneDone = false;

            @Override
            public void run() {
                // TODO: Arun: 28June2017: This is overkill since this costly operation is repeated in responseCallback.
                CableInfo cableInfo = mCablePresenter.getCableViewModel();

                LinkedHashMap<String, VaultInfo> vaultMap = cableInfo.getmVaultInfoMap();

                for (String upid : vaultMap.keySet()) {
                    VaultInfo vaultInfo = vaultMap.get(upid);
                    mDbg.trace("Downloading smartdata database for upid: " + vaultInfo.getUpid());

                    LinkedHashMap<Byte, CategoryInfo> catMap = vaultInfo.getmCategoryInfoMap();
                    for (Byte cat : catMap.keySet()) {
                        CategoryInfo catInfo = catMap.get(cat);

                        if (MMLCategory.isSmartCategoryCode(cat)) {
                            MMLSmartDataDesc smartDesc = toMMLSmartDataDesc(upid, catInfo);

                            if (!mCableDriver.fetchSmartData(upid, smartDesc, null)) {
                                mDbg.trace("Error while fetching smartdata db: " + smartDesc);
                            } else {
                                mDbg.trace("Downloaded smartdata db: " + smartDesc);
                            }
                        }

                        if (!mCableDriver.isCableConnected()) {
                            break;
                        }
                    }
                }

                if (!mCableDriver.isCableConnected()) {
                    mDbg.trace("Cable disconnected while downloading smartdata dbs!");
                    // responsecallbacks are only to be run from ui thread. so cant directly call it from here.
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            responseCallback.execute(false, null, null);
                        }
                    });

                    return;
                }

                mDbg.trace("All smart data files for all upids downloaded. Now, sanitizing...");

                // VERY IMPORTANT: Sanitize all smart data db files
                for (String upid : vaultMap.keySet()) {
                    VaultInfo vaultInfo = vaultMap.get(upid);
                    mDbg.trace("Sanitizing smartdata database for vault: " + vaultInfo.getmName());
                    LinkedHashMap<Byte, CategoryInfo> catMap = vaultInfo.getmCategoryInfoMap();

                    for (Byte cat : catMap.keySet()) {
                        if (MMLCategory.isSmartCategoryCode(cat)) {
                            mDbg.trace("Sanitizing smartdata cat (mirror and plus files): " + cat);
                            sanitizeSmartDataDbFiles(upid, cat);
                        }
                    }
                }

                // responsecallbacks are only to be run from ui thread. so cant directly call it from here.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        responseCallback.execute(true, null, null);
                    }
                });
            }
        });

        smartDataFetcherThread.start();

        return true;
    }

    private boolean sanitizeSmartDataDbFiles(String upid, byte catCode) {
        mDbg.trace();

        switch (catCode) {
            case MMPConstants.MMP_CATCODE_CONTACT:
                ContactsV2 contactsV2 = new ContactsV2(this, upid);
                contactsV2.getMirrTotalItemsCount();
                break;
            case MMPConstants.MMP_CATCODE_MESSAGE:
                MessagesV2 messagesV2 = new MessagesV2(this, upid);
                messagesV2.getMirrTotalItemsCount();
                break;
            case MMPConstants.MMP_CATCODE_CALENDER:
                CalendersV2 calendersV2 = new CalendersV2(this, upid);
                calendersV2.getMirrTotalItemsCount();
                break;
        }
        return false;
    }

    // === For V2 ===

    private MMLSmartDataDesc toMMLSmartDataDesc(String upid, CategoryInfo catInfo) {
        mDbg.trace();

        String mirrPhonePath = mAppData.getSmartDataV2DatabasePath(upid, catInfo.mMmpCode, true);
        String plusPhonePath = mAppData.getSmartDataV2DatabasePath(upid, catInfo.mMmpCode, false);

        MMLSmartDataDesc desc = new MMLSmartDataDesc();

        desc.mPaths[0] = mirrPhonePath;
        desc.mMeemPaths[0] = catInfo.mSmartMirrorMeemPath;
        desc.mSizes[0] = catInfo.mMirrorSizeKB;
        desc.mCSums[0] = catInfo.mSmartMirrorCSum;

        desc.mPaths[1] = plusPhonePath;
        desc.mMeemPaths[1] = catInfo.mSmartPlusMeemPath;
        desc.mSizes[1] = catInfo.mPlusSizeKB;
        desc.mCSums[1] = catInfo.mSmartPlusCSum;

        desc.mCatCode = catInfo.mMmpCode;

        return desc;
    }

    // ==== CableDriverListener Overrrides for V2
    @Override
    public PhoneDbModel getThisPhoneDbModel() {
        mDbg.trace();

        PhoneDbModel phoneDbModel = new PhoneDbModel();
        phoneDbModel.mUpid = mMobilePhone.getUpid();
        phoneDbModel.mName = getPhoneName();
        phoneDbModel.mOpetr = mMobilePhone.getOperator();
        phoneDbModel.mPltfrm = "Android";
        phoneDbModel.mVer = mMobilePhone.getVersion();
        phoneDbModel.mLang = mMobilePhone.getLanguage();
        phoneDbModel.mBrand = mMobilePhone.getModel();
        phoneDbModel.mMod_name = mMobilePhone.getModelNumber();

        return phoneDbModel;
    }

    // ==== CableDriverListener Overrrides for V2
    @Override
    public VaultDbModel getThisPhonesVaultDbModel() {
        mDbg.trace();

        VaultDbModel vaultDbModel = new VaultDbModel();

        vaultDbModel.mUpid = mMobilePhone.getUpid();
        vaultDbModel.mIs_migration = 0;
        vaultDbModel.mName = getPhoneName();
        vaultDbModel.mBackup_time = 0;
        vaultDbModel.mBackup_status = 0;
        vaultDbModel.mRestore_time = 0;
        vaultDbModel.mRestore_status = 0;
        vaultDbModel.mCopy_time = 0;
        vaultDbModel.mCopy_status = 0;
        vaultDbModel.mSync_time = 0;
        vaultDbModel.mSync_status = 0;
        vaultDbModel.mBackup_mode = 0;
        vaultDbModel.mSound = 1;
        vaultDbModel.mAuto = 1;

        return vaultDbModel;
    }

    /**
     * Early firmware hook for Ineda hardware - this will be invoked immediately after init sequence. Remember: for TI hardware, we will do
     * firmware update after GET_MSTAT response which happens much later after init sequence, GET_TIME, GET_RANDOM_SEED, GET_PASSWD and so
     * on.
     *
     * @param responseCallback
     *
     * @return
     */
    @Override
    public boolean earlyFirmwareHook(ResponseCallback responseCallback) {
        mDbg.trace();

        if (mCableDriver.isRemoteCable()) {
            mDbg.trace("Accessory is remote. We won't do firmware update remotely.");
            return responseCallback.execute(false, null, null);
        }

        // Arun: 20Aug2018: Cable Disconnect in between DeleteVault may cause orphan entries in DB in cable.
        // Check for it - Added this flagging in FW by Barath - check mail on same date.
        if(ProductSpecs.FIRMWARE_FLAG_DB_CORRUPT_ON_DELETEVAULT ==  mCableDriver.getFwDbStatus()) {
            mDbg.trace("DB corruption detected in firmware on last delete vault operation for upid:" + mCableDriver.getFwDelPendingUpid());
            showToast("Completing backup data deletion.");

            mCableDriver.deleteVault(mCableDriver.getFwDelPendingUpid(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    if(!result) {
                        showAlertMessage("Oops! something went wrong. Please reconnect the cable to proceed", null);
                    }

                    return result;
                }
            });
        }

        String fwVersion = mCableDriver.getFwVersion();
        VersionString curVersion = new VersionString(fwVersion);
        VersionString minVersion;

        // this check is not needed as this method is used only for Ineda hardware.
        if (mCableHardwareVersion == ProductSpecs.HW_VERSION_1_TI) {
            minVersion = new VersionString(ProductSpecs.MIN_FW_VERSION_FOR_TI_HW);
        } else {
            minVersion = new VersionString(ProductSpecs.MIN_FW_VERSION_FOR_INEDA_HW);
        }

        boolean updateNotNeeded = curVersion.isEqual(minVersion) || curVersion.isGreaterThan(minVersion);

        if (updateNotNeeded) {
            mDbg.trace("No need for fw update, currently: " + fwVersion);
            responseCallback.execute(false, null, null);
        } else {
            mDbg.trace("Required fw update, currently: " + fwVersion);
            // user must update. no other way!
            showFirmwareUpdateNotice();
        }

        return true;
    }

    private void showProgressPopup(String message) {
        mProgressPopup = new ProgressDialog(this);

        mProgressPopup.setMessage(message); // TODO: Translation
        mProgressPopup.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressPopup.setIndeterminate(true);
        mProgressPopup.setCancelable(false);
        mProgressPopup.setProgress(0);

        mProgressPopup.show();
    }

    private void hideProgressPopup() {
        if (null != mProgressPopup) {
            mProgressPopup.dismiss();
            mProgressPopup = null;
        }
    }

    public void onSessionXfrCommentary(int curr, int total, byte cat, byte opMode) {
        if (null == mPrettyCatStrings) return;
        mLastSessionXfrComment = "" + mPrettyCatStrings.get(cat) + " " + curr + " / " + total;
    }

    public void onSessionPrepCommentary(int curr, int total, byte cat, byte opMode) {
        if (null == mPrettyCatStrings) return;

        String opModeStr = "";

        switch (opMode) {
            case SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS:
                opModeStr = " in phone ";
                break;
            case SessionCommentary.OPMODE_PROCESSING_MEEM_ITEMS:
                opModeStr = " in meem ";
                break;
            default:
                break;
        }

        mLastSessionPrepComment = mPrettyCatStrings.get(cat) + opModeStr + curr + " / " + total;
    }

    public void shoeBackupTV(boolean isVisible){
        if(isVisible)
            mBackupTV.setVisibility(View.VISIBLE);
        else
            mBackupTV.setVisibility(View.GONE);
    }

    public void  setBackupEventListner(ApplistBackupListener listner){
        mListenerApp =listner;
    }

    public interface ApplistBackupListener{
        void  onBackupTVClickListner();
    }

    public interface ToolBarItemClickListener {
        void onEditOrCancelClickListner(String text);

        void onSaveClickListner();

        void onShareIconClickListener();

        void onRestoreIconListener();

        void onSelectIconClickListener();

        void onBackButtonPressed();

        void onDeleteIconClickListener();
    }

    private class JsonToSqlAsync extends AsyncTask<Void, Void, Boolean> {

        private SmartDataSpec mSmartDataSpec;
        private Context appCtxt = getApplicationContext();
        private ResponseCallback mResponseCallback;

        private Contacts mCon;
        private Calenders mCal;
        private Messages mMsg;

        private boolean res = false;

        public JsonToSqlAsync(SmartDataSpec smartDataSpec, ResponseCallback responseCallback) {
            mSmartDataSpec = smartDataSpec;
            mResponseCallback = responseCallback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mDbg.trace();

            switch (mSmartDataSpec.mCatCode) {
                case MMPConstants.MMP_CATCODE_CONTACT:
                    mCon = new Contacts(appCtxt, mSmartDataSpec.mMirrorPath, mSmartDataSpec.mPlusPath, mSmartDataSpec.mUpid);
                    mDbg.trace(" " + mSmartDataSpec.mMirrorPath);
                    res = mCon.parseNAddToDb();
                    break;
                case MMPConstants.MMP_CATCODE_CALENDER:
                    mCal = new Calenders(appCtxt, mSmartDataSpec.mMirrorPath, mSmartDataSpec.mPlusPath, mSmartDataSpec.mUpid);
                    res = mCal.parseNAddToDb();
                    break;
                case MMPConstants.MMP_CATCODE_MESSAGE:
                    mMsg = new Messages(appCtxt, mSmartDataSpec.mMirrorPath, mSmartDataSpec.mPlusPath, mSmartDataSpec.mUpid);
                    res = mMsg.parseNAddToDb();
                    break;
                default:
                    mDbg.trace("In-Valid Category Code");

            }
            return res;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mDbg.trace();

            int indx = smartDataAsyncTaskList.indexOf(this);
            smartDataAsyncTaskList.remove(indx);
            if (smartDataAsyncTaskList.isEmpty()) {
                mDbg.trace("Response CallBack Res" + result);
                MeemEvent event = new MeemEvent();
                event.setResponseCallback(mResponseCallback);
                event.setResult(result);
                mEventHandler.handleEvent(event);
//                mResponseCallback.execute(result, null, null);
            }
        }


        public SmartDataSpec getSmartDataSpec() {
            return mSmartDataSpec;
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
    }

    // =========================================================================================
    // --------------- MNet Service bind, unbind etc (unbind is called from ondestroy too).
    // =========================================================================================
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDbg.trace("Service connected");
            mNetService = ((MeemNetService.LocalBinder) iBinder).getInstance();
            mNetService.setOnServiceListener(MainActivity.this, mMobilePhone.getUpid());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDbg.trace("Service discconnected");
            mNetService = null;
        }
    };

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, MeemNetService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBoundToMNetService = true;
    }

    private void doUnbindService() {
        if (!mIsBoundToMNetService) {
            return;
        }
        // Detach our existing connection.
        unbindService(mServiceConnection);
        mIsBoundToMNetService = false;
    }

    /**
     * Starts meem network server: shall be called only is cable is physically connected.
     */
    public void startMeemNetServer() {
        mDbg.trace();

        if (mCableDriver.isRemoteCable()) {
            mDbg.trace("Warning: Cable is remote. Won't start network service now.");
            return;
        }

        mDbg.trace("Starting network service and binding to it");
        startService(new Intent(this, MeemNetService.class));
        doBindService();

        showToast("Meem Network started");
    }

    /**
     * Stops the service and disconnects all clients (sends disconnect message to connected clients).
     * Shall be called only if cable is physically connected.
     */
    public void stopMeemNetServer() {
        mDbg.trace();

        if(!isNetworkFeatureEnabled()) {
            return;
        }

        GenUtils.enableNetworkOnMainThreadPolicy();

        if (mCableDriver != null && mCableDriver.isRemoteCable()) {
            // remove wifi icons from all meem ui
            mHomeFragment.onRemoteClientQuit(null);

            mDbg.trace("Warning: Cable is remote. Won't touch network service now.");
            return;
        }

        if (mIsBoundToMNetService) {
            showToast("Stopping Meem Network...");
        } else {
            // Actually we can return from here. Not doing it just to make sure that service is stopped (see below).
            // User might have killed the app or some other condition may arise where we does not get a chance to stop it.
            // Remember, network service is power hungry.
        }

        mDbg.trace("Unbinding from network service and stopping it");
        doUnbindService();
        stopService(new Intent(this, MeemNetService.class));

        mDbg.trace("Disconnecting all clients.");
        for (MNetClientHandler client : mConnectedClients) {
            client.stopAndClose();
        }

        mConnectedClients.clear();
    }

    // --------------------------------------------------------------------------------------------
    // Important: Will be called from server thread, running in service. just bring it to ui thread
    // --------------------------------------------------------------------------------------------
    @Override
    public boolean onMNetSvcDiscoveryBCastReceived(final String senderIp, final String message) {
        mDbg.trace("New broadcast from: " + senderIp + ", message: " + message);
        mUiCtxt.postEvent(new MeemEvent(EventCode.MNET_DISCOVERY_BCAST, senderIp, message, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return onMNetDiscoveryBcast((String) senderIp, (String) message);
            }
        }));

        return true;
    }

    // --------------------------------------------------------------------------------------------
    // Important: Will be called from server thread, running in service. just bring it to ui thread
    // --------------------------------------------------------------------------------------------
    @Override
    public boolean MNetSvcTCPConnection(final SSLSocket clientSocket) {
        mDbg.trace("New connection from client: " + clientSocket.getRemoteSocketAddress());

        mUiCtxt.postEvent(new MeemEvent(EventCode.MNET_CONNECT_REQUEST, clientSocket, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                return onMNetConnectRequest((SSLSocket) info);
            }
        }));

        return true;
    }

    // =========================================================================================
    // --------------- MNet Service related handling, master side
    // =========================================================================================
    private boolean onMNetDiscoveryBcast(String senderIp, String message) {
        mDbg.trace();
        // Will be responded by service according as our return value.
        return true;
    }

    private boolean onMNetConnectRequest(SSLSocket clientSocket) {
        mDbg.trace();

        String hostTag = clientSocket.getRemoteSocketAddress().toString();

        for (MNetClientHandler client : mConnectedClients) {
            if (client.getTag().equals(hostTag)) {
                mDbg.trace("Host is already connected");
                return false;
            }
        }

        showToast("New network connection to Meem");

        MNetClientHandler clientHandler = new MNetClientHandler(clientSocket);
        clientHandler.start();

        mConnectedClients.add(clientHandler);
        return true;
    }

    public void onCableAcquireRequest(String upid) {
        mHomeFragment.onCableAcquireRequest(upid);
    }

    public void onCableReleaseRequest(String upid) {
        mHomeFragment.onCableReleaseRequest(upid);
    }

    public void onRemoteClientStart(String upid) {
        mDbg.trace();
        mHomeFragment.onRemoteClientStart(upid);
    }

    public void onRemoteClientQuit(String upid) {
        mDbg.trace("Remote client quits: " + upid);
        mHomeFragment.onRemoteClientQuit(upid);
    }

    /**
     * upid can be null - if app is in net client mode.
     *
     * @param upid
     */
    public void onStartAutoCopyNotification(final String upid) {
        mDbg.trace();
        mDbg.trace("Ignoring the auto copy notification (this message is for desktop): " + upid);
    }

    /**
     * upid can be null - if app is in net client mode.
     *
     * @param upid
     */
    public void onUiRefreshNotification(final String upid) {
        mDbg.trace();

        mProgressPopup = CustomDialogProgress.ctor(this);
        showSimpleProgressBar(0, mProgressPopup, "");

        refreshAllRequiredDatabasesAndRefreshViewModel(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mProgressPopup.dismiss();

                if (!result) {
                    mDbg.trace("Failed to refresh databases and ui (upon refresh ui client notification)");
                    return result;
                }

                // very important to call this to update the view model
                // in home fragment and details fragment.
                CableInfo cableInfo = getFreshCableInfo();
                mHomeFragment.update(mPhoneInfo, cableInfo);
                if(mDetailsFragment != null && mDetailsFragment.isAdded() && !mDetailsFragment.isDetached()) {
                    // conditions are important - because it may be in detached state and that will end up in IllegalStateException.
                    mDetailsFragment.onNewObject(cableInfo);
                    mDetailsFragmentRefreshPending = false;
                } else {
                    mDbg.trace("Marking a pending update for DetailsFragment (it is not attached now)");
                    mDetailsFragmentRefreshPending = true;
                }

                if (mCableDriver.isRemoteCable()) {
                    mDbg.trace("Meem network client, won't relay UI refresh message");
                } else {
                    mDbg.trace("Meem network master, relaying UI refresh notification from: " + upid);
                    for (MNetClientHandler client : mConnectedClients) {
                        if (!client.getUpid().equals(upid)) {
                            client.sendUiRefreshMsg(upid);
                        }
                    }
                }

                return true;
            }
        });
    }

    // Arun: 18Sept2017: See comments in DetailsFragment.java
    public boolean isDetailsFragmentRefreshPending() {
        mDbg.trace();
        boolean result = mDetailsFragmentRefreshPending;
        mDetailsFragmentRefreshPending = false;
        return result;
    }

    @Override
    public void sendUiRefreshMessageToNetClients() {
        mDbg.trace();

        mDbg.trace("Upon driver request, sending UI refresh notification to all clients");
        for (MNetClientHandler client : mConnectedClients) {
            client.sendUiRefreshMsg(mMobilePhone.getUpid());
        }
    }

    public void OnCableAcquireReqFromDesktop() {
        mDbg.trace();
        setAppTitle("Transferring data to desktop...");
    }

    public void OnCableReleaseReqFromDesktop() {
        mDbg.trace();
        setAppTitle(getString(R.string.app_name));
    }
}
