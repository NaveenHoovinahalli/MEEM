package com.meem.core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.control.MeemCoreHandler;
import com.meem.mmp.messages.MMPCableAuth;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPDummyFwStatus;
import com.meem.usb.Accessory;
import com.meem.utils.GenUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;

/**
 * MeemCore class presently abstracts the MMP data transfer operations defined in MMP specification. It leaves the handling of the MMP
 * control messages to the external modules. When using MeemCore, the user must not read or write any kind of data over USB using any other
 * means than by the interfaces provided by MeemCore class. Obviously it is a singleton class whose lifetime shall be valid during the
 * entire lifetime of an Accessory object and its associated I/O streams once provided to the Activity by Android USBManager class.
 * <p/>
 * Note: in this application, the MeemCoreListener interface is implemented by com.meem.mmp.control.MeemCoreHandler class
 *
 * @author Arun T A
 * @version September 15, 2015 Minor bug fixes because of some regressions - Arun. also removed the native/experimental reader thread
 *          implementations.
 */

@SuppressLint("UseSparseArrays")
public class MeemCore {
    private static volatile MeemCore mThis;

    // private static final int AOA_ADVANCED_MODE_READ_BUFFER_SIZE = (1024 * 16);

    // Special processing to prevent MeemCore being used with
    // incompatible version of firmware. To be removed later.
    // private static final int mMinFwVersion = 4447;

    // 05-Mar-2014, Fix for HashMap release
    // private static final int mMeemCoreVersion = 3338;
    protected MeemCoreListener mCoreHandler;
    AccessoryInterface mAcc;
    private String tag = "MeemCore";
    private InputStream mUsbInStream;
    private OutputStream mUsbOutStream;
    private Object usbOutStreamMonitor = new Object();
    private Thread mUsbReaderThread;
    private HashMap<Byte, FileReceiver> mFileReceiverMap = null;
    private HashMap<Byte, FileSender> mFileSenderMap = null;
    private boolean bUsbReaderException = false;

    private double mXfrBytes = 0;
    private boolean mShutDownRequest = false;

    // IMPORTANT: THESE TWO MEMBERS ARE ONLY FOR DEBUGGING XFR DATALOSS
    private byte mDbgExpChunkIdx = 1;
    private long mDbgBytesRcvd = 0;

    private Object mShutdownMonitor;

    private MeemCore() {
        dbgTrace("MeemCore: Constructor");
        mShutDownRequest = false;
        mCoreHandler = null;
        mUsbInStream = null;
        mUsbOutStream = null;
        mShutdownMonitor = new Object();

        mFileReceiverMap = new HashMap<Byte, FileReceiver>();
        mFileSenderMap = new HashMap<Byte, FileSender>();
    }

    /**
     * Get an instance of this singleton class.
     */
    public static synchronized MeemCore getInstance() {
        if (mThis == null) {
            synchronized (MeemCore.class) {
                if (mThis == null) {
                    mThis = new MeemCore();
                }
            }
        }
        return mThis;
    }

    public MeemCoreListener getHandler() {
        return mCoreHandler;
    }

    public void setShutDownFlag() {
        dbgTrace();

        synchronized (mShutdownMonitor) {
            mShutDownRequest = true;
        }

        dbgTrace("Aborting all XFRs");
        abortAllXfr();
    }

    /**
     * This function must be called after the USB manager notifies the activity about connection of a new accessory and activity gets the
     * I/O streams to communicate with the connected accessory
     */

    public void start(AccessoryInterface acc) {
        dbgTrace();

        this.mAcc = acc;

        // for debugging only.
        if (null != mCoreHandler) {
            mCoreHandler.startupSanityCheck();
        }

        mCoreHandler = new MeemCoreHandler();

        mUsbInStream = mAcc.getInputStream();
        mUsbOutStream = mAcc.getOutputStream();

        if (null != mFileReceiverMap) {
            mFileReceiverMap.clear();
        }

        if (null != mFileSenderMap) {
            mFileSenderMap.clear();
        }

        bUsbReaderException = false;

        // IMPORTANT: ONLY FOR XFR DATA MISSING TESTS
        mDbgExpChunkIdx = 1;

        mShutDownRequest = false;

        // start read thread
        mUsbReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readUsbLegacy(mUsbInStream);
            }
        });

        if (ProductSpecs.DUMMY_CABLE_MODE) {
            dbgTrace("Test/Dummy mode is set");
            return;
        }

        mUsbReaderThread.start();
    }

    /**
     * this function will initiate the probe for cable.
     * <p/>
     * When the cable is just inserted and the firmware is launched, following are the sequence - app sends 0x3E 0xE3 0x3E 0xE3 0x6D 0x65
     * 0x65 0x6D firmware responds with 0x6D, 0x65, 0x65, 0x6D, 0x3E, 0xE3, 0x3E, 0xE3 app responds with 0x6D, 0x65, 0x65, 0x6D, 0x3E, 0xE3,
     * 0x3E, 0xE3 after this firmware will send GET_TIME and GET_RANDOM_SEED commands
     * <p/>
     * When the app is killed but the firmware is already running, following are the sequence - app sends 0x3E 0xE3 0x3E 0xE3 0x6D 0x65 0x65
     * 0x6D firmware responds with 0x3E 0xE3 0x3E 0xE3 0x6D 0x65 0x65 0x6D
     * <p/>
     * NOTE: this function shall only be called after calling MeemCore.start()
     * <p>
     * Update on 26Nov2016
     * <p>
     * The new Init sequence will have the following structure, First 8 bytes - Normal usual sequence Next 4 bytes - Blackberry or Android 4
     * byte hex code Next 4 bytes - 0x736c6f77 Always the Serial number will come as #512.
     */

    public void probeForCable(boolean blacklistedPhone) {
        dbgTrace();

        // Arun: 16Jan2016: Changing the probe sequence for platform recognition
        // Appending 0xAAAA3EE3 for Android
        // Blackberry must send 0xBBBB3EE3 appended here.

        byte[] probeSequence = {(byte) 0x3E, (byte) 0xE3, (byte) 0x3E, (byte) 0xE3, (byte) 0x6D, (byte) 0x65, (byte) 0x65, (byte) 0x6D, (byte) 0xAA, (byte) 0xAA, (byte) 0x3E, (byte) 0xE3};

        ByteBuffer buf = ByteBuffer.allocate(1024 * 16);

        buf.put(probeSequence);

        if (blacklistedPhone) {
            buf.putInt(MMPConstants.BLACKLISTED_PHONE_SLOWSPEED);
        }

        if (false == sendUsbMessage(buf.array())) {
            dbgTrace("Probe request failed!");
        } else {
            dbgTrace("Probe request completed");
        }
    }

    public void probeForCableLegacy() {
        dbgTrace();

        byte[] probeSequence = {(byte) 0x3E, (byte) 0xE3, (byte) 0x3E, (byte) 0xE3, (byte) 0x6D, (byte) 0x65, (byte) 0x65, (byte) 0x6D};

        if (false == sendUsbMessage(probeSequence)) {
            dbgTrace("Probe request failed!");
        } else {
            dbgTrace("Probe request completed");
        }
    }

    /**
     * This function must be called when the USB manager notifies the activity about disconnection of an accessory.
     */
    public void stop() {
        dbgTrace();

        synchronized (usbOutStreamMonitor) {
            if (null != mUsbOutStream) {
                try {
                    mUsbOutStream.close();
                } catch (IOException e) {
                    dbgTrace("Error closing usb output stream: " + e.getMessage());
                }
                mUsbOutStream = null;
            }
        }

        /**
         * The reader thread will always encounter exception on disconnect and
         * exit itself.
         *
         * 13Aug2015 Arun: Below change is added to properly handle currently
         * executing MMP handler and previously queued up MMP handlers in meem
         * core handler queue
         */
        if (mCoreHandler != null) {
            dbgTrace("Cleaning up meem core handler instance: " + mCoreHandler);
            mCoreHandler.cleanup();
        }

        bUsbReaderException = false;
    }

    /**
     * Special request to abort all file transfers in progress. Note that this API must be used ONLY in exceptional conditions. You must
     * know what you are doing.
     *
     * @return true.
     */
    public boolean abortAllXfr() {
        dbgTrace();

        stopAllSenders();
        stopAllReceivers();
        return true; // for the time being.
    }

    /**
     * This function shall be the only function to be used to send messages over USB to MEEM.
     *
     * @param message Message byte array
     *
     * @return true after successful USB write of the given byte array. false on all errors encountered during USB write operation.
     */
    public boolean sendUsbMessage(byte[] message) {
        return writeUsb(message);
    }

    // ------------------------------------------------------------
    // private stuff
    // ------------------------------------------------------------

    // Thread to read from USB and take actions. This is the 'core' thread
    private void readUsbLegacy(InputStream is) {
        dbgTrace("USB reader thread starting (readUsbLegacy)");
        int readLen = 0;

        if (ProductSpecs.USB_BUFFER_SIZE_UNDER_FW_CONTROL) {
            dbgTrace("Warning: MMP Buffer size is set by firmware.");
        }

        final int readBufSize = MMPConstants.MMP_PKT_SIZE;
        dbgTrace("MMP USB buffer size: " + MMPConstants.MMP_PKT_SIZE + ". App AOA read bffer size: " + readBufSize);

        while (true) {
            if (bUsbReaderException) {
                dbgTrace("Warning: USB read continues after reception of invalid packet");
                bUsbReaderException = false;
            }

            try {
                ByteBuffer buf;

                try {
                    buf = ByteBuffer.allocate(readBufSize);
                } catch (Exception ex) {
                    dbgTrace("CRITICAL: Could not allocate ByteBuffer for bytes: " + readBufSize);
                    continue; // go to read again.
                }

                readLen = is.read(buf.array(), 0, readBufSize);

                // Careful handling is needed for shutting down this thread - as this has impact on
                // accessory framework in Android.
                synchronized (mShutdownMonitor) {
                    if (mShutDownRequest) {
                        // This is important. Do not change unless you know the impacts.
                        if (mAcc != null) {
                            mAcc.closeAccessory();
                            mAcc = null;
                            dbgTrace("Accesory closed by MeemCore in reader thread");
                        } else {
                            dbgTrace("WTF: Accesory null in reader thread?");
                        }

                        // 11Sept2015: must do this. otherwise receiver threads won't die
                        abortAllXfr();

                        // Notify whoever waiting for this to happen
                        mShutDownRequest = false;
                        mShutdownMonitor.notifyAll();

                        dbgTrace("Reader thread exiting on request");
                        return; // monitor will be unlocked automatically.
                    }
                }

                if (readLen < 0) {
                    dbgTrace("WARNING: USB read returns negative: " + readLen);
                    continue;
                }

                if (readLen < readBufSize) {
                    dbgTrace("WARNING: USB read does not return a full MMP packet: " + readLen);
                    continue;
                }

                mXfrBytes += readLen;

                buf.order(ByteOrder.BIG_ENDIAN);

                // Check for XFR first, even there preference is for XFR_DATA
                byte xfrCode = buf.get(0);
                if (xfrCode == MMPConstants.XFR_FILEDATA || xfrCode == MMPConstants.XFR_REQUEST || xfrCode == MMPConstants.XFR_ABORT || xfrCode == MMPConstants.XFR_ERROR) {
                    onDataMessage(buf);
                } else {
                    int magic = buf.getInt(0);
                    if (magic == MMPConstants.MMP_MAGIC) {
                        int initSeq = buf.getInt(4);
                        if (initSeq == MMPConstants.MMP_INIT_SEQ) {
                            postProbeSucceeded();

                            // OK. Tell firmware to go with GET_TIME and GET_RANDOM_SEED
                            dbgTrace("probe response (primary) received. Acking");
                            writeUsb(buf.array());

                            if (ProductSpecs.ENABLE_GENUINE_CABLE_CHECK) {
                                dbgTrace("Going for cable authentication");
                                initiateCableAuth();
                            } else {
                                dbgTrace("Cable authentication is DISABLED. Bypassing it for MMP");
                                bypassCableAuth();
                            }
                        } else {
                            // Create an anonymous thread and call the listener interface from that thread context.
                            final ByteBuffer ctrlPkt = buf.duplicate();
                            new Thread() {
                                public void run() {
                                    mCoreHandler.onCtrlMessage(ctrlPkt);
                                }
                            }.start();
                        }
                    } else if (magic == MMPConstants.MMP_MAGIC_2) {
                        int initSeq = buf.getInt(4);
                        if (initSeq == MMPConstants.MMP_INIT_SEQ_2) {
                            dbgTrace("Probe response (secondary) received. FW already initialized.");
                            postProbeSucceeded();

                            dbgTrace("Cable was already connected. Bypassing authentication.");
                            bypassCableAuth();
                        } else {
                            String errStr = "Invalid probe response from firmware!: \n" + GenUtils.formatHexString(GenUtils.getHexString(buf.array()));
                            dbgTrace(errStr);
                        }
                    } else {
                        String errStr = "Invalid packet received: neither control nor data: \n" + GenUtils.formatHexString(GenUtils.getHexString(buf.array()));

                        dbgTrace(errStr);
                        debugDumpReceiverMap();

                        bUsbReaderException = true;

                        Throwable ex = new RuntimeException(errStr);
                        mCoreHandler.onException(ex);
                    }
                }
            } catch (Exception ex) {
                String exStr = "Usb reader thread exiting on exception: \n" + GenUtils.getStackTrace(ex);

                Log.wtf(tag, exStr);
                dbgTrace(exStr);

                // 10Sept2015: should have enabled this. Or else receiver threads won't die.
                abortAllXfr();

                mCoreHandler.onException(ex);

                // Added: 12June2015
                if (mUsbInStream != null) {
                    try {
                        mUsbInStream.close();
                    } catch (IOException e) {
                        dbgTrace("Usb input stream close exception: " + e.getMessage());
                    }
                }

                try {
                    if (mAcc != null) {
                        mAcc.closeAccessory();
                        mAcc = null;
                        dbgTrace("Accesory closed by MeemCore on failed write");
                    } else {
                        dbgTrace("Accesory is null during a failed write attept..hmm");
                    }
                } catch (Exception e) {
                    dbgTrace("Accessory close exception: " + e.getMessage());
                }

                synchronized (mShutdownMonitor) {
                    mShutDownRequest = false;
                    mShutdownMonitor.notifyAll();
                }

                // final change to handle the disconnect: 22June2015
                MeemEvent evt = new MeemEvent(EventCode.MEEMCORE_DETECTED_CABLE_DISCONNECT);
                UiContext.getInstance().postEvent(evt);

                return;
            }
        } // while true
    }

    private void postProbeSucceeded() {
        MeemEvent evt = new MeemEvent(EventCode.CABLE_PROBE_SUCCEEDED);
        UiContext.getInstance().postEvent(evt);
    }

    private void initiateCableAuth() {
        MMPCableAuth authCable = new MMPCableAuth();
        writeUsb(authCable.getBuffer().array());
    }

    // TODO: Remove this hack by removing this initial handshake stupidity altogether from MMP.
    private void bypassCableAuth() {
        final MMPDummyFwStatus fwStatus = new MMPDummyFwStatus(MMPConstants.MMP_INTERNAL_HACK_BYPASS_CABLE_AUTH);

        new Thread() {
            public void run() {
                mCoreHandler.onCtrlMessage(fwStatus.getBuffer());
            }
        }.start();
    }

    private void onDataMessage(ByteBuffer msg) {
        // IMPORTANT: MUST BE FALSE ALWAYS - UNLESS FOR DEVELOPER TESTING
        if (ProductSpecs.ENABLE_XFR_DATALOSS_DEBUG) {
            dbgXfrDataLoss(msg);
            return;
        }

        byte xfrCode = msg.get(0);

        if (xfrCode == MMPConstants.XFR_REQUEST) {
            Log.d(tag, "Got XFR_REQUEST");

            // Arun: Abort fix 22May2015: Handle the acceptability of this request by the current handler.
            if (mCoreHandler != null) {
                if (!mCoreHandler.onXfrRequest()) {
                    dbgTrace("XFR_REQUEST is rejected as handler says no-way!");
                    ByteBuffer xfrMsg = ByteBuffer.allocate(6);
                    xfrMsg.put(MMPConstants.XFR_ABORT);
                    xfrMsg.put(msg.get(1));
                    writeUsb(xfrMsg.array());
                    return;
                }
            }

            // This is remnants of original specification.
            int chunkSize = MMPConstants.MMP_PKT_SIZE;

            // Arun: Change in specification on 19Aug2014
            byte xfrId = msg.get(1);
            short msgLength = msg.getShort(2);

            byte xOfy = msg.get(4);

            byte partNum = (byte) ((xOfy & 0xF0) >> 4);
            byte partTot = (byte) (xOfy & 0x0F);

            if (partNum == 1) {
                dbgTrace("XFR_REQUEST part 1 of " + partTot);

                long fSize = msg.getLong(5);

                // read checksum from position 13
                msg.position(13);
                ByteBuffer csum = ByteBuffer.allocate(ProductSpecs.XFR_CSUM_LENGTH);
                msg = msg.get(csum.array(), 0, ProductSpecs.XFR_CSUM_LENGTH);
                String chkSum = new String(csum.array());

                int fnLength = msgLength - MMPConstants.MMP_XFR_REQ_SINGLE_PART_HEADER_LEN;
                if (fnLength > (MMPConstants.MMP_PKT_SIZE - MMPConstants.MMP_XFR_REQ_SINGLE_PART_HEADER_LEN)) {
                    String errStr = "Invalid FPATH length or Multipart message: " + fnLength + "for XFRID (int): " + (int) xfrId + ", XofY: " + xOfy;
                    Log.wtf(tag, errStr);
                    Throwable ex = new RuntimeException(errStr);
                    mCoreHandler.onException(ex);
                    return;
                }

                ByteBuffer baFileName = ByteBuffer.allocate(fnLength);

                // read file path from next location after checksum
                msg.get(baFileName.array(), 0, fnLength);
                String fName = new String(baFileName.array());

                File thisFile = new File(fName);

                String origFileName = fName;

                // NOTE: This is a shameless hack for handling MML files.
                if (!thisFile.isAbsolute()) {
                    boolean isSending = ((xfrId & 0x80) == 0x80);
                    fName = hackForExternalStorage(fName, isSending);
                    if (null == fName) {
                        dbgTrace("BUG: hackForExternalStorage returns NULL!");
                        // Will proceed and fail in prepare of receiver.
                    }
                    dbgTrace("XFR_REQEST: " + "ID: " + xfrId + " File: " + fName + " Checksum: " + chkSum);
                } else {
                    dbgTrace("XFR_REQEST for MML: " + "ID: " + xfrId + " File: " + fName);
                }

                if ((xfrId & 0x80) == 0x80) {
                    FileSender fileSender = new FileSender(this);
                    // Sender do not need checksum.
                    fileSender.init(fName, xfrId, fSize, chunkSize, xOfy);

                    // TODO: This if condition is duplicated 3 times logically in this function.
                    // Can merge all once things are tested fine.
                    if (partNum == partTot) {
                        dbgTrace("Preparing to send: Id: " + xfrId);

                        if (0 == fileSender.prepare()) {
                            synchronized (mFileSenderMap) {
                                mFileSenderMap.put(Byte.valueOf(xfrId), fileSender);
                            }

                            fileSender.start();
                        }
                    } else {
                        // 03Sept2015: put it in the map so that it is there to process the other parts.
                        if (partNum == 1) {
                            synchronized (mFileSenderMap) {
                                mFileSenderMap.put(Byte.valueOf(xfrId), fileSender);
                            }
                        }
                    }
                } else {
                    // create & start a receiver object, post the parameters to
                    FileReceiver fileReceiver = new FileReceiver(this);
                    synchronized (mFileReceiverMap) {
                        mFileReceiverMap.put(Byte.valueOf(xfrId), fileReceiver);
                    }

                    fileReceiver.start();

                    boolean isCrossPlatform = false;
                    isCrossPlatform = origFileName.contains("Iassets-library://");

                    ReceiverCtrlMsg ctrlMsg = new ReceiverCtrlMsg(fName, xfrId, fSize, chunkSize, xOfy, chkSum, isCrossPlatform);

                    Handler rcvHandler = fileReceiver.getSafeHandler();
                    Message rmsg = rcvHandler.obtainMessage();
                    rmsg.obj = ctrlMsg;
                    rmsg.arg1 = FileReceiver.MSG_TYPE_INIT;

                    rcvHandler.sendMessage(rmsg);

                    if (partNum == partTot) {
                        dbgTrace("Preparing to receive: Id: " + xfrId);

                        ctrlMsg = new ReceiverCtrlMsg(fName, xOfy);
                        rmsg = rcvHandler.obtainMessage();
                        rmsg.obj = ctrlMsg;
                        rmsg.arg1 = FileReceiver.MSG_TYPE_PREPARE;

                        rcvHandler.sendMessage(rmsg);
                    }
                }
            }

            // If it is multiple-part message, we need additional processing from second part onwards as the header
            // is different from xOfy field in further messages. Remember, all that is changing is the file path.
            if ((partTot != 1) && (partNum != 1)) {
                dbgTrace("XFR_REQUEST multi part " + partNum + " of " + partTot);

                int fnPartLength = msgLength - -MMPConstants.MMP_XFR_REQ_SINGLE_PART_HEADER_LEN - (partNum - 1) * MMPConstants.MMP_XFR_REQ_MULTI_PART_HEADER_LEN;

                ByteBuffer baFileName = ByteBuffer.allocate(fnPartLength);

                // read file path from next location
                msg.position(5);
                msg.get(baFileName.array(), 0, fnPartLength);
                String fName = new String(baFileName.array());

                if ((xfrId & 0x80) == 0x80) {
                    // sending
                    synchronized (mFileSenderMap) {
                        FileSender fileSender = mFileSenderMap.get(Byte.valueOf(xfrId));
                        if (null == fileSender) {
                            dbgTrace("BUG: File sender object is null during multi part message processing: id: " + xfrId);
                            return;
                        }
                        fileSender.update(fName);

                        if (partNum == partTot) {
                            dbgTrace("Preparing to send: Id: " + xfrId);

                            if (0 == fileSender.prepare()) {
                                fileSender.start();
                            } else {
                                mFileSenderMap.remove(Byte.valueOf(xfrId));
                            }
                        }
                    }
                } else {
                    synchronized (mFileReceiverMap) {
                        FileReceiver fileReceiver = mFileReceiverMap.get(Byte.valueOf(xfrId));

                        if (null == fileReceiver) {
                            dbgTrace("BUG: File receiver object is null during multi part message processing: id: " + xfrId);
                            return;
                        }

                        ReceiverCtrlMsg ctrlMsg = new ReceiverCtrlMsg(fName, xOfy);
                        Handler rcvHandler = fileReceiver.getSafeHandler();
                        Message rmsg = rcvHandler.obtainMessage();
                        rmsg.obj = ctrlMsg;
                        rmsg.arg1 = FileReceiver.MSG_TYPE_UPDATE;

                        rcvHandler.sendMessage(rmsg);

                        if (partNum == partTot) {
                            dbgTrace("Preparing to receive: Id: " + xfrId);

                            ctrlMsg = new ReceiverCtrlMsg(fName, xOfy);
                            rcvHandler = fileReceiver.getSafeHandler();
                            rmsg = rcvHandler.obtainMessage();
                            rmsg.obj = ctrlMsg;
                            rmsg.arg1 = FileReceiver.MSG_TYPE_PREPARE;
                        }
                    }
                }
            }
        } else if (xfrCode == MMPConstants.XFR_FILEDATA) {
            byte xfrId = msg.get(1);
            if ((xfrId & 0x80) == 0x80) {
                // Direction: To MEEM, impossible in this thread
                dbgTrace("BUG: Invalid XFRID direction: " + xfrId);
            } else {
                synchronized (mFileReceiverMap) {
                    FileReceiver fileReceiver = mFileReceiverMap.get(Byte.valueOf(xfrId));
                    if (null != fileReceiver) {
                        Handler rcvHandler = fileReceiver.getSafeHandler();
                        Message rmsg = fileReceiver.getSafeHandler().obtainMessage();
                        rmsg.obj = msg;
                        rmsg.arg1 = FileReceiver.MSG_TYPE_DATA;
                        if (!rcvHandler.sendMessage(rmsg)) {
                            dbgTrace("CRITICAL: sending XFR data message to receiver object failed for xfrid: " + xfrId);
                        }
                    } else {
                        dbgTrace("WARN: File receiver object is null during XFR (packet on wire during XFR abort): id: " + xfrId);
                    }
                }
            }
        } else if (xfrCode == MMPConstants.XFR_ERROR || xfrCode == MMPConstants.XFR_ABORT) {
            byte xfrId = msg.get(1);
            if ((xfrId & 0x80) == 0x80) {
                stopSender(xfrId);
            } else {
                stopReceiver(xfrId);
            }
        } else {
            byte xfrId = msg.get(1);
            dbgTrace("BUG: Unhandled xfr code: " + xfrCode + " for xfr id: " + xfrId);
        }
    }

    /**
     * This is a dirty hack to deal with secondary external storage (aka removable SDCARD). What it essentially does is, if the path starts
     * with character 'I' it will look for internal storage root path and prepend it. If the path begins with character 'S' it will try to
     * see the external storage root path to be used. These are made complicated by the fact that from KITKAT onwards android won't allow us
     * to write outside our package private folder on removable SDCARD.
     * <p/>
     * This hack is ugly because here from within meem core, we need to get the high level classes to get all these info. The ugliest part
     * is: there are the UI level questions to get user options to determine where to save data if the source vault contains SDCARD items
     * and this phone does not have removable SDCARD. All these user options are kept in AppLocalData class and is accessed from here.
     *
     * @param fName     The path name in XFR_REQUEST
     * @param isSending Sending or receiving logic
     *
     * @return fName with correct destination root path prepended.
     */
    private String hackForExternalStorage(String fName, boolean isSending) {
        dbgTrace();

        AppLocalData appLocalData = AppLocalData.getInstance();

        /**
         * Arun: 14Dec2016: Hack for cross platform support (as of now, only iOS)
         *
         * When this Android phone is restoring items from an iOS vault, the path will contain characters that are not valid
         * for an android phone. Typically, an iOS vault's DATD entry will be:
         * <p>
         * "Iassets-library://asset/asset.JPG?id=497B8161-760A-4DD7-A794-AE5ABC5EAC44&amp;ext=JPG"
         * </p>
         *
         * It should be converted to something like:
         *
         * "IiPhone/Media/497B8161-760A-4DD7-A794-AE5ABC5EAC44.JPG"
         */

        if (fName.contains("Iassets-library://") && !isSending) {
            dbgTrace("Sanitizing for reception of iOS item: " + fName);
            fName = GenUtils.sanitizeIOSAssetLibraryItemPath(fName);
            dbgTrace("Sanitized iOS Item: " + fName);
        }

        char prefix = fName.charAt(0);
        String hackPath = fName.substring(1);

        if ('I' == prefix) {
            hackPath = appLocalData.getInternalStorageRootPath() + File.separator + hackPath;
        } else if ('S' == prefix) {
            String secStoreRootPath;

            if (isSending) {
                secStoreRootPath = appLocalData.getSecondaryExternalStorageRootPath();
                hackPath = secStoreRootPath + File.separator + hackPath;

                File trialFile = new File(hackPath);
                if (!trialFile.exists()) {
                    secStoreRootPath = appLocalData.getSecondaryExternalStorageRootPath();
                    hackPath = secStoreRootPath + File.separator + hackPath;

                    trialFile = new File(hackPath);
                    if (!trialFile.exists()) {
                        return null;
                    }
                }

                hackPath = trialFile.getAbsolutePath();
            } else {
                secStoreRootPath = appLocalData.getSecondaryExternalStoragePrivateRootPath();

                if (null != secStoreRootPath) {
                    // TODO: this option check is to be removed as it will be decided in UI level that if
                    // secondary external memory is available, we will always use it.
                    if (appLocalData.getPrimaryStorageUsageOption()) {
                        hackPath = appLocalData.getInternalStorageRootPath() + File.separator + hackPath;
                    } else {
                        hackPath = secStoreRootPath + File.separator + hackPath;
                    }
                } else {
                    dbgTrace("WARNING: No secondary storage private root to save file. Saving to internal storage");
                    hackPath = appLocalData.getInternalStorageRootPath() + File.separator + hackPath;
                }
            }
        } else {
            dbgTrace("CRITICAL ERROR: XFR_REQUEST path does not conform to meem convensions: " + fName);
            return null;
        }

        dbgTrace(fName + " -> " + hackPath);

        return hackPath;
    }

    private void stopReceiver(byte xfrId) {
        dbgTrace();

        synchronized (mFileReceiverMap) {
            FileReceiver fReceiver = (FileReceiver) mFileReceiverMap.get(xfrId);
            if (fReceiver == null) {
                return;
            }

            if (fReceiver.done) { // no synchronization needed.
                return;
            }

            // ask the thread to stop
            Message msg = fReceiver.getSafeHandler().obtainMessage();
            msg.obj = null;
            msg.arg1 = FileReceiver.MSG_TYPE_STOP;

            // FindBugs fix.
            fReceiver.getSafeHandler().sendMessage(msg);

            mFileReceiverMap.remove(xfrId);
        }
    }

    private void stopAllReceivers() {
        dbgTrace();

        // unlikely
        if (null == mFileReceiverMap) {
            return;
        }

        synchronized (mFileReceiverMap) {
            Iterator<HashMap.Entry<Byte, FileReceiver>> iter = mFileReceiverMap.entrySet().iterator();
            while (iter.hasNext()) {
                HashMap.Entry<Byte, FileReceiver> entry = iter.next();

                FileReceiver fReceiver = (FileReceiver) entry.getValue();
                if (fReceiver == null) {
                    continue;
                }

                if (fReceiver.done) { // no synchronization needed.
                    continue;
                }

                // ask the thread to stop
                Message msg = fReceiver.getSafeHandler().obtainMessage();
                msg.obj = null;
                msg.arg1 = FileReceiver.MSG_TYPE_STOP;
                // FindBugs fix.
                fReceiver.getSafeHandler().sendMessage(msg);

                iter.remove();
            }

            mFileReceiverMap.clear();
        }
    }

    private void stopSender(byte xfrId) {
        dbgTrace();

        synchronized (mFileSenderMap) {
            FileSender fSender = (FileSender) mFileSenderMap.get(xfrId);
            if (fSender == null) {
                return;
            }

            fSender.stopSender();

            mFileSenderMap.remove(xfrId);
        }
    }

    private void stopAllSenders() {
        dbgTrace();

        // unlikely.
        if (null == mFileSenderMap) {
            return;
        }

        synchronized (mFileSenderMap) {
            Iterator<HashMap.Entry<Byte, FileSender>> iter = mFileSenderMap.entrySet().iterator();
            while (iter.hasNext()) {
                HashMap.Entry<Byte, FileSender> entry = iter.next();

                FileSender fSender = (FileSender) entry.getValue();
                if (fSender == null) {
                    return;
                }

                // this will double lock map - but ok.
                fSender.stopSender();
                iter.remove();
            }
            mFileSenderMap.clear();
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

    public int debugDumpReceiverMap() {
        dbgTrace("Begin: Receiver object map dump");
        int numItems = 0;

        synchronized (mFileReceiverMap) {
            numItems = mFileReceiverMap.size();
            dbgTrace("Receiver object map has " + numItems + " items");

            Iterator<HashMap.Entry<Byte, FileReceiver>> iter = mFileReceiverMap.entrySet().iterator();
            while (iter.hasNext()) {
                HashMap.Entry<Byte, FileReceiver> entry = iter.next();

                FileReceiver fReceiver = (FileReceiver) entry.getValue();
                if (fReceiver == null) {
                    continue;
                }

                dbgTrace(fReceiver.toString());
            }
        }
        dbgTrace("End: Receiver object map dump");

        return numItems;
    }

    // -------------------------------------------------
    // Package private stuff.
    // Note: The following functions will be called from sender/receiver
    // thread contexts.
    // -------------------------------------------------

    // raw USB write
    boolean writeUsb(byte[] message) {
        synchronized (usbOutStreamMonitor) {

            if (mUsbOutStream == null) {
                dbgTrace("sendData: error: mUsbOutStream is null!");
                return false;
            }

            try {
                mUsbOutStream.write(message);
                mUsbOutStream.flush();
                mXfrBytes += message.length;
            } catch (Exception ex) {
                dbgTrace("sendData: USB write failed on: " + mUsbOutStream);
                // Arun: 02June: Major bug fix
                // ---------------------------
                // Since writeUsb function is called from MMPHandlers in its thread context (remember: each MMPHandler
                // is a thread), calling back to them can cause deadlocks on exceptions like this - as the onException()
                // may call the safe termination of the same thread.
                //
                // See comments in MMPHandler.selfCheck() and MMPHandler.terminate()
                new Thread() {
                    public void run() {
                        mCoreHandler.onException(new IllegalStateException("USB Write failed. Cable disconnected?"));
                    }
                }.start();

                // Keyur: 17July2015: Need to close accessory on failed write.
                if (mAcc != null) {
                    try {
                        mAcc.closeAccessory();
                        dbgTrace("Accesory closed by MeemCore on failed write");
                    } catch (Exception e) {
                        dbgTrace("WTF: Accesory close exception on failed write: " + e.getMessage());
                    }

                    mAcc = null;
                } else {
                    dbgTrace("Accesory is null during a failed write attept..hmm");
                }

                return false;
            }

            return true;
        }
    }

    void onXfrCompletion(byte xfrId, File file) {
        dbgTrace();

        mCoreHandler.onXfrCompletion(file);
        removeFromMap(xfrId);
    }

    void onXfrError(byte xfrId, File file, MeemCoreStatus error) {
        dbgTrace();

        mCoreHandler.onXfrError(file, error);
        removeFromMap(xfrId);
    }

    // For developer testing & debugging only
    private void dbgXfrDataLoss(ByteBuffer msg) {
        byte chunkIdx = msg.get(2);
        if (mDbgExpChunkIdx != chunkIdx) {
            dbgTrace("Chunk index mismatch: expected: " + mDbgExpChunkIdx + " received: " + chunkIdx);
            System.exit(-1);
        }
        mDbgExpChunkIdx++;

        if ((mDbgBytesRcvd % (1024 * 1024)) == 0) {
            dbgTrace("Received: " + mDbgBytesRcvd);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ByteBuffer pktToSend = ByteBuffer.allocate(MMPConstants.MMP_PKT_SIZE);
                    if (!sendUsbMessage(pktToSend.array())) {
                        dbgTrace("Sent response falied!");
                    }
                }
            }).start();
        }

        mDbgBytesRcvd += msg.capacity();
    }

    // debugging
    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("MeemCore.log", trace);
    }

    // debugging
    private void dbgTrace() {
        GenUtils.logMethodToFile("MeemCore.log");
    }

    public void resetXfrStat() {
        mXfrBytes = 0;
    }

    public double getXfrStat() {
        return mXfrBytes;
    }

    public boolean waitForShutdown(boolean isConnected) {
        dbgTrace();

        if (isConnected == false) {
            return false;
        }

        synchronized (mShutdownMonitor) {
            if (!mShutDownRequest) {
                dbgTrace("Shutdown flag is not set! WTF?");
                return false;
            }

            try {
                // 23Sept2014: Important Bugfix: Do not loop here.
                dbgTrace("Waiting for APP QUIT response...");
                mShutdownMonitor.wait(ProductSpecs.HACK_APPQUIT_TIMEOUT_MS);
                dbgTrace("Shutdown flag status now is: " + mShutDownRequest);
                return true;
            } catch (InterruptedException e) {
                dbgTrace("Exception during wait: " + e.getMessage());
                return false;
            }
        }
    }
}
