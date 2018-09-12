package com.meem.androidapp;

/**
 * @author Arun T A
 */

public class ProductSpecs {
    // limits
    public static final int LIMIT_MAX_VAULTS = 3;
    public static final int MEEM_CABLE_PASSWORD_MIN_LENGTH = 4;

    public static final int AUTOBACKUP_COUNTDOWN_MS = 21000;
    public static final int AUTOBACKUP_COUNTDOWN_TICK_MS = 1000;

    public static final int CATEGORY_WISE_OPERATIONS_COUNTDOWN_MS = 6000;
    public static final int CATEGORY_WISE_OPERATIONS_COUNTDOWN_TICK_MS = 1000;

    // whether to use media store or brute-force file system scan
    public static final boolean USE_SYSTEM_MEDIA_STORE = true;

    // Fix in toAlphaNum in firmware
    public static final int HACK_UPID_LEN_FIX = 1;
    public static final boolean MIRROR_PLUS_FEATURE_ENABLED = true;

    // Very important for V1 hw.
    public static final boolean ENABLE_GENUINE_CABLE_CHECK = true;

    // ---------------- Old style firmware update stuff ----------------------

    // Fw check interval : once all available fw versions are downloaded.
    public static final long FIRMWARE_CHECK_INTERVAL_MS = (24 * 60 * 60 * 1000);

    // WARNING: true is only used for FW update testing.
    public static final boolean FW_UPDATE_USING_LOCAL_FOLDER = false;
    public static final String FW_UPDATE_APP_LOCAL_FOLDER = "fwupdates";

    // IMPORTANT: One and only one of http and ftp these shall be true
    public static final boolean FW_UPDATE_USING_FTP = false;
    public static final boolean FW_UPDATE_USING_HTTP = true;
    public static final String FW_UPDATE_SERVER_HOSTNAME = "firmware.meemmemory.com";
    public static final String FW_UPDATE_SERVER_USERNAME = "";
    public static final String FW_UPDATE_SERVER_PASSWORD = "";
    public static final String FW_UPDATE_MANIFEST_FILE_PATH = "/android/update.mml";

    public static final long MMP_CTRL_COMMAND_TIMEOUT_MS = (60 * 1000); //ms
    public static final long MMP_ABORT_CTRL_COMMAND_TIMEOUT_MS = (120 * 1000); //ms

    // supported file formats and categories
    public static final String[] PICTURE_FORMATS = {".jpg", ".png", ".gif", ".bmp", ".jpeg", ".tiff"};
    public static final String[] AUDIO_FORMATS = {".mp3", ".mp2", ".m2a", ".m3a", ".wav", ".ra", ".ac3", ".fla", ".asf"};
    public static final String[] VIDEO_FORMATS = {".mov", ".mp4", ".m2v", ".mpg", ".mpeg", ".vob", ".qt", ".flv", ".rm", ".rmvb", ".webm", ".3gp", ".avi", ".mkv"};
    public static final String[] DOCUMENT_FORMATS = {".doc", ".docx", ".pdf", ".xls", ".xlsx", ".xlsm", ".ppt", ".pptx", ".txt", ".rtf", ".odt", ".ods", ".htm", ".html", ".epub", ".mobi"};

    // ===================================== Modern stuff =======================================

    // make this false only for development purposes.
    // if this is false, make sure housekeeping wont delete MeemAndroid folder.
    public static final boolean MEEM_APP_LOCAL_DATA_PRIVATE = true;
    public static final String MEEM_APP_LOCAL_DATA_PUBLIC_FOLDER = "MeemAndroid";
    public static final String MEEM_APP_LOCAL_DATA_PRIVATE_FOLDER = "MeemAndroid";
    public static final String PUBLIC_TEMP_DIR_NAME = ".meemtemp"; // In Downloads folder.

    // Must not be empty or null
    public static final String LOGS_DIR = "MeemAndroid/Logs";
    public static final boolean ENABLE_DEBUG_MESSAGES_FILE_LOGGING = true;
    public static final boolean ENABLE_DEBUG_METHOD_TRACING = true;
    public static final boolean ENABLE_DEBUG_MESSAGES_LOGCAT_LOGGING = true;

    public static final long MIN_FREE_STORAGE_MAINTAINED_MB = 500; // 500MB
    public static final long DEBUG_LOG_MAX_SIZE = 1024 * 1024 * 2; // 2MB;
    public static final boolean USB_BUFFER_SIZE_UNDER_FW_CONTROL = false; // Important change on 28Nov2016

    // TODO: MUST BE FALSE unless debugging XFR
    public static final boolean ENABLE_XFR_DATALOSS_DEBUG = false;
    public static final boolean INEDA_HW_XFR_DEBUGGING = false;

    public static final int XFR_CSUM_LENGTH = 32;
    public static final long XFR_TIMEOUT = 60000; // ms (1 minute)

    // Most important to verify vthis on each release.
    public static final String MIN_FW_VERSION_FOR_TI_HW = "1.1.265.0";
    public static final String BUNDLED_FW_VERSION_FOR_TI_HW = "1.1.265.0";

    public static final String MIN_FW_VERSION_FOR_INEDA_HW = "2.2.73.0";
    public static final String BUNDLED_FW_VERSION_FOR_INEDA_HW = "2.2.73.0";

    // For initial blind estimates of session duration (should never be set 0)
    public static final double NOMINAL_DATA_TRANSFER_SPEED_KBPS = 0.5 * 1024;
    public static final double NOMINAL_DATA_PROCESSING_SPEED_KBPS = 5 * 1024;
    public static final long MINIMUM_NUMBER_CONTACTS_FOR_STATS = 50;
    public static final long MINIMUM_NUMBER_MESSAGES_FOR_STATS = 50;
    public static final long MINIMUM_NUMBER_CALENDAR_EVENTS_FOR_STATS = 50;
    public static final double SINGE_CONTACT_PROCESSING_TIME_SEC = 0.5;
    public static final double SINGE_MESSAGE_PROCESSING_TIME_SEC = 0.2;
    public static final double SINGE_CALENDAR_EVENT_PROCESSING_TIME_SEC = 0.2;
    public static final double SUBSEQUENT_SINGE_CONTACT_PROCESSING_TIME_SEC = 0.05;
    public static final double SESSION_DURATION_ESTIMATION_LIMIT = (3600 * 12); // 12hrs in seconds[+ random(0, 3600) by code]
    public static final double SESSION_ESTIMATION_MINIMUM_DURATION_SECS = 3;
    public static final long SESSION_ESTIMATION_MAXIMUM_DURATION_SECS = (3600 * 12); // 12hrs [+ random(0, 3600) by code]
    public static final double SESSION_ESTIMATION_TIME_CORRECTION_SECS = 300; // 5 minutes

    // Battery level related stuff
    public static final int MINIMUM_BATTERY_LEVEL_FOR_AUTOBACKUP = 5; // percent.

    // android bugs related workarounds - we need to get the count of
    // these smart data cats as soon as we start backup - so we need to query these
    // guys from main thread itself. But android lollipop on Nexus simply locks up
    // and show ANR - especially if there are large number of SMS.
    public static final int ANDROID_SMS_COUNT_BUG_WORKAROUND = 500;
    public static final int ANDROID_CALENDAR_COUNT_BUG_WORKAROUND = 500;
    public static final int ANDROID_CONTACTS_COUNT_BUG_WORKAROUND = 500;

    // sometimes FW does not give a response to APP_QIT_NOTIFY message.. well,
    // this will force the cable to be reconnected..
    public static final long HACK_APPQUIT_TIMEOUT_MS = 1000;

    // MINI_KIND: 512 x 384, MICRO_KIND: 96 x 96 (Defined in MediaStore class)
    public static final int IMG_THUMBNAIL_SIZE = 96;
    public static final int VID_THUMBNAIL_SIZE = 96;
    public static final int THUMBNAIL_JPEG_QUALITY = 75;

    public final static String CONTACT_EMAIL = "hello@meemmemory.com";
    public static final String MEEM_WEB_SITE_URL = "http://meemmemory.com/";
    public static final boolean DUMMY_CABLE_MODE = false;
    public static final String DUMMY_CABLE_MOED_FW_VERSION = "2.2.12.0";
    public static final int HW_VERSION_1_TI = 1;
    public static final int HW_VERSION_2_INEDA = 2;
    public static final int INEDA_CABLE_SERIAL_LEN = 16;
    protected static final long WAIT_TIMEOUT_FOR_DEFAULT_SMS_APP = 60 * 1000; // ms;
    private final static int SLOWDOWN_FACTOR = 1;
    public static float ANIMATION_SPEED = 1.0f;
    public static int DEFAULT_ANIM_DURATION = (int) (1500 / ANIMATION_SPEED); // change res/integers too
    public static int TIME_VAULT_SPIRIT_RETURN_ON_INVALID_DROP = 250 * SLOWDOWN_FACTOR;
    public static int TIME_VAULT_SPIRIT_RETURN_ON_VALID_DROP = 250 * SLOWDOWN_FACTOR;

    public static final int MAX_VAULT_NAME_LEN = 12;

    // Arun: 05July2018: Added for pin recover support
    public static final byte PIN_RECOVERY_NUM_QUESTIONS = 3;

    // Arun: 20Aug2018: Cable Disconnect in between DeleteVault may cause orphan entries in DB in cable.
    // Check for it - Added this flagging in FW by Barath - check mail on same date.
    public static final int FIRMWARE_FLAG_DB_CORRUPT_ON_DELETEVAULT = 0xDEADDBDB;
    public static final int MAX_UPID_LEN = 1024;
}
