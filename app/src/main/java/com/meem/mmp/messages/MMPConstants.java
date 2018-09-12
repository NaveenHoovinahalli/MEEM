package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPConstants {
    public static final int MMP_LEGACY_PKT_SIZE = 256;
    public static final int MMP_FULLSPEED_PKT_SIZE = 2048;

    public static final int MMP_MAGIC = 0x6D65656D;
    public static final int MMP_INIT_SEQ = 0x3EE33EE3;
    /**
     * The reverse of the above - if the fw is already up and running. Scenario: App killed when cable is connected.
     */
    public static final int MMP_MAGIC_2 = 0x3EE33EE3;
    public static final int MMP_INIT_SEQ_2 = 0x6D65656D;

    // Added on 28Nov2016, USB packet size is not unde FW control anymore. (From v258 onwards)
    public static final int BLACKLISTED_PHONE_SLOWSPEED = 0x736C6F77;

    public static final short MMP_MAJOR_REV = 1;
    public static final byte MMP_MINOR_REV = (byte) 0x02;
    public static final byte MMP_RESERVED = (byte) 0x00;
    public static final int MMP_HDR_MAGIC_POS = 0;
    public static final int MMP_HDR_REV_POS = 4;
    public static final int MMP_HDR_LEN_POS = 8;
    public static final int MMP_HDR_SEQ_POS = 12;
    public static final int MMP_HDR_FLAGS_POS = 16;
    public static final int MMP_HDR_CODE_POS = 17;
    public static final int MMP_HDR_BODY_POS = 20;
    public static final int MMP_CTRL_MSG_HEADER_LEN = 20; // WTF...
    // Arun: specification change on 19Aug2014
    public static final int MMP_XFR_REQ_SINGLE_PART_HEADER_LEN = 45;
    public static final int MMP_XFR_REQ_MULTI_PART_HEADER_LEN = 5;
    public static final byte MMP_TYPE_CMD = (byte) 0x01;
    public static final byte MMP_TYPE_ACK = (byte) 0x02;
    public static final byte MMP_TYPE_NCK = (byte) 0x03;
    public static final byte MMP_TYPE_RES = (byte) 0x04;
    public static final byte MMP_TYPE_ERR = (byte) 0x0F;
    public static final byte XFR_REQUEST = (byte) 0xBD;
    public static final byte XFR_READY = (byte) 0xBE;
    public static final byte XFR_FILEDATA = (byte) 0xBB;
    public static final byte XFR_ERROR = (byte) 0xBF;
    public static final byte XFR_ABORT = (byte) 0xB0;
    public static final byte XFR_ABORTED = (byte) 0xB1;
    // after whole file XFR
    public static final byte XFR_SUCCESS = (byte) 0xB2;
    public static final byte XFR_FAILURE = (byte) 0xB3;
    public static final int MMP_CODE_SET_TIME = 0x003ECCD1;
    public static final int MMP_CODE_GET_TIME = 0x003ECCD2;
    public static final int MMP_CODE_SET_PASSWD = 0x003ECCD3;
    public static final int MMP_CODE_GET_PASSWD = 0x003ECCD4;
    public static final int MMP_CODE_PHONE_ID = 0x003ECCD5;
    public static final int MMP_CODE_GET_MSTAT = 0x003ECCD6;
    public static final int MMP_CODE_GET_VSTAT = 0x003ECCD7;
    public static final int MMP_CODE_SET_MCFG = 0x003ECCD8;
    public static final int MMP_CODE_SET_VCFG = 0x003ECCD9;
    public static final int MMP_CODE_GET_RANDOM_SEED = 0x003ECCDA;
    public static final int MMP_CODE_CREATE_BACKUP_SESSION = 0x003ECCDB;
    public static final int MMP_CODE_CREATE_RESTORE_SESSION = 0x003ECCDC;
    public static final int MMP_CODE_CREATE_CPY_SESSION = 0x003ECCDD;
    public static final int MMP_CODE_CREATE_SYNC_SESSION = 0x003ECCDE;
    public static final int MMP_CODE_GET_DATD = 0x003ECCDF;
    public static final int MMP_CODE_GET_SMART_DATA = 0x003ECCE0;
    public static final int MMP_CODE_SET_SESD = 0x003ECCE1;
    public static final int MMP_CODE_GET_CPY_DATD = 0x003ECCE2;
    public static final int MMP_CODE_SET_CPY_SESD = 0x003ECCE3;
    public static final int MMP_CODE_GET_CPY_SMART_DATA = 0x003ECCE4;
    public static final int MMP_CODE_SESSION_STATUS = 0x003ECCE5;
    public static final int MMP_CODE_SAVE_SESSION = 0x003ECCE6;
    public static final int MMP_CODE_EXECUTE_SESSION = 0x003ECCE7;
    public static final int MMP_CODE_ABORT_SESSION = 0x003ECCE8;
    public static final int MMP_CODE_CREATE_VAULT = 0x003ECCE9;
    public static final int MMP_CODE_DESTROY_VAULT = 0x003ECCEA;
    public static final int MMP_CODE_SET_GUEST_MODE = 0x003ECCEB;
    public static final int MMP_CODE_ADD_SDCARD = 0x003ECCEC;
    public static final int MMP_CODE_DESTROY_SDCARD = 0x003ECCED;
    public static final int MMP_CODE_MODIFY_PINF = 0x003ECCEE;
    public static final int MMP_CODE_SEIZE_VAULT = 0x003ECCEF;
    public static final int MMP_CODE_COMPLETE_SESSION = 0x003ECCF0;
    public static final int MMP_CODE_RESET_CABLE = 0x003ECCF6;
    public static final int MMP_CODE_CLOSE_SESSION = 0x003ECCC5;
    public static final int MMP_CODE_GET_USER_DATA = 0x3ECCF1;
    public static final int MMP_CODE_SET_USER_DATA = 0x3ECCF2;
    public static final int MMP_CODE_SET_MEEM_INFO = 0x000082C3;
    public static final int MMP_CODE_SET_PHONE_INFO = 0x000082C4;
    public static final int MMP_CODE_GET_MEEM_INFO = 0x000082C5;
    public static final int MMP_CODE_GET_PHONE_INFO = 0x000082C6;
    public static final int MMP_CODE_UPDATE_FIRMWARE = 0x003EDDFA;
    public static final int MMP_CODE_DELETE_CATEGORY = 0x003EDDFB;
    // This is a dummy code for app to handle scenarios like restarting the app when it was
    // already connected to cable and was working. See comments is meem core.
    public static final int MMP_INTERNAL_HACK_BYPASS_CABLE_AUTH = 0x003EEEFC;
    // for firmware charging mode settings
    public static final int MMP_CODE_SET_CHARGING_MODE = 0x003EDDFD;
    // New command code introduced to handle the app restarts with cable connected.
    public static final int MMP_CODE_APP_KILL = 0x003EDDFE;
    // New command to retrieve cable serial number 09July2015
    public static final int MMP_CODE_GET_SERIAL_NUMBER = 0x003EDDFF;
    // New command for authentication
    public static final int MMP_CODE_AUTH_CABLE = 0x003ECCF7;
    public static final int MMP_CODE_GET_FW_LOG = 0x003ECCF8;
    // 12Aug2015: commands for new authentication scheme
    public static final int MMP_CODE_GET_AUTH_DETAILS = 0x003ECCC6;
    public static final int MMP_CODE_PIN_AUTH = 0x003ECCC7;
    public static final int MMP_CODE_PID_AUTH = 0x003ECCC8;
    public static final int MMP_CODE_TOGGLE_RNDIS = 0x003EDDF9;
    // 04Aug2016: Cusom codes for app
    public static final int MMP_CODE_APP_CABLE_INIT = 0xA0000001;
    public static final int MMP_CODE_APP_EXECUTE_SESSION = 0xA0000002;
    public static final int MMP_CODE_APP_GET_ALL_VAULT_STATUS = 0xA0000003;
    public static final int MMP_CODE_APP_GET_DATA_DESCRIPTOR = 0xA0000004;
    public static final int MMP_CODE_APP_PERFORM_AUTHENTICATION = 0xA0000005;
    public static final int MMP_CODE_APP_GET_SESSIONLESS_SMARTDATA = 0xA0000006;
    public static final int MMP_CODE_APP_GET_SESSIONLESS_GENERICDATA = 0xA0000007;
    public static final int MMP_CODE_APP_DELETE_GENERICDATA = 0xA0000008;

    // even newer features : 15Aug2016
    public static final int MMP_CODE_SEND_THUMBNAIL_DB = 0x3EDDF7;
    public static final int MMP_CODE_GET_THUMBNAIL_DB = 0x3EDDF8;
    public static final int MMP_CODE_GET_SINGLE_FILE = 0x3EDDF4;
    public static final int MMP_CODE_GET_SINGLE_SMART_DATA = 0x3EDDFC;
    public static final int MMP_CODE_DELETE_SINGLE_FILE = 0x3EEEF1;

    // 12Aug2015: error codes for new authentication scheme
    public static final int MMP_ERROR_PIN_MISMATCH = 0x800000C1;
    public static final int MMP_ERROR_AUTH_FAILED = 0x800000C2;
    public static final int MMP_ERROR_PID_MISMATCH = 0x800000C3;
    public static final int MMP_ERROR_CABLE_LOCKED = 0x800000C4;
    // This will never come in a production environment from DVT release of HW.
    public static final int MMP_ERROR_PID_NOT_SET = 0x800000A2;
    // error codes
    public static final int MMP_ERROR_DEVICE_UNREGISTERED = 0x800000EF;
    public static final int MMP_ERROR_FILE_XFR_ERROR = 0x800000EE;
    public static final int MMP_ERROR_FILE_NOT_FOUND = 0x800000ED;
    public static final int MMP_ERROR_GUEST_DISABLED = 0x800000EC;
    public static final int MMP_ERROR_INCOMPLETE_MESSAGE = 0x800000EB;
    public static final int MMP_ERROR_INVALID_CATEGORY = 0x800000EA;
    public static final int MMP_ERROR_INVALID_FLAGS = 0x800000E9;
    public static final int MMP_ERROR_INVALID_LAG = 0x800000E8;
    public static final int MMP_ERROR_INVALID_LANGUAGE = 0x800000E7;
    public static final int MMP_ERROR_INVALID_LENGTH = 0x800000E6;
    public static final int MMP_ERROR_INVALID_MSG_CODE = 0x800000E5;
    public static final int MMP_ERROR_INVALID_PARAM = 0x800000E4;
    public static final int MMP_ERROR_INVALID_PASSWD = 0x800000E3;
    public static final int MMP_ERROR_INVALID_PINF = 0x800000E2;
    public static final int MMP_ERROR_INVALID_PLATFORM = 0x800000E1;
    public static final int MMP_ERROR_INVALID_SESD = 0x800000E0;
    public static final int MMP_ERROR_INVALID_TIME = 0x800000DF;
    public static final int MMP_ERROR_INVALID_UMID = 0x800000DE;
    public static final int MMP_ERROR_INVALID_UPID = 0x800000DD;
    public static final int MMP_ERROR_MEMEXT_DELETE_FAILURE = 0x800000DC;
    public static final int MMP_ERROR_MIRRORPLUS_DISABLED = 0x800000DB;
    public static final int MMP_ERROR_MML_SYNTAX = 0x800000DA;
    public static final int MMP_ERROR_MMP_REVISION_MISMATCH = 0x800000D9;
    public static final int MMP_ERROR_MULTIPHONE_DISABLED = 0x800000D8;
    public static final int MMP_ERROR_INSUFFICIENT_MEMORY = 0x800000D7;
    public static final int MMP_ERROR_MULTIPHONE_STORAGE = 0x800000D6;
    public static final int MMP_ERROR_OOBP_FAILURE = 0x800000D5;
    public static final int MMP_ERROR_OOBR_FAILURE = 0x800000D4;
    public static final int MMP_ERROR_PASSWORD_UNSET = 0x800000D3;
    public static final int MMP_ERROR_PLATFORM_MISMATCH = 0x800000D2;
    public static final int MMP_ERROR_SESSION_EXECUTE_FAILURE = 0x800000D1;
    public static final int MMP_ERROR_SESSION_SAVE_FAILURE = 0x800000D0;
    public static final int MMP_ERROR_TIME_MISMATCH = 0x800000CF;
    public static final int MMP_ERROR_VAULT_LOCKED = 0x800000CE;
    // custom.
    public static final int MMP_ERROR_TIMEOUT = 0x800000CF;
    public static final int MMP_ERROR_BUGCHECK = 0x8000DEAD;
    // MMP category codes for smart data and generic data
    public static final byte MMP_CATCODE_INVALID = (byte) 0x00;
    public static final byte MMP_CATCODE_SMARTDATA_BASE = (byte) 0x51;
    public static final byte MMP_CATCODE_CONTACT = (byte) (MMP_CATCODE_SMARTDATA_BASE + 0);
    public static final byte MMP_CATCODE_MESSAGE = (byte) (MMP_CATCODE_SMARTDATA_BASE + 1);
    public static final byte MMP_CATCODE_CALENDER = (byte) (MMP_CATCODE_SMARTDATA_BASE + 2);
    public static final byte MMP_CATCODE_BOOKMARK = (byte) (MMP_CATCODE_SMARTDATA_BASE + 3);
    public static final byte MMP_CATCODE_NOTE = (byte) (MMP_CATCODE_SMARTDATA_BASE + 4);
    public static final byte MMP_CATCODE_CALL_LOG = (byte) (MMP_CATCODE_SMARTDATA_BASE + 5);
    public static final byte MMP_CATCODE_MEMO = (byte) (MMP_CATCODE_SMARTDATA_BASE + 6);
    public static final byte MMP_CATCODE_APP = (byte) (MMP_CATCODE_SMARTDATA_BASE + 7);
    public static final byte MMP_CATCODE_SETTING = (byte) (MMP_CATCODE_SMARTDATA_BASE + 8);
    public static final byte MMP_CATCODE_MAIL_ACCOUNT = (byte) (MMP_CATCODE_SMARTDATA_BASE + 9);
    public static final byte MMP_CATCODE_APP_DATA = (byte) (MMP_CATCODE_SMARTDATA_BASE + 10);
    public static final byte MMP_CATCODE_SMARTDATA_MAX = (byte) (MMP_CATCODE_SMARTDATA_BASE + 10);
    public static final byte MMP_CATCODE_GENERICDATA_BASE = (byte) 0xC1;
    public static final byte MMP_CATCODE_PHOTO = (byte) (MMP_CATCODE_GENERICDATA_BASE + 0);
    public static final byte MMP_CATCODE_VIDEO = (byte) (MMP_CATCODE_GENERICDATA_BASE + 1);
    public static final byte MMP_CATCODE_MUSIC = (byte) (MMP_CATCODE_GENERICDATA_BASE + 2);
    public static final byte MMP_CATCODE_FILE = (byte) (MMP_CATCODE_GENERICDATA_BASE + 3);
    public static final byte MMP_CATCODE_PHOTO_CAM = (byte) (MMP_CATCODE_GENERICDATA_BASE + 4);
    public static final byte MMP_CATCODE_VIDEO_CAM = (byte) (MMP_CATCODE_GENERICDATA_BASE + 5);
    public static final byte MMP_CATCODE_DOCUMENTS = (byte) (MMP_CATCODE_GENERICDATA_BASE + 6);
    public static final byte MMP_CATCODE_DOCUMENTS_SD = (byte) (MMP_CATCODE_GENERICDATA_BASE + 7);
    public static final byte MMP_CATCODE_GENERICDATA_MAX = (byte) (MMP_CATCODE_GENERICDATA_BASE + 7);
    // limits: out of spec....
    public static final int MMP_MIN_PASSWORD_LENGTH = 4;
    public static final int MMP_XFR_FILE_BUFFERED_STREAM_READ_SIZE_MULTIPLIER = 8; // See usage and comments in MeemCore.FileSender
    /**
     * Note: MMP_PKT_SIZE is not final. We need to change this upon accessory connections if platform debugging is enabled in
     * ProductSpecs.java
     */
    public static int MMP_PKT_SIZE = MMP_LEGACY_PKT_SIZE;
    public static final int MMP_MAX_PARAM_LENGTH = MMP_PKT_SIZE - MMP_CTRL_MSG_HEADER_LEN; // TODO verify.
}
