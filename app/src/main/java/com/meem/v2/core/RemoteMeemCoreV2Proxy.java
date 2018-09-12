package com.meem.v2.core;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.utils.GenUtils;
import com.meem.v2.net.MNetConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by arun on 24/7/17.
 */

public class RemoteMeemCoreV2Proxy {
    private AccessoryInterface mAccessory;
    private RemoteMeemCoreV2ProxyListener mListener;

    private InputStream mInputStream;
    private OutputStream mOutputStream;

    Thread mReaderThread;

    private Object mCableAccessMonitor;
    private boolean mCableAcquiredFlag;
    private boolean mCableAcquireResult;
    private boolean mCableReleaseResult;
    private String mCableAcquireReqTag;
    private String mCableAcquiredByTag;

    private boolean mStopFlag;

    private String mPhoneUpid;

    // This must be implemented by MeemCoreV2
    public interface RemoteMeemCoreV2ProxyListener {
        void onCompletePayloadReceivedFromRemote(byte platformCode, ByteBuffer payload);
        void onRemoteCableDisconnectOrError();
        int getUsbPayloadSize();
    }

    public RemoteMeemCoreV2Proxy(AccessoryInterface accessory, RemoteMeemCoreV2ProxyListener listener) {
        dbgTrace();

        mAccessory = accessory;
        mListener = listener;

        mInputStream = mAccessory.getInputStream();
        mOutputStream = mAccessory.getOutputStream();

        mCableAccessMonitor = new Object();

        mPhoneUpid = UiContext.getInstance().getPhoneUpid();

        mStopFlag = false;
    }

    public boolean start() {
        dbgTrace();

        mStopFlag = false;
        startReaderThread();

        return true;
    }

    public boolean stop() {
        dbgTrace();
        boolean result = true;

        mStopFlag = true;

        dbgTrace("Notifying all acquire requesters");

        synchronized (mCableAccessMonitor) {
            mCableAcquiredFlag = false;
            mCableAcquireResult = false;
            mCableReleaseResult = false;
            mCableAccessMonitor.notifyAll();
        }

        // notify the master over nwteork.
        writeMNetCtrlCmdToNetwork(MNetConstants.MNET_MSG_CLIENT_DIED);

        // cleanup network stuff
        dbgTrace("Closing streams");

        try {
            if (null != mInputStream) mInputStream.close();
        } catch (Exception e) {
            dbgTrace("Exception during input stream close: " + e.getMessage());
            result = false;
        }

        try {
            if (null != mOutputStream) mOutputStream.close();
        } catch (Exception e) {
            dbgTrace("Exception during output stream close: " + e.getMessage());
            result = false;
        }

        mStopFlag = true;
        if(null != mReaderThread) {
            mReaderThread.interrupt();
        }

        return result;
    }

    public boolean acquireRemoteCable(String tag) {
        dbgTrace();
        boolean result = false;

        synchronized (mCableAccessMonitor) {
            if (mCableAcquiredFlag && mCableAcquiredByTag != null && mCableAcquiredByTag.equals(tag)) {
                dbgTrace("Cable already acquired by same tag: " + tag);
                return result;
            } else if (mCableAcquiredFlag && !mCableAcquiredByTag.equals(tag)) {
                dbgTrace("BUG? Cable already acquired by tag: " + tag);
                return true;
            } else if (!mCableAcquiredFlag) {

                mCableAcquireReqTag = tag;
                mCableAcquireResult = false;

                if (!writeMNetCtrlCmdToNetwork(MNetConstants.MNET_MSG_ACQUIRE_CABLE)) {
                    dbgTrace("Writing acquire command to network failed.");
                    return false;
                }

                // wait for response
                while (!mCableAcquiredFlag || mStopFlag) {
                    try {
                        mCableAccessMonitor.wait();
                        result = mCableAcquireResult;
                        if (!result) break;
                    } catch (Exception e) {
                        dbgTrace("Wait for acquire interrupted: " + e.getMessage());
                    }
                }

                dbgTrace("Wait for acquire ended: result is:" + mCableAcquireResult + ", acquire state: " + mCableAcquiredFlag + ", shutdown state:  " + mStopFlag);
            }
        }

        return result;
    }

    public boolean releaseRemoteCable(String tag) {
        dbgTrace();
        boolean result = true;

        synchronized (mCableAccessMonitor) {
            if (!mCableAcquiredFlag) {
                dbgTrace("BUG? Release request while cable is not acquired");
                return result;
            }

            if (mCableAcquiredFlag && mCableAcquiredByTag != null && !mCableAcquiredByTag.equals(tag)) {
                dbgTrace("BUG? Release request while cable is not acquired by: " + tag + ", current owner: " + mCableAcquiredByTag);
                return result;
            } else {
                if (!writeMNetCtrlCmdToNetwork(MNetConstants.MNET_MSG_RELEASE_CABLE)) {
                    dbgTrace("Writing release command to network failed.");
                    return false;
                }

                mCableReleaseResult = false;

                // wait for response
                while (mCableAcquiredFlag) {
                    try {
                        mCableAccessMonitor.wait();
                        result = mCableReleaseResult;
                        if (!result) break;
                    } catch (Exception e) {
                        dbgTrace("Wait for release interrupted: " + e.getMessage());
                    }
                }

                dbgTrace("Wait for release ended: result is:" + mCableReleaseResult + ", acquire state: " + mCableAcquiredFlag + ", shutdown state:  " + mStopFlag);
            }
        }

        return result;
    }

    public boolean sendMessageToNetMaster(int msgCode) {
        dbgTrace();
        return writeMNetCtrlCmdToNetwork(msgCode);
    }

    private boolean writeMNetCtrlCmdToNetwork(int cmdCode) {
        dbgTrace();

        ByteBuffer netPacket = ByteBuffer.allocate(MNetConstants.NW_TCP_PACKET_SIZE);
        netPacket.order(ByteOrder.BIG_ENDIAN);

        netPacket.put(MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID);
        netPacket.put(MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_CMD);
        netPacket.put(MNetConstants.RESERVED_8BIT);
        netPacket.put(MNetConstants.RESERVED_8BIT);
        netPacket.putInt(cmdCode);

        if(cmdCode == MNetConstants.MNET_MSG_ACQUIRE_CABLE || cmdCode == MNetConstants.MNET_MSG_RELEASE_CABLE) {
            netPacket.put((byte) mPhoneUpid.getBytes().length);
            netPacket.put(mPhoneUpid.getBytes());
        }

        try {
            mOutputStream.write(netPacket.array());
        } catch (Exception e) {
            dbgTrace("Exception during network write: " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean writeMmpCtrlDataToNetwork(ByteBuffer bb, String desc) {
        dbgTrace();

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
            dbgTrace("Exception during mmp ctrl network write: " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean writeMmpXfrDataToNetwork(ByteBuffer bb, String desc) {
        /*dbgTrace();*/

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
            dbgTrace("Exception during mmp xfr network write: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void startReaderThread() {

        mReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final int packetSize = MNetConstants.NW_TCP_PACKET_SIZE;
                ByteBuffer packet = ByteBuffer.allocate(packetSize);
                packet.order(ByteOrder.BIG_ENDIAN);

                int readCount, packetFree = packetSize;;
                byte[] readBuffer = new byte[MNetConstants.NW_TCP_PACKET_SIZE];
                while (!mStopFlag) {
                    try {
                        readCount = mInputStream.read(readBuffer, 0, packetFree);
                        if (readCount == -1) {
                            dbgTrace("Socket read returns eos: -1");
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
                        String msg = e.getMessage();
                        dbgTrace("Socket read exception: " + ((msg == null) ? e : e.getMessage()));
                        break;
                    }
                }

                dbgTrace("reader thread quits");
                stop();

                mListener.onRemoteCableDisconnectOrError();
            }
        });

        mReaderThread.start();
    }

    private void onMNetPacket(ByteBuffer netPacket) {
        byte platformCode = netPacket.get(0);
        byte payloadType = netPacket.get(1);

        int payloadLength = MNetConstants.NW_TCP_PACKET_STANDARD_PAYLOAD_SIZE;
        if (platformCode == MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID && payloadType == MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_CTRL) {
            payloadLength = MNetConstants.NW_TCP_PACKET_ANDROID_CTRL_PAYLOAD_SIZE;
        }

        dbgTrace("plat: " + platformCode + ", pay: " + payloadType + ", len: " + payloadLength);

        int mnetCode = 0;
        ByteBuffer usbPacket;
        switch (payloadType) {
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_CMD:
                mnetCode = netPacket.getInt(4);
                if(!onMNetCommand(mnetCode)) {
                    // TODO: Warning: This isn't a network command. Offloading to core. Apparently iOS do not differentiate between packets!
                    usbPacket = netToUsb(netPacket);
                    mListener.onCompletePayloadReceivedFromRemote(platformCode, usbPacket);
                }
                break;
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MNET_RESP:
                mnetCode = netPacket.getInt(4);
                byte responseCode = netPacket.get(8);
                boolean result = onMNetResponse(mnetCode, responseCode);
                break;
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_CTRL:
            case MNetConstants.NW_TCP_PAYLOAD_TYPE_MMP_DATA:
                usbPacket = netToUsb(netPacket);
                mListener.onCompletePayloadReceivedFromRemote(platformCode, usbPacket);
                break;
            default:
                dbgTrace("Unknown payload type: " + payloadType);
                break;
        }
    }

    private ByteBuffer netToUsb(ByteBuffer netPacket) {
        int usbPayloadSize = mListener.getUsbPayloadSize();

        ByteBuffer usbPacket = ByteBuffer.allocate(usbPayloadSize);
        usbPacket.order(ByteOrder.BIG_ENDIAN);
        usbPacket.put(netPacket.array(), MNetConstants.NW_TCP_PACKET_HEADER_SIZE, usbPayloadSize);

        usbPacket.rewind();
        return usbPacket;
    }

    /**
     * Only few commands can reach slave - #1 is remote cable disconnect (OR may be server quit)
     */
    private boolean onMNetCommand(int mnetCode) {
        dbgTrace();

        boolean result = true;
        MeemEvent event = null;

        switch(mnetCode) {
            case MNetConstants.MNET_MSG_SERVER_DIED:
            case MNetConstants.MNET_MSG_MEEM_DISCONNECT:
                mStopFlag = true;
                mReaderThread.interrupt();
                mListener.onRemoteCableDisconnectOrError();
                break;
            case MNetConstants.MNET_MSG_UI_REFRESH:
                dbgTrace("UI Refresh message from someone");
                event = new MeemEvent(EventCode.MNET_REMOTE_CLIENT_UI_REFRESH, null);
                UiContext.getInstance().postEvent(event);
                break;
            case MNetConstants.MNET_MSG_START_AUTO_COPY:
                dbgTrace("Backup completed in some client");
                event = new MeemEvent(EventCode.MNET_START_AUTO_COPY, null);
                UiContext.getInstance().postEvent(event);
                break;
            default:
                /*dbgTrace("Unhandled meem net command: " + mnetCode);*/
                result = false;
                break;
        }

        return result;
    }

    private boolean onMNetResponse(int msgCode, byte respCode) {
        dbgTrace();

        switch (msgCode) {
            case MNetConstants.MNET_MSG_ACQUIRE_CABLE:
                if (respCode == MNetConstants.MNET_RESPCODE_SUCCESS) {
                    dbgTrace("Response: Cable acquire: success");
                    synchronized (mCableAccessMonitor) {
                        mCableAcquiredFlag = true;
                        mCableAcquiredByTag = mCableAcquireReqTag;
                        mCableAcquireResult = true;
                        mCableAccessMonitor.notifyAll();
                    }
                } else {
                    dbgTrace("Response: Cable acquire: failed!");
                    synchronized (mCableAccessMonitor) {
                        mCableAcquireResult = false;
                        mCableAccessMonitor.notifyAll();
                    }
                }

                break;
            case MNetConstants.MNET_MSG_RELEASE_CABLE:
                if (respCode == MNetConstants.MNET_RESPCODE_SUCCESS) {
                    dbgTrace("Response: Cable release: success");
                    synchronized (mCableAccessMonitor) {
                        mCableAcquiredFlag = false;
                        mCableAcquiredByTag = "";
                        mCableReleaseResult = true;
                        mCableAccessMonitor.notifyAll();
                    }
                } else {
                    dbgTrace("Response: Cable release: failed!");
                    synchronized (mCableAccessMonitor) {
                        mCableReleaseResult = false;
                        mCableAccessMonitor.notifyAll();
                    }
                }
                break;
            case MNetConstants.MNET_MSG_START_AUTO_COPY:
                dbgTrace("Response for backup comeplete notification message (ignored)");
                break;
            case MNetConstants.MNET_MSG_UI_REFRESH:
                dbgTrace("Response for ui refresh notification message (ignored)");
                break;
            default:
                dbgTrace("WARNING: Unhandled response for MNet messsge: " + Integer.toHexString(msgCode));
                break;
        }

        return false;
    }

    // debugging
    private void dbgTrace(String trace) {
        GenUtils.logCat("RemoteMeemCoreV2Proxy", trace);
        GenUtils.logMessageToFile("RemoteMeemCoreV2Proxy.log", trace);
    }

    // debugging
    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
