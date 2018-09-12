package com.meem.v2.net;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.v2.core.MeemCoreV2;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.net.ssl.SSLSocket;

/**
 * Created by arun on 23/6/17.
 * <p>
 * This class will have one thread to read/process from the client socket. It will use the current driver instance and thus in tern current
 * meem core v2 (singleton) and the meem core handler therein. Other methods are invoked in ui thread context (usual meem event pattern).
 * <p>
 * You must understand and remember the above facts when you are modifying any code here!
 */

public class MNetClientHandler {
    private static final String TAG = "MNetClientHandler";
    private String mClientTag;

    DebugTracer mDbg = new DebugTracer(TAG, "MNetClientHandler.log");

    private SSLSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private Thread mReaderThread;
    private boolean mStopFlag;

    private String mClientUpid;

    public MNetClientHandler(SSLSocket clientSocket) {
        mSocket = clientSocket;
        mClientTag = mSocket.getRemoteSocketAddress().toString();
    }

    public String getTag() {
        return mClientTag;
    }

    public String getUpid() {
        return mClientUpid;
    }

    public void start() {
        mDbg.trace();
        startMNetReaderThread();
    }

    public boolean stopAndClose() {
        mDbg.trace();
        boolean result = true;

        if (mSocket == null) {
            return result;
        }

        sendDisconnectedMsg();

        mStopFlag = true;

        try {
            mSocket.close();
        } catch (Exception e) {
            mDbg.trace("Socket close exception: " + GenUtils.getStackTrace(e));
            result = false;
        }

        return result;
    }

    public void sendStartAutoCopyMsg(String upid) {
        mDbg.trace();
        sendSimpleMNetMsg(MNetConstants.MNET_MSG_START_AUTO_COPY, upid);
    }

    public void sendUiRefreshMsg(String upid) {
        mDbg.trace();
        sendSimpleMNetMsg(MNetConstants.MNET_MSG_UI_REFRESH, upid);
    }

    private void startMNetReaderThread() {
        mDbg.trace();

        mReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mInputStream = mSocket.getInputStream();
                    mOutputStream = mSocket.getOutputStream();
                } catch (Exception e) {
                    mDbg.trace("ERROR! Could not get streams from socket: " + e.getMessage());
                    return;
                }

                final int packetSize = MNetConstants.NW_TCP_PACKET_SIZE;
                ByteBuffer packet = ByteBuffer.allocate(packetSize);
                packet.order(ByteOrder.BIG_ENDIAN);

                int readCount = 0, packetFree = packetSize;
                byte[] readBuffer = new byte[MNetConstants.NW_TCP_PACKET_SIZE];
                while (!mStopFlag) {
                    try {
                        readCount = mInputStream.read(readBuffer, 0, packetFree);
                        if (readCount == -1) {
                            mDbg.trace("Socket read returns eos: -1");
                            break;
                        }

                        packet.put(readBuffer, 0, readCount);

                        if (packet.position() == MNetConstants.NW_TCP_PACKET_SIZE) {
                            packet.rewind();
                            onMNetPacket(packet);

                            packet.clear();
                            packetFree = packetSize;
                        } else {
                            packetFree = packetSize - packet.position();
                        }
                    } catch (Exception e) {
                        // it happens with ios - exception with null message
                        mDbg.trace("Exception in read loop: " + GenUtils.getStackTrace(e) + "\nRead count: " + readCount + "\nBB Position: " + packet.position());
                        break;
                    }
                }

                mDbg.trace("MNetClientHandler: reader thread quits (will try to release cable to be clean)");
                MeemCoreV2 coreV2 = MeemCoreV2.getInstance();
                coreV2.releaseCable(mClientTag);
            }
        });

        mReaderThread.start();
    }

    private void onMNetPacket(ByteBuffer packet) {
        //mDbg.trace();

        byte platformCode = packet.get(0);
        byte payloadType = packet.get(1);

        int payloadLength = MNetConstants.NW_TCP_PACKET_STANDARD_PAYLOAD_SIZE;
        if (platformCode == MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID && payloadType == MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_CTRL) {
            payloadLength = MNetConstants.NW_TCP_PACKET_ANDROID_CTRL_PAYLOAD_SIZE;
        }

        //mDbg.trace("plat: " + platformCode + ", pay: " + payloadType + ", len: " + payloadLength);

        int mnetCode = 0;
        switch (payloadType) {
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_CMD:
                mnetCode = packet.getInt(4);
                boolean result = onMNetCommand(platformCode, mnetCode, packet);
                sendMNetResponse(packet, result);
                break;
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_RESP:
                mDbg.trace("BUG: MNET response should not reach master!");
                break;
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_CTRL:
                onMNetMMPControl(platformCode, packet);
                break;
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_DATA:
                onMNetXFRData(platformCode, packet);
                break;
            default:
                mDbg.trace("Unknown payload type: " + payloadType);
                break;
        }
    }

    /**
     * This call may end up waiting for a loooog time!
     *
     * @param platformCode ignored
     * @param mnetCode     which command
     * @param packet       the packet
     *
     * @return success or error
     */
    private boolean onMNetCommand(byte platformCode, int mnetCode, ByteBuffer packet) {
        mDbg.trace();

        boolean result = true;

        if (mnetCode == MNetConstants.MNET_MSG_ACQUIRE_CABLE) {
            if(!notifyUiThread(EventCode.MNET_CABLE_ACQUIRE_REQUEST, packet)) {
                // This is hack!
                MeemEvent event = new MeemEvent(EventCode.MNET_CABLE_ACQUIRE_REQUEST_FROM_DESKTOP);
                UiContext.getInstance().postEvent(event);
            }

            MeemCoreV2 coreV2 = MeemCoreV2.getInstance();
            result = coreV2.acquireCable(mClientTag, new MeemCoreV2.MeemNetworkInterface() {
                @Override
                public boolean onCtrlMessage(ByteBuffer messasge) {
                    boolean res = writeMmpCtrlDataToNetwork(messasge, "ctrlToRemote");
                    if (!res) {
                        mDbg.trace("Writing ctrl msg to socket failed!");
                    }
                    return res;
                }


                @Override
                public boolean onDataPacket(ByteBuffer packet) {
                    boolean res = writeMmpXfrDataToNetwork(packet, "xfrToRemote");
                    if (!res) {
                        mDbg.trace("Writing xfr packet to socket failed!");
                    }
                    return res;
                }
            });
        } else if (mnetCode == MNetConstants.MNET_MSG_RELEASE_CABLE) {
            if(!notifyUiThread(EventCode.MNET_CABLE_RELEASE_REQUEST, packet)) {
                // This is hack!
                MeemEvent event = new MeemEvent(EventCode.MNET_CABLE_RELEASE_REQUEST_FROM_DESKTOP);
                UiContext.getInstance().postEvent(event);
            }

            MeemCoreV2 coreV2 = MeemCoreV2.getInstance();
            result = coreV2.releaseCable(mClientTag);
        } else if (mnetCode == MNetConstants.MNET_MSG_CLIENT_DIED) {
            mDbg.trace("Death notice from client!");

            mStopFlag = true;
            mReaderThread.interrupt();

            MeemEvent event = new MeemEvent(EventCode.MNET_REMOTE_CLIENT_HANDLER_QUITS, mClientUpid);
            UiContext.getInstance().postEvent(event);
        } else if (mnetCode == MNetConstants.MNET_MSG_START_AUTO_COPY) {
            mDbg.trace("Backup complete message from client");

            MeemEvent event = new MeemEvent(EventCode.MNET_START_AUTO_COPY, mClientUpid);
            UiContext.getInstance().postEvent(event);
        } else if (mnetCode == MNetConstants.MNET_MSG_UI_REFRESH) {
            mDbg.trace("UI Refresh message from client");

            MeemEvent event = new MeemEvent(EventCode.MNET_REMOTE_CLIENT_UI_REFRESH, mClientUpid);
            UiContext.getInstance().postEvent(event);
        } else {
            mDbg.trace("WARN: Unknown message from client: " + Integer.toHexString(mnetCode));
        }

        return result;
    }

    private boolean notifyUiThread(EventCode eventCode, ByteBuffer packet) {
        byte len = packet.get(8);
        if (len != ((byte) 0xFF)) {
            byte[] upidArr = new byte[len];
            packet.position(9);
            packet.get(upidArr, 0, len);

            String upid = new String(upidArr);

            MeemEvent acquireEvent = new MeemEvent(eventCode, upid);
            UiContext.getInstance().postEvent(acquireEvent);

            if (null == mClientUpid) {
                mClientUpid = upid;

                MeemEvent event = new MeemEvent(EventCode.MNET_REMOTE_CLIENT_HANDLER_STARTS, mClientUpid);
                UiContext.getInstance().postEvent(event);
            }

            packet.rewind();

            return true;
        } else {
            // Hack for desktop.
            // mDbg.trace("Invalid upid length");
            return false;
        }
    }

    private boolean onMNetMMPControl(byte platformCode, ByteBuffer packet) {
        mDbg.trace();

        boolean result = false;
        MeemCoreV2 coreV2 = MeemCoreV2.getInstance();

        // Convert net to usb
        ByteBuffer usbBb = netToUsb(coreV2, platformCode, packet);
        result = coreV2.writeUsbCtrlData(usbBb, this.toString());

        return result;
    }

    private boolean onMNetXFRData(byte platformCode, ByteBuffer packet) {
        //mDbg.trace();

        boolean result;
        MeemCoreV2 coreV2 = MeemCoreV2.getInstance();

        // Convert net to usb
        ByteBuffer usbBb = netToUsb(coreV2, platformCode, packet);
        result = coreV2.writeUsbXfrData(usbBb, this.toString());

        return result;
    }

    private boolean sendMNetResponse(ByteBuffer packet, boolean result) {
        mDbg.trace();

        packet.put(1, MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_RESP);
        packet.put(8, (byte) (result ? 0 : 0x0E));

        try {
            mOutputStream.write(packet.array());
        } catch (Exception e) {
            mDbg.trace("Exception while writing response to socket: " + e.getMessage());
            return false;
        }

        return true;
    }

    // -----------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------
    private ByteBuffer netToUsb(MeemCoreV2 coreV2, byte platformCode, ByteBuffer netPacket) {
        int usbPayloadSize = coreV2.getPayloadSize();
        ByteBuffer usbBb = ByteBuffer.allocate(usbPayloadSize);

        usbBb.put(netPacket.array(), MNetConstants.NW_TCP_PACKET_HEADER_SIZE, usbPayloadSize);

        return usbBb;
    }

    private boolean writeMmpCtrlDataToNetwork(ByteBuffer bb, String desc) {
        mDbg.trace();

        ByteBuffer netPkt = ByteBuffer.allocate(MNetConstants.NW_TCP_PACKET_SIZE);
        netPkt.order(ByteOrder.BIG_ENDIAN);

        netPkt.put(MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID);
        netPkt.put(MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_CTRL);
        netPkt.put(MNetConstants.RESERVED_8BIT);
        netPkt.put(MNetConstants.RESERVED_8BIT);

        bb.rewind(); // important.
        netPkt.put(bb);

        try {
            mOutputStream.write(netPkt.array());
        } catch (Exception e) {
            mDbg.trace("Exception during mmp ctrl network write: " + e.getMessage());
            return false;
        }

        return true;
    }

    private boolean writeMmpXfrDataToNetwork(ByteBuffer bb, String desc) {
        ByteBuffer netPkt = ByteBuffer.allocate(MNetConstants.NW_TCP_PACKET_SIZE);
        netPkt.order(ByteOrder.BIG_ENDIAN);

        netPkt.put(MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID);
        netPkt.put(MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_DATA);
        netPkt.put(MNetConstants.RESERVED_8BIT);
        netPkt.put(MNetConstants.RESERVED_8BIT);

        bb.rewind(); // important.
        netPkt.put(bb);

        try {
            mOutputStream.write(netPkt.array());
        } catch (Exception e) {
            mDbg.trace("Exception during mmp xfr network write: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void sendDisconnectedMsg() {
        mDbg.trace();
        sendSimpleMNetMsg(MNetConstants.MNET_MSG_MEEM_DISCONNECT, null);
    }

    private void sendSimpleMNetMsg(int msgCode, String upid) {
        mDbg.trace();

        ByteBuffer netPkt = ByteBuffer.allocate(MNetConstants.NW_TCP_PACKET_SIZE);
        netPkt.order(ByteOrder.BIG_ENDIAN);

        netPkt.put(MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID);
        netPkt.put(MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_CMD);
        netPkt.put(MNetConstants.RESERVED_8BIT);
        netPkt.put(MNetConstants.RESERVED_8BIT);

        netPkt.putInt(msgCode);

        if(upid != null) {
            netPkt.put((byte) upid.getBytes().length);
            netPkt.put(upid.getBytes());
        }

        try {
            mOutputStream.write(netPkt.array());
        } catch (Exception e) {
            mDbg.trace("sendSimpleMNetMsg: Exception on network write: " + e);
        }
    }
}
