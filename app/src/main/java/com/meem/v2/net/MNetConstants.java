package com.meem.v2.net;

/**
 * Created by arun on 20/6/17.
 * List of constants used in MEEM Network iplementation.
 */

public class MNetConstants {
    public static final String NW_CLIENT_REQ = "MEEM_CLIENT_REQ.";
    public static final String NW_SERVER_RES = "MEEM_SERVER_RES.";

    public static final int NW_BCAST_RCV_PORT_MASTER = 1502; //4444;//
    public static final int NW_BCAST_RCV_PORT_CLIENT = 1503;//4445;//

    public static final int NW_TCP_PORT = 7891;//4446;//

    public static final int NW_BC_PACKET_SIZE = 512;

    public static final int NW_TCP_PACKET_HEADER_SIZE = 4;
    public static final int NW_TCP_PACKET_STANDARD_PAYLOAD_SIZE = (32 * 1024);

    public static final int NW_TCP_PACKET_SIZE = NW_TCP_PACKET_STANDARD_PAYLOAD_SIZE + NW_TCP_PACKET_HEADER_SIZE;
    public static final int NW_TCP_PACKET_ANDROID_CTRL_PAYLOAD_SIZE = NW_TCP_PACKET_STANDARD_PAYLOAD_SIZE - 1;

    // |-------0---------|----------1---------|--------2----------|--------3---------|
    // |--platform code--|----payload type ---|-------rsvd--------|------rsvd--------|
    // |-----------------|--------------------|-------------------|------------------|

    // === for payload types mnet command and response, the 4 bytes are command code
    // |-------4---------|----------5---------|--------6----------|--------7---------|
    // |--------------------------- command code ------------------------------------|
    // |-----------------|--------------------|-------------------|------------------|

    // === for payload type mnet response, response is 1 byte, the 9th byte (offset 8) as shown below

    // |-------8---------|----------9---------|--------10---------|--------11--------|
    // |--0 for success--|--------rsvd--------|-------rsvd--------|------rsvd--------|
    // |-----------------|--------------------|-------------------|------------------|

    public static final byte NW_TCP_PLATFORM_CODE_IOS = (byte) 0x15;
    public static final byte NW_TCP_PLATFORM_CODE_ANDROID = (byte) 0xAD;

    public static final byte NW_TCP_PAYLOAD_TYPE_MNET_CMD = (byte) 0x41;
    public static final byte NW_TCP_PAYLOAD_TYPE_MNET_RESP = (byte) 0x42;
    public static final byte NW_TCP_PAYLOAD_TYPE_MMP_CTRL = (byte) 0x43;
    public static final byte NW_TCP_PAYLOAD_TYPE_MMP_DATA = (byte) 0x44;

    public static final int MNET_MSG_ACQUIRE_CABLE = 0x6D6E0001;
    public static final int MNET_MSG_RELEASE_CABLE = 0x6D6E0002;
    public static final int MNET_MSG_UI_REFRESH = 0x6D6E0003;
    public static final int MNET_MSG_MEEM_DISCONNECT = 0x6D6E0004;
    public static final int MNET_MSG_CLIENT_DIED = 0x6D6E0005;
    public static final int MNET_MSG_SERVER_DIED = 0x6D6E0006;
    public static final int MNET_MSG_START_AUTO_COPY = 0x6D6E0007;

    public static final byte MNET_RESPCODE_SUCCESS = 0x00;
    public static final byte MNET_RESPCODE_ERROR = 0x0E;

    public static final byte RESERVED_8BIT = (byte) 0;
    public static final short RESERVED_16BIT = (short) 0;
    public static final int RESERVED_32BIT = (int) 0;
    public static final long RESERVED_64BIT = (long) 0;
}
