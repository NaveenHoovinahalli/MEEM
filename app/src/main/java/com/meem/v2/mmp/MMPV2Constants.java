package com.meem.v2.mmp;

public class MMPV2Constants {
    public static final int MMP_HEADER_MAGIC = 0x6d65656d;
    public static final int MMP_INIT_SEQ = 0x3EE33EE3;

    /**
     * The reverse of the above - if the fw is already up and running. Scenario:
     * App killed when cable is connected.
     */
    public static final int MMP_MAGIC_2 = 0x3EE33EE3;
    public static final int MMP_INIT_SEQ_2 = 0x6D65656D;

    public static final byte[] MMP_INIT_SEQ_MAGIC_BYTES = {0x3E, (byte) 0xE3, 0x3E, (byte) 0xE3, 0x6D, 0x65, 0x65, 0x6D};
    public static final byte[] MMP_FW_VERSION_PLACEHOLDER_BYTES = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final int MMP_PLATFORM_CODE_ANDROID = 0xAAAA3EE3;
    public static final int MMP_FULL_SPEED_DEVICE_CODE = 0x00000000;
    public static final int BLACKLISTED_PHONE_SLOWSPEED = 0x736C6F77;

    public static final int INIT_SEQ_REMIAN_LEN_OFFSET = 16;
    public static final int INIT_SEQ_FW_VER_OFFSET = 36;
    public static final int INIT_SEQ_PIN_STATUS_OFFSET = 43;

    public static final byte INIT_SEQ_PIN_STATUS_IS_SET = 0x10;
    public static final byte INIT_SEQ_PIN_STATUS_NOT_SET = (byte) 0xF0;
    public static final byte INIT_SEQ_PIN_STATUS_LOCKED = (byte) 0xE0;

    public static final byte MMP_HDR_TYPE_CODE_COMMAND = 0x01;
    public static final byte MMP_HDR_TYPE_CODE_RESPONSE_SUCCESS = 0x04;
    public static final byte MMP_HDR_TYPE_CODE_RESPONSE_FAILED = 0x0F;

    public static final int MMP_CODE_SET_PIN = 0x003ECCD3;
    public static final int MMP_CODE_PIN_AUTH = 0x003ECCC7;
    /*public static final int MMP_CODE_CREATE_VAULT = 0x003ECCE9;
    public static final int MMP_CODE_SET_VCFG = 0x003ECCD9;*/
    public static final int MMP_CODE_DELETE = 0x003ECCC5;
    public static final int MMP_CODE_CHANGE_MODE = 0x003ECCCB;
    public static final int MMP_CODE_APP_QUIT = 0x003ECCCD;
    public static final int MMP_CODE_CABLE_CLEANUP = 0x003ECCC9;
    public static final int MMP_CODE_REBOOT_CABLE = 0x003ECCCF;


    public static final byte MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM = (byte) 0xE1;
    public static final byte MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS = (byte) 0xE3;

    public static final byte MMP_CODE_DELETE_PARAM_TYPE_RESET = (byte) 0xD1;
    public static final byte MMP_CODE_DELETE_PARAM_TYPE_VAULT = (byte) 0xD2;
    public static final byte MMP_CODE_DELETE_PARAM_TYPE_CATEGORY = (byte) 0xD3;
    public static final byte MMP_CODE_DELETE_PARAM_TYPE_FILE = (byte) 0xD4;

    public static final int MMP_CODE_INTERNAL_CABLE_INIT = 0x11000011;
    public static final int MMP_CODE_INTERNAL_CABLE_EXCL_ACCESS = 0x11000012;

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

    public static final byte MMP_CATCODE_GENERICDATA_BASE = (byte) 0xC1; // -63
    public static final byte MMP_CATCODE_PHOTO = (byte) (MMP_CATCODE_GENERICDATA_BASE + 0); // -63
    public static final byte MMP_CATCODE_VIDEO = (byte) (MMP_CATCODE_GENERICDATA_BASE + 1); // -62
    public static final byte MMP_CATCODE_MUSIC = (byte) (MMP_CATCODE_GENERICDATA_BASE + 2); // -61
    public static final byte MMP_CATCODE_FILE = (byte) (MMP_CATCODE_GENERICDATA_BASE + 3); // -60
    public static final byte MMP_CATCODE_PHOTO_CAM = (byte) (MMP_CATCODE_GENERICDATA_BASE + 4); // -59
    public static final byte MMP_CATCODE_VIDEO_CAM = (byte) (MMP_CATCODE_GENERICDATA_BASE + 5); // -58
    public static final byte MMP_CATCODE_DOCUMENTS = (byte) (MMP_CATCODE_GENERICDATA_BASE + 6); // -57
    public static final byte MMP_CATCODE_DOCUMENTS_SD = (byte) (MMP_CATCODE_GENERICDATA_BASE + 7); // -56
    public static final byte MMP_CATCODE_GENERICDATA_MAX = (byte) (MMP_CATCODE_GENERICDATA_BASE + 7);

    // File transfer related
    public static final int MMP_FILE_XFR_PKT_HEADER_SIZE = 0;
    public static final int MMP_FILE_XFR_FNVHASH_LEN = 32;

    public static final byte MMP_FILE_SEND_XFR_REQUEST = (byte) 0xF0;
    public static final byte MMP_FILE_RECV_XFR_REQUEST = (byte) 0xF1;
    public static final byte MMP_FILE_SEND_XFR_READY = (byte) 0xF2;
    public static final byte MMP_FILE_RECV_XFR_READY = (byte) 0xF3;
    /*public static final byte MMP_FILE_DATA = (byte) 0xB4;*/
    public static final byte MMP_FILE_XFR_SUCCESS = (byte) 0xF5;
    public static final byte MMP_FILE_XFR_ERROR = (byte) 0xF6;

    public static final byte MMP_FILE_XFR_ABORT = (byte) 0xF7;

    public static final byte MMP_FILE_XFR_CONTINUE = (byte) 0xF8;
    public static final byte MMP_FILE_XFR_ABORTED = (byte) 0xF9; // by fw
    public static final int MMP_FILE_XFR_STATUS_INTERVAL = 255;

    public static final byte MMP_FILE_XFR_TYPE_CONFIG_DB_FROM_MEEM = (byte) 0xD1;
    public static final byte MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_CREATE_VAULT = (byte) 0xD7;
    public static final byte MMP_FILE_XFR_TYPE_CONFIG_DB_TO_MEEM_SET_VCFG = (byte) 0xD9;

    public static final byte MMP_FILE_XFR_TYPE_SECURE_DB_FROM_MEEM = (byte) 0xD2;
    public static final byte MMP_FILE_XFR_TYPE_SECURE_DB_TO_MEEM = (byte) 0xD5;

    public static final byte MMP_FILE_XFR_TYPE_LOGFILE_FROM_MEEM = (byte) 0xD3;
    public static final byte MMP_FILE_XFR_TYPE_FW_BINARY = (byte) 0xD6;

    public static final byte MMP_FILE_XFR_TYPE_DATA = (byte) 0xD4;

    public static final int MMP_XFR_FILE_BUFFERED_STREAM_READ_SIZE_MULTIPLIER = 8;
}
