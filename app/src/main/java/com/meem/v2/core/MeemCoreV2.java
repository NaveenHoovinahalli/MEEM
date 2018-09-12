package com.meem.v2.core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.utils.GenUtils;
import com.meem.v2.mmp.MMPV2Constants;
import com.meem.v2.net.MNetConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.meem.androidapp.ContactTrackerWrapper.tag;

@SuppressLint("UseSparseArrays")
public class MeemCoreV2 implements FileXfrV2Listener, RemoteMeemCoreV2Proxy.RemoteMeemCoreV2ProxyListener {
    private static final String TAG = "MeemCoreV2";

    // meemcore is singleton
    private static volatile MeemCoreV2 mThis;
    private boolean mShutDownRequest;

    // The following three sizes are very important parameters (do not change it without consulting with Arun/Barath).
    private static final int MEEM_STABLE_USB_RECV_BUF_SIZE = 2048;
    private static final int MEEM_STABLE_USB_RECV_BUF_SIZE_LOWSPEED = 256;
    private static int MEEM_INEDA_HW_BUFFER_SIZE = (32 * 1024);

    // read buffers
    private int mUsbReadBufSize = MEEM_STABLE_USB_RECV_BUF_SIZE;
    private byte[] mUsbReadBuffer;
    private ByteBuffer mMmpPayloadBuffer;

    // accessory and core handler, i/o stream objects
    private AccessoryInterface mAccessory;
    private MeemCoreV2Listener mCoreHandler;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    // xfr related
    private static final Object mXfrMonitor = new Object();
    private AtomicBoolean mXfrCodeExpected = new AtomicBoolean(true);
    private HashMap<Byte, FileReceiverV2> mFileReceiverMap = null;
    private HashMap<Byte, FileSenderV2> mFileSenderMap = null;
    private boolean mXfrOngoing = false;
    private double mXfrBytes = 0;

    // Begin: For MEEM Network
    private boolean mCableOwned;
    private String mCableOwnerTag;
    private Object mOwnerShipMonitor = new Object();

    public interface MeemNetworkInterface {
        boolean onCtrlMessage(ByteBuffer messasge);
        boolean onDataPacket(ByteBuffer packet);
    }

    private MeemNetworkInterface mMeemNetInterface;

    // for client side
    private RemoteMeemCoreV2Proxy mCoreProxy;

    private byte mRemotePayloadPlatFormCode = MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID;

    // End: For MEEM Network

    private MeemCoreV2() {
        dbgTrace(TAG + " : constructor");
        mShutDownRequest = false;

        mFileReceiverMap = new HashMap<>();
        mFileSenderMap = new HashMap<>();

        mCoreHandler = null;
    }

    /**
     * Get an instance of this singleton class.
     */
    public static synchronized MeemCoreV2 getInstance() {
        if (mThis == null) {
            synchronized (MeemCoreV2.class) {
                if (mThis == null) {
                    mThis = new MeemCoreV2();
                }
            }
        }
        return mThis;
    }

    /**
     * ONLY FOR DEBUGGING AND EXPERIMENTS!!!!
     */
    public void setInedaHwBufferSize(int newSize) {
        dbgTrace("### EXPERIMENTAL ### BUFFER SIZE SET TO: " + newSize);
        MEEM_INEDA_HW_BUFFER_SIZE = newSize;
    }

    public void start(AccessoryInterface accessory) {
        dbgTrace();

        mAccessory = accessory;

        if (null != mCoreHandler) {
            dbgTrace("Cleaning up meem core handler v2 instance: " + mCoreHandler);
            mCoreHandler.cleanup();
        }

        if(null != mFileSenderMap) mFileSenderMap.clear();
        if(null != mFileReceiverMap) mFileReceiverMap.clear();

        mCoreHandler = new MeemCoreV2Handler();

        if(mAccessory.isRemote()) {
            dbgTrace("Accessory is remote. Starting proxy...");
            mCoreProxy = new RemoteMeemCoreV2Proxy(accessory, this);
            mCoreProxy.start();
            return;
        }

        dbgTrace("Accessory is local. Starting legacy core...");

        mInputStream = accessory.getInputStream();
        mOutputStream = accessory.getOutputStream();

        if (accessory.isSlowSpeedMode()) {
            mUsbReadBufSize = MEEM_STABLE_USB_RECV_BUF_SIZE_LOWSPEED;
        } else {
            mUsbReadBufSize = MEEM_STABLE_USB_RECV_BUF_SIZE;
        }

        dbgTrace("Usb raw read buffer size is: " + mUsbReadBufSize);
        mUsbReadBuffer = new byte[mUsbReadBufSize];

        mMmpPayloadBuffer = ByteBuffer.allocate(MEEM_INEDA_HW_BUFFER_SIZE);
        mMmpPayloadBuffer.order(ByteOrder.BIG_ENDIAN);

        mXfrOngoing = false;
        mXfrCodeExpected.set(true);
        mShutDownRequest = false;
        mCableOwned = false;

        Thread usbReaderThread = new UsbReaderThread();
        usbReaderThread.start();
    }

    public void stop() {
        dbgTrace();

        if(mAccessory != null && mAccessory.isRemote() && mCoreProxy != null) {
            mCoreProxy.stop();
        }

        mShutDownRequest = true;

        stopAllReceivers(false);
        stopAllSenders(false);

        if(null != mFileSenderMap) mFileSenderMap.clear();
        if(null != mFileReceiverMap) mFileReceiverMap.clear();

        if(mCoreHandler != null) {
            mCoreHandler.cleanup();
        }
    }

    public MeemCoreV2Listener getHandler() {
        return mCoreHandler;
    }

    public void resetXfrStat() {
        mXfrBytes = 0;
    }

    public double getXfrStat() {
        return mXfrBytes;
    }

    /**
     * Special request to abort all file transfers in progress. Note that this API must be used ONLY in exceptional conditions. You must
     * know what you are doing.
     *
     * @return true.
     */
    public boolean abortAllXfr(boolean waitForFw) {
        dbgTrace();

        stopAllSenders(waitForFw);
        stopAllReceivers(waitForFw);

        return true;
    }

    /**
     * This function shall be the only function to be used to send messages over USB to MEEM.
     *
     * @param usbData Message byte array
     *
     * @return true after successful USB write of the given byte array. false on all errors encountered during USB write operation.
     */
    public boolean sendUsbMessage(ByteBuffer usbData, String txtMsg) {
        return writeUsbCtrlData(usbData, txtMsg);
    }

    public boolean sendFile(String upid, byte fType, byte fMode, byte catCode, String path, String meemPath, String cSum, long rowId, ResponseCallback responseCallback) {
        dbgTrace();

        synchronized (mXfrMonitor) {
            while (mXfrOngoing) {
                try {
                    mXfrMonitor.wait();
                } catch (Exception e) {
                    // Ignored
                }
            }

            mXfrOngoing = true;
        }

        mXfrCodeExpected.set(true);

        FileSenderV2 fileSender = new FileSenderV2(this);

        synchronized (mFileSenderMap) {
            byte xfrId = (byte) mFileSenderMap.size();

            if (xfrId != 0) {
                FileSenderV2 sender = mFileSenderMap.remove(xfrId);
                if (sender != null) {
                    dbgTrace("!BUG! one sender is already active, stopping it: " + sender.toString());
                    sender.stopSender();
                }
            }

            xfrId = (byte) mFileSenderMap.size();
            xfrId = (byte) (xfrId | (byte) 0x80);

            // byte xfrId, String upid, byte fType, byte catCode, long fSize,
            // String fName, int chunkSize, ResponseCallback uicb
            fileSender.init(xfrId, upid, fType, fMode, catCode, path, meemPath, cSum, rowId, MEEM_INEDA_HW_BUFFER_SIZE, responseCallback);

            if (fileSender.prepare() < 0) {
                // Arun: 09June2017: Bugfix - this check was not there and we will add a sender to map which is never gonna be removed.
                // and xfrid will be -127 which will make firmware halt.
                dbgTrace("Filesender prepare failed!");
                return false;
            }

            mFileSenderMap.put(xfrId, fileSender);
        }

        return true;
    }

    public boolean receiveFile(String upid, byte fType, byte fMode, byte catCode, String path, String meemPath, String cSum, ResponseCallback responseCallback) {
        dbgTrace();

        synchronized (mXfrMonitor) {
            while (mXfrOngoing) {
                try {
                    mXfrMonitor.wait();
                } catch (Exception e) {
                    // Ignored
                }
            }

            mXfrOngoing = true;
        }

        mXfrCodeExpected.set(true);

        byte xfrId;
        // create & start a receiver object, post the parameters to it
        FileReceiverV2 fileReceiver = new FileReceiverV2(this);
        synchronized (mFileReceiverMap) {
            xfrId = (byte) mFileReceiverMap.size();
            if (xfrId != 0) {
                FileReceiverV2 rcvr = mFileReceiverMap.remove(xfrId);
                if (rcvr != null) {
                    dbgTrace("!BUG! one receiver is already active, stopping it: " + rcvr.toString());
                    stopReceiver(xfrId, true);
                }
            }

            xfrId = (byte) mFileReceiverMap.size();
            mFileReceiverMap.put(xfrId, fileReceiver);
        }

        fileReceiver.init(xfrId, upid, fType, fMode, catCode, path, meemPath, cSum, MEEM_INEDA_HW_BUFFER_SIZE, responseCallback);
        /*
         * unlike sender object, must start receiver thread now itself. can't be
         * smart by calling prepare directly as those parameters required for
         * prepare should come from fw. for that, thread should start. but
         * remember, thread start time is arbitrary and we can not assume any
         * timing. so we must start the thread, send a message to it asking it
         * to send the RTR to fw, on reception of which, fw will send READY.
         * this way, we are 100% sure that thread is up and running the loop
         * when fw is sending READY. by handling the READY through its message
         * loop, the receiver thread object will call prepare method itself.
         */
        fileReceiver.start();

        Handler rcvHandler = fileReceiver.getSafeHandler();
        Message rmsg = rcvHandler.obtainMessage();
        rmsg.arg1 = FileReceiverV2.MSG_TYPE_SEND_RTR;
        rcvHandler.sendMessage(rmsg);

        return true;
    }

    private void onInitSeqPrimary(ByteBuffer payload) {
        dbgTrace("Primary init sequence receieved (FW just booted up)");

        if(mAccessory.isRemote()) {
            mCoreHandler.onCtrlMessage(payload);
            return;
        }

        synchronized (mOwnerShipMonitor) {
            if (mCableOwned && mMeemNetInterface != null) {
                mMeemNetInterface.onCtrlMessage(payload);
            } else if (mCableOwned && mMeemNetInterface == null) {
                mCoreHandler.onCtrlMessage(payload);
            } else {
                dbgTrace("BUG: Cable not locked during control msg");
            }
        }
    }

    private void onInitSeqSecondary(ByteBuffer payload) {
        dbgTrace("Secondary init sequence receieved (FW was already booted up)");

        if(mAccessory.isRemote()) {
            mCoreHandler.onCtrlMessage(payload);
            return;
        }

        synchronized (mOwnerShipMonitor) {
            if (mCableOwned && mMeemNetInterface != null) {
                mMeemNetInterface.onCtrlMessage(payload);
            } else if (mCableOwned && mMeemNetInterface == null) {
                mCoreHandler.onCtrlMessage(payload);
            } else {
                dbgTrace("BUG: Cable not locked during control msg");
            }
        }
    }

    private void onDataMessage(ByteBuffer payload) {
        /*dbgTrace();*/

        synchronized (mOwnerShipMonitor) {
            if (mCableOwned && mMeemNetInterface != null) {
                mMeemNetInterface.onDataPacket(payload);
                return;
            }
        } // TODO: Other bug checks

        if (!mXfrCodeExpected.get()) {
            // This is receiving mode, expecting only data in whole pkt
            synchronized (mFileReceiverMap) {
                FileReceiverV2 fileReceiver = mFileReceiverMap.get((byte) 0);
                if (null != fileReceiver) {
                    Handler rcvHandler = fileReceiver.getSafeHandler();
                    Message rmsg = fileReceiver.getSafeHandler().obtainMessage();
                    rmsg.obj = payload;
                    rmsg.arg1 = FileReceiverV2.MSG_TYPE_DATA;
                    if (!rcvHandler.sendMessage(rmsg)) {
                        dbgTrace("CRITICAL: sending XFR data message to receiver object failed");
                    }
                } else {
                    dbgTrace("WARN: Receiver object is null during XFR (packet on wire during XFR abort)");
                }
            }

            return;
        }

        // else, usual processing.

        byte xfrCode = payload.get(0);
        byte xfrId = payload.get(1);

        boolean isSender = ((xfrId & 0x80) == 0x80);

        if (xfrCode == MMPV2Constants.MMP_FILE_SEND_XFR_READY) {
            dbgTrace("MMP_FILE_SEND_XFR_READY");

            synchronized (mFileSenderMap) {
                FileSenderV2 fileSender = mFileSenderMap.get(xfrId);
                if (null == fileSender) {
                    dbgTrace("BUG: File sender object is null: id: " + xfrId);
                } else {
                    fileSender.start();
                }
            }

        } else if (xfrCode == MMPV2Constants.MMP_FILE_RECV_XFR_READY) {
            dbgTrace("MMP_FILE_RECV_XFR_READY");

            synchronized (mFileReceiverMap) {
                FileReceiverV2 fileReceiver = mFileReceiverMap.get(xfrId);
                if (null == fileReceiver) {
                    dbgTrace("BUG: File receiver object is null: id: " + xfrId);
                } else {
                    // must do this as a result of firmware issue (it handles xfr separately for Android and iOS).
                    fileReceiver.setPlatform(mRemotePayloadPlatFormCode);

                    long fSize = payload.getLong(2);
                    int cSumLen = payload.get(10);

                    byte[] cSumArray = new byte[cSumLen];

                    payload.position(11);
                    payload.get(cSumArray);
                    String cSum = new String(cSumArray);

                    dbgTrace("Parsed fSize: " + fSize + ", cSum: " + cSum);

                    FileReceiverV2CtrlMsg ctrlMsg = new FileReceiverV2CtrlMsg(null, (byte) 0);
                    ctrlMsg.fSize = fSize;
                    ctrlMsg.chkSum = cSum; // no more!

                    Handler rcvHandler = fileReceiver.getSafeHandler();
                    Message rmsg = rcvHandler.obtainMessage();
                    rmsg.obj = ctrlMsg;
                    rmsg.arg1 = FileReceiverV2.MSG_TYPE_PREPARE;

                    rcvHandler.sendMessage(rmsg);

                    // TODO: Arun: Revisit this (check comment in FileReceiverV2.java)
                    onXfrRecvStateChange(xfrId, false);
                }
            }

        } else if (xfrCode == MMPV2Constants.MMP_FILE_XFR_SUCCESS) {
            dbgTrace("MMP_FILE_XFR_SUCCESS");

            if (isSender) {
                // This is from firmware, for file sender
                FileSenderV2 fileSender = mFileSenderMap.get(xfrId);
                if (null == fileSender) {
                    dbgTrace("BUG: File sender object is null: id: " + xfrId);
                } else {
                    fileSender.notifyXfrSuccessByFw();
                }
            } else {
                // This is from firmware, for file receiver
                FileReceiverV2 fileReceiver = mFileReceiverMap.get(xfrId);
                if (null == fileReceiver) {
                    dbgTrace("BUG: File receiver object is null: id: " + xfrId);
                } else {
                    int cSumLen = payload.get(2);
                    byte[] cSumArray = new byte[cSumLen];

                    payload.position(3);
                    payload.get(cSumArray);
                    String cSum = new String(cSumArray);

                    FileReceiverV2CtrlMsg ctrlMsg = new FileReceiverV2CtrlMsg(null, (byte) 0);
                    ctrlMsg.chkSum = cSum;

                    Handler rcvHandler = fileReceiver.getSafeHandler();
                    Message rmsg = rcvHandler.obtainMessage();
                    rmsg.obj = ctrlMsg;
                    rmsg.arg1 = FileReceiverV2.MSG_TYPE_FW_SAYS_SUCCESS;

                    rcvHandler.sendMessage(rmsg);
                }
            }
        } else if (xfrCode == MMPV2Constants.MMP_FILE_XFR_ABORT || xfrCode == MMPV2Constants.MMP_FILE_XFR_ERROR) {
            // NOTE: 14June2017: For receive abort request from app, FW will aalways send ERROR instead of ABORTED.
            dbgTrace("MMP_FILE_XFR_ABORT || ERROR");
            if (isSender) {
                stopSender(xfrId, false);
            } else {
                stopReceiver(xfrId, false);
            }
        } else if (xfrCode == MMPV2Constants.MMP_FILE_XFR_ABORTED) {
            dbgTrace("MMP_FILE_XFR_ABORTED");

            if (isSender) {
                onXfrSendAbortAckedByFw(xfrId);
            } else {
                onXfrRecvAbortAckedByFw(xfrId);
            }
        } else {
            dbgTrace("WARN: Unknown XFR command code (got data packet?)");

            try {
                byte[] printBuf = new byte[32];
                payload.get(printBuf);
                String dataStr = GenUtils.formatHexString(GenUtils.getHexString(printBuf));
                String shortLog = dataStr + "\n";
                dbgTrace("Chunk[0-32]: " + shortLog);
            } catch (Exception e) {
                // Nothing
            }

            payload.rewind();
        }
    }

    private void onCtrlMessage(ByteBuffer payload) {
        dbgTrace();

        if(mAccessory.isRemote()) {
            mCoreHandler.onCtrlMessage(payload);
            return;
        }

        synchronized (mOwnerShipMonitor) {
            if (mCableOwned && mMeemNetInterface != null) {
                mMeemNetInterface.onCtrlMessage(payload);
            } else if (mCableOwned && mMeemNetInterface == null) {
                mCoreHandler.onCtrlMessage(payload);
            } else {
                dbgTrace("BUG: Cable not locked during control msg");
            }
        }
    }

    private void stopReceiver(byte xfrId, boolean waitForFw) {
        dbgTrace();

        synchronized (mFileReceiverMap) {
            FileReceiverV2 fReceiver = (FileReceiverV2) mFileReceiverMap.get(xfrId);
            if (fReceiver == null) {
                return;
            }

            if (fReceiver.finished) { // no synchronization needed.
                return;
            }

            // ask the thread to stop
            Message msg = fReceiver.getSafeHandler().obtainMessage();
            msg.obj = null;
            msg.arg1 = FileReceiverV2.MSG_TYPE_STOP;
            msg.arg2 = waitForFw ? 0 : FileReceiverV2.MSG_ARG_DIE_NOW;

            // FindBugs fix.
            fReceiver.getSafeHandler().sendMessage(msg);

            if (!waitForFw) {
                mFileReceiverMap.remove(xfrId);
            }
        }
    }

    // =============================================================================

    private void stopAllReceivers(boolean waitForFw) {
        dbgTrace();

        // unlikely
        if (null == mFileReceiverMap) {
            return;
        }

        synchronized (mFileReceiverMap) {
            Iterator<HashMap.Entry<Byte, FileReceiverV2>> iter = mFileReceiverMap.entrySet().iterator();
            while (iter.hasNext()) {
                HashMap.Entry<Byte, FileReceiverV2> entry = iter.next();

                FileReceiverV2 fReceiver = (FileReceiverV2) entry.getValue();
                if (fReceiver == null) {
                    continue;
                }

                if (fReceiver.finished) { // no synchronization needed.
                    continue;
                }

                // ask the thread to stop
                Message msg = fReceiver.getSafeHandler().obtainMessage();
                msg.obj = null;
                msg.arg1 = FileReceiverV2.MSG_TYPE_STOP;
                msg.arg2 = waitForFw ? 0 : FileReceiverV2.MSG_ARG_DIE_NOW;

                // FindBugs fix.
                fReceiver.getSafeHandler().sendMessage(msg);

                if (!waitForFw) {
                    iter.remove();
                }
            }

            if (!waitForFw) {
                mFileReceiverMap.clear();
            }
        }
    }

    private void stopSender(byte xfrId, boolean waitForFw) {
        dbgTrace();

        // unlikely
        if (null == mFileSenderMap) {
            return;
        }

        synchronized (mFileSenderMap) {
            FileSenderV2 fSender = (FileSenderV2) mFileSenderMap.get(xfrId);
            if (fSender == null) {
                return;
            }

            fSender.stopSender();
            if (!waitForFw) {
                fSender.dieNow();
                mFileSenderMap.remove(xfrId);
            }
        }
    }

    private void stopAllSenders(boolean waitForFw) {
        dbgTrace();

        // unlikely.
        if (null == mFileSenderMap) {
            return;
        }

        synchronized (mFileSenderMap) {
            Iterator<HashMap.Entry<Byte, FileSenderV2>> iter = mFileSenderMap.entrySet().iterator();
            while (iter.hasNext()) {
                HashMap.Entry<Byte, FileSenderV2> entry = iter.next();

                FileSenderV2 fSender = (FileSenderV2) entry.getValue();
                if (fSender == null) {
                    return;
                }

                // this will double lock map - but ok.
                fSender.stopSender();
                if (!waitForFw) {
                    fSender.dieNow();
                    iter.remove();
                }
            }

            if (!waitForFw) {
                mFileSenderMap.clear();
            }
        }
    }

    private void removeFromMap(byte xfrId) {
        dbgTrace();

        if ((xfrId & 0x80) == 0x80) {
            synchronized (mFileSenderMap) {
                mFileSenderMap.remove(xfrId);
            }
        } else {
            synchronized (mFileReceiverMap) {
                mFileReceiverMap.remove(xfrId);
            }
        }
    }

    private void onXfrSendAbortAckedByFw(byte xfrId) {
        dbgTrace();

        synchronized (mFileSenderMap) {
            FileSenderV2 fSender = (FileSenderV2) mFileSenderMap.get(xfrId);
            if (fSender == null) {
                return;
            }

            fSender.notifyXfrAbortAckedByFw();
        }
    }

    private void onXfrRecvAbortAckedByFw(byte xfrId) {
        dbgTrace();

        synchronized (mFileReceiverMap) {
            FileReceiverV2 fReceiver = (FileReceiverV2) mFileReceiverMap.get(xfrId);
            if (fReceiver == null) {
                return;
            }

            // notify the thread that firmware has stopped sending file.
            Message msg = fReceiver.getSafeHandler().obtainMessage();
            msg.obj = null;
            msg.arg1 = FileReceiverV2.MSG_TYPE_FW_ACK_ABORT;

            // FindBugs fix.
            fReceiver.getSafeHandler().sendMessage(msg);
        }

    }

    /**
     * =====================================================================================
     * THIS IS A VERY IMPORTANT MATTER: TO SOLVE A BUG IN FW WHERE IT IS NOT
     * HANDLING ZERO LENGTH PACKETS (ZLP - which will come from phone, esp. during restore)
     * PROPERLY, WE MUST SEND 32k-1 BYTES FOR ALL CONTROL CONMMANDS AND XFR-CONTROL
     * COMMANDS. THIS WILL ENSURE THAT ZLP IS NEVER SENT BY PHONE TO MEEM.
     * FOR DATA PACKETS, FW IS TAKING CARE OF ZLP.
     * =====================================================================================
     */
    public boolean writeUsbCtrlData(ByteBuffer bb, String desc) {

        if(mAccessory.isRemote()) {
            mXfrBytes += MEEM_INEDA_HW_BUFFER_SIZE;
            return mCoreProxy.writeMmpCtrlDataToNetwork(bb, desc);
        }

        if (mOutputStream == null) {
            dbgTrace("Null output stream while writing: " + desc);
            return false;
        }

        /*saveBufferForDebugging(bb.array(), bb.array().length, desc, "UsbWriteData.log");*/

        int len = MEEM_INEDA_HW_BUFFER_SIZE - 1; /* see comment above */

        try {
            mOutputStream.write(bb.array(), 0, len);
            mOutputStream.flush();
            mXfrBytes += MEEM_INEDA_HW_BUFFER_SIZE;
            return true;
        } catch (IOException e) {
            dbgTrace("USB write failed: " + e.getMessage() + " for: " + desc);
            return false;
        }
    }

    /**
     * =====================================================================================
     * THIS IS A VERY IMPORTANT MATTER: TO SOLVE A BUG IN FW WHERE IT IS NOT
     * HANDLING ZERO LENGTH PACKETS (ZLP - which will come from phone, esp. during restore)
     * PROPERLY, WE MUST SEND 32k-1 BYTES FOR ALL CONTROL CONMMANDS AND XFR-CONTROL
     * COMMANDS. THIS WILL ENSURE THAT ZLP IS NEVER SENT BY PHONE TO MEEM.
     * FOR DATA PACKETS, FW IS TAKING CARE OF ZLP. SO, HERE WE WILL USE 32k as buffer size.
     * =====================================================================================
     */
    public boolean writeUsbXfrData(ByteBuffer bb, String desc) {
        if(mAccessory.isRemote()) {
            mXfrBytes += MEEM_INEDA_HW_BUFFER_SIZE;
            return mCoreProxy.writeMmpXfrDataToNetwork(bb, desc);
        }

        if (mOutputStream == null) {
            dbgTrace("Null output stream while writing: " + desc);
            return false;
        }

        /*saveBufferForDebugging(bb.array(), bb.array().length, desc, "UsbWriteData.log");*/

        int len = MEEM_INEDA_HW_BUFFER_SIZE; /* see comment above */

        try {
            mOutputStream.write(bb.array(), 0, len);
            mOutputStream.flush();
            mXfrBytes += MEEM_INEDA_HW_BUFFER_SIZE;
            return true;
        } catch (IOException e) {
            dbgTrace("USB write failed: " + e.getMessage() + " for: " + desc);
            return false;
        }
    }

    // debugging
    private void dbgTrace(String trace) {
        GenUtils.logCat("MeemCoreV2", trace);
        GenUtils.logMessageToFile("MeemCoreV2.log", trace);
    }

    // debugging
    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }

    @Override
    public int getPayloadSize() {
        // dbgTrace();
        return MEEM_INEDA_HW_BUFFER_SIZE;
    }

    // ---------------------- XFR Listener interface impl.

    @Override
    public void onXfrRecvStateChange(byte xfrId, boolean xfrCodeExpected) {
        mXfrCodeExpected.set(xfrCodeExpected);
    }

    @Override
    public void onXfrCompletion(byte xfrId, String path, ResponseCallback uicb) {
        dbgTrace();
        mCoreHandler.onXfrCompletion(path);
        removeFromMap(xfrId);

        mXfrCodeExpected.set(true);

        synchronized (mXfrMonitor) {
            mXfrOngoing = false;
            mXfrMonitor.notifyAll();
        }
    }

    @Override
    public void onXfrError(byte xfrId, String path, int result, ResponseCallback uicb) {
        dbgTrace();

        mCoreHandler.onXfrError(path, result);
        removeFromMap(xfrId);

        mXfrCodeExpected.set(true);

        synchronized (mXfrMonitor) {
            mXfrOngoing = false;
            mXfrMonitor.notifyAll();
        }
    }

    @Override
    public boolean sendXfrData(byte[] buffer) {
        return writeUsbXfrData(ByteBuffer.wrap(buffer), "XFR");
    }

    @Override
    public boolean sendXfrCtrlData(byte[] buffer) {
        return writeUsbCtrlData(ByteBuffer.wrap(buffer), "XFRCTRL");
    }

    private class UsbReaderThread extends Thread {
        public void run() {
            dbgTrace("UsbReaderThread starts with read buffer size: " + mUsbReadBufSize);

            int readCount/*, loopCount*/;

            while (true) {
                try {
                    if (mInputStream != null) {
                        readCount = mInputStream.read(mUsbReadBuffer, 0, mUsbReadBufSize);

                        /*loopCount++;
                        String comment = "This read: " + readCount + " bytes. Loop# is: " + loopCount + " . Total: " + (loopCount * mUsbReadBufSize) + "/" + MEEM_INEDA_HW_BUFFER_SIZE + " bytes";
                        saveBufferForDebugging(mUsbReadBuffer, readCount, comment, "UsbReadData.log");*/

                        /*dbgTrace("Read returned with: " + readcount + " bytes");*/
                        mXfrBytes += readCount;

                        // Careful handling is needed for shutting down this thread - as this has impact on
                        // accessory framework in Android.
                        if (mShutDownRequest) {
                            // This is important. Do not change unless you know the impacts.
                            if (mAccessory != null) {
                                mAccessory.closeAccessory();
                                mAccessory = null;
                                dbgTrace("Accesory closed by MeemCore in reader thread");
                            } else {
                                dbgTrace("WTF: Accesory null in reader thread?");
                            }

                            // 11Sept2015: must do this. otherwise receiver threads won't die
                            abortAllXfr(false);

                            mShutDownRequest = false;

                            dbgTrace("Reader thread exiting on request");
                            return; // monitor will be unlocked automatically.
                        }

                        if (readCount < 0) {
                            dbgTrace("WARNING: USB read returns negative: " + readCount);
                            continue;
                        }

                        if (readCount < mUsbReadBufSize) {
                            dbgTrace("WARNING: USB read does not return a full MMP packet: " + readCount + ", expected: " + mUsbReadBufSize);
                            continue;
                        }

                        mMmpPayloadBuffer.put(mUsbReadBuffer, 0, readCount);

                        int curPos = mMmpPayloadBuffer.position();
                        if (curPos == MEEM_INEDA_HW_BUFFER_SIZE) {
                            // Do a deep copy
                            ByteBuffer payloadCopy = ByteBuffer.allocate(MEEM_INEDA_HW_BUFFER_SIZE);
                            payloadCopy.order(ByteOrder.BIG_ENDIAN);

                            mMmpPayloadBuffer.rewind();
                            try {
                                payloadCopy.put(mMmpPayloadBuffer);
                            } catch (Exception e) {
                                dbgTrace("Exception while copying full payload: " + e.getMessage() + " mmpPayLoadBuffer.remaining: " + mMmpPayloadBuffer.remaining() + "CopyBuffer.remaining: " + payloadCopy.remaining());
                            }
                            mMmpPayloadBuffer.clear();

                            payloadCopy.rewind();
                            onCompletePayloadReceived(MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID, payloadCopy);

                            /*loopCount = 0;*/
                        }
                    }
                } catch (IOException e) {
                    String exStr = "Usb reader thread exiting on exception: \n" + GenUtils.getStackTrace(e);
                    dbgTrace(exStr);

                    // 10Sept2015: should have enabled this. Or else receiver threads won't die.
                    abortAllXfr(false);
                    mCoreHandler.onException(e);

                    // Added: 12June2015
                    if (mInputStream != null) {
                        try {
                            mInputStream.close();
                        } catch (IOException ex) {
                            dbgTrace("Usb input stream close exception: " + ex.getMessage());
                        }
                    }

                    try {
                        if (mAccessory != null) {
                            mAccessory.closeAccessory();
                            /*mAccessory = null;*/
                            dbgTrace("Accesory closed by MeemCore on failed write");
                        } else {
                            dbgTrace("Accesory is null during a failed write attept..hmm");
                        }
                    } catch (Exception ex) {
                        dbgTrace("Accessory close exception: " + ex.getMessage());
                    }

                    // final change to handle the disconnect: 22June2015
                    MeemEvent evt = new MeemEvent(EventCode.MEEMCORE_DETECTED_CABLE_DISCONNECT);
                    UiContext.getInstance().postEvent(evt);

                    break;
                }
            }

            stopAllSenders(false);
            stopAllReceivers(false);

            releaseCable("meemcore");

            mCoreHandler.cleanup();
        }
    }

    // TODO: There is no need to synchronize here - because local and remote nodes are required to acquire cable before any operation.
    public void onCompletePayloadReceived(byte platformCode, ByteBuffer payload) {
        /*dbgTrace();*/
        if(mAccessory.isRemote()) {
            mXfrBytes += MEEM_INEDA_HW_BUFFER_SIZE;
        }

        mRemotePayloadPlatFormCode = platformCode;

        int magic = payload.getInt(0);
        if (magic == MMPV2Constants.MMP_HEADER_MAGIC) {
            int initSeq = payload.getInt(4);
            if (initSeq == MMPV2Constants.MMP_INIT_SEQ) {
                onInitSeqPrimary(payload);
            } else {
                onCtrlMessage(payload);
            }
        } else if (magic == MMPV2Constants.MMP_MAGIC_2) {
            int initSeq = payload.getInt(4);
            if (initSeq == MMPV2Constants.MMP_INIT_SEQ_2) {
                onInitSeqSecondary(payload);
            }
        } else {
            onDataMessage(payload);
        }
    }

    /*private void saveBufferForDebugging(byte [] buf, int len, String comment, String fileName) {
        if (!ProductSpecs.INEDA_HW_XFR_DEBUGGING) return;

        try {
            byte[] arr = Arrays.copyOf(buf, len);
            String dataStr = GenUtils.formatHexString(GenUtils.getHexString(arr));
            GenUtils.logMessageToFile(fileName, comment + "\n" + dataStr + "\n");
        } catch (Exception e) {
            dbgTrace("Exception while saving data for dbg: " + e.getMessage());
        }
    }*/

    // ===============================================================
    // ---- MEEM Network related stuff
    // ===============================================================

    /**
     * Protect sequence of operations that should not be interrupted by another meem app (running on a remote client).
     * <p>
     * The parameter tag: For local acquire requests, this shall be the upid of the phone. For remote requests, this shall
     * be the remote socket address of the client.
     * <p>
     * This use of tag is most important to allow multiple acquire request by the same guy. E.g. local driver may explicitely acquire cable
     * during phone registration and carry on multiple MeeMCoreV2Requests. The only requirement with locking is that another meem app must
     * not interfere the sequence of operations.
     * <p>
     * The meenNetIf interface is used by remote clients to take ownership of cable and redirect the iostreams to their sockets treams.
     * Local meem app/MCH must pass null here.
     *
     * @param tag       see description
     * @param meemNetIf see description
     *
     * @return boolean
     */
    public boolean acquireCable(String tag, MeemNetworkInterface meemNetIf) {
        dbgTrace();

        if(mAccessory.isRemote()) {
            return mCoreProxy.acquireRemoteCable(tag);
        }

        synchronized (mOwnerShipMonitor) {

            if (mCableOwnerTag != null && mCableOwnerTag.equals(tag) && mCableOwned == true) {
                dbgTrace(tag + ": is already the current cable owner");
                return true;
            }

            while (mCableOwned && !mShutDownRequest) {
                try {
                    dbgTrace(tag + ": will wait for cable");
                    mOwnerShipMonitor.wait();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            if (mShutDownRequest) {
                mCableOwned = false;
                return false;
            }

            mMeemNetInterface = meemNetIf;
            mCableOwned = true;
            mCableOwnerTag = tag;
            dbgTrace(tag + ": acquired cable");
        }

        return true;
    }

    public boolean releaseCable(String tag) {
        dbgTrace();

        if(mAccessory.isRemote()) {
            return mCoreProxy.releaseRemoteCable(tag);
        }

        synchronized (mOwnerShipMonitor) {
            mCableOwned = false;
            mMeemNetInterface = null;
            dbgTrace(tag + ": released cable");
            mOwnerShipMonitor.notifyAll();
        }

        return true;
    }

    public boolean sendMessageToNetMaster(int msgCode) {
        dbgTrace();

        if(mAccessory.isRemote()) {
            return mCoreProxy.sendMessageToNetMaster(msgCode);
        } else {
            dbgTrace("BUG?: Accessory is not remote!");
            return false;
        }
    }


    // ----------------------------------------------------------
    // remote proxy listener interface implementation
    // ----------------------------------------------------------

    @Override
    public void onCompletePayloadReceivedFromRemote(byte platformCode, ByteBuffer payload) {
        /*dbgTrace();*/
        onCompletePayloadReceived(platformCode, payload);
    }

    @Override
    public void onRemoteCableDisconnectOrError() {
        dbgTrace();

        // inform the ui thread that remote is dead.
        MeemEvent evt = new MeemEvent(EventCode.MEEMCORE_DETECTED_CABLE_DISCONNECT);
        UiContext.getInstance().postEvent(evt);
    }

    @Override
    public int getUsbPayloadSize() {
        return MEEM_INEDA_HW_BUFFER_SIZE;
    }
}
