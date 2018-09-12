package com.meem.v2.core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.meem.androidapp.AppConstants;
import com.meem.events.ResponseCallback;
import com.meem.utils.GenUtils;
import com.meem.v2.mmp.MMPV2Constants;
import com.meem.v2.net.MNetConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * @author Arun T A
 */

class FileReceiverV2 extends Thread {
    public static final int MSG_TYPE_UPDATE = 2;
    public static final int MSG_TYPE_PREPARE = 3;
    public static final int MSG_TYPE_DATA = 4;
    public static final int MSG_TYPE_STOP = 5;
    public static final int MSG_TYPE_SEND_RTR = 6;
    public static final int MSG_TYPE_FW_ACK_ABORT = 7;
    public static final int MSG_TYPE_FW_SAYS_SUCCESS = 8;
    public static final int MSG_ARG_DIE_NOW = 8;
    private static final String TAG = "FileReceiverV2";
    protected boolean finished = false;
    private String upid, desc, fPath;
    private File thisFile;
    private BufferedOutputStream thisStream;
    private byte xfrId, fType, fMode, catCode;
    private int chunkSize = 0, totalChunksReceived = 0; // for dbg
    private long remainBytes = 0;
    private long fileSize = 0;
    private String meemPath;
    private String chkSum;
    private boolean abortReq = false, fileError = false;
    private int errCode = 0, statusIntervalCounter = 0;
    private Handler mHandler;
    private FileXfrV2Listener mListener;
    private ResponseCallback mResponseCallback;

    private MessageDigest digester;

    private byte mPlatform = MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID;
    boolean mAppleRecvStateChangeHack_isXfrCtrlExpected = false; // see usage!!

    public void setPlatform(byte platform) {
        dbgTrace("Receiving from server platform: " + ((platform == MNetConstants.NW_TCP_PLATFORM_CODE_ANDROID) ? "Android" : "Apple"));
        mPlatform = platform;
    }

    FileReceiverV2(FileXfrV2Listener listener) {
        dbgTrace();
        mListener = listener;
    }

    @Override
    public String toString() {
        return "FileReceiver [fileName=" + fPath + ", thisFile=" + thisFile + ", xfrId=" + xfrId + ", chunkSize=" + chunkSize + ", remainBytes=" + remainBytes
                + ", fileSize=" + fileSize + ", done=" + finished + "]" + "\n";
    }

    public void init(byte xfrId, String upid, byte fType, byte fMode, byte catCode, String fName, String meemPath, String cSum, int chunkSize, ResponseCallback uicb) {
        dbgTrace();

        this.xfrId = xfrId;
        this.upid = upid;
        this.fType = fType;
        this.fMode = fMode;
        this.catCode = catCode;

        this.fPath = fName;
        this.chunkSize = chunkSize;
        this.mResponseCallback = uicb;

        this.meemPath = meemPath;
        this.chkSum = cSum;
    }

    private void update(String fName) {
        dbgTrace();
        fPath += fName;
    }

    public int prepare(long fSize, String cSum) {
        dbgTrace();

        this.fileSize = fSize;
        this.chkSum = cSum; // no more!

        try {
            // may happen if there is an unsupported phone which creates issues
            // with our filename hacks for secondary storage.
            if (fPath == null) {
                dbgTrace("BUG: filename is null in receiver");
                errCode = AppConstants.FILE_XFR_ERROR_FILE_NOT_FOUND;
                sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
                return -1;
            }

            // create checksum calculator instance
            try {
                digester = MessageDigest.getInstance("MD5");
            } catch (Exception ex) {
                dbgTrace("ERROR: MD5 digest algorithm not available!");
                errCode = AppConstants.FILE_XFR_ERROR_OTHER;
                sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
                return -1;
            }

            desc = fPath + ", size: " + String.valueOf(fileSize) + ", xfrid: " + String.valueOf(xfrId) + ", csum: " + chkSum;
            this.setName(desc);

            thisFile = new File(fPath);
            if (!thisFile.exists()) {
                boolean strangeFilesystemBug = false;
                File parentDir = new File(thisFile.getParent());
                // Additional check for removable SDCARD related issues
                if (!parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        dbgTrace("Directory tree creation problem for: " + fPath + ": Continuing without aborting.");
                    } else {
                        if (!parentDir.isDirectory()) {
                            dbgTrace("WTF: Parent directory created but it is not a directory where file: " + fPath + " was present.");
                            // See:
                            // http://codereply.com/answer/65fcjk/android-mkdirs-creates-zero-byte-file-instead-folder.html
                            strangeFilesystemBug = true;
                        } else {
                            dbgTrace("Directory tree created properly for: " + fPath);
                        }
                    }
                }

                // Arun: Possible bug identified and fixed on: 28July2015
                if (strangeFilesystemBug || !thisFile.createNewFile()) {
                    dbgTrace("WTF: Could not create new file: " + fPath);
                    errCode = AppConstants.FILE_XFR_ERROR_FILEOPEN;
                    sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
                    return -1;
                }
            } else {
                String log = "Restoring an existing file: " + fPath;
                dbgTrace(log);
            }

            thisStream = new BufferedOutputStream(new FileOutputStream(thisFile));
            remainBytes = fileSize;
        } catch (Exception ex) {
            dbgTrace("Unable to prepare for reception: " + fPath);
            errCode = AppConstants.FILE_XFR_ERROR_FILEOPEN;
            sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);

            if (!thisFile.delete()) {
                dbgTrace("Could not delete file!");
            }

            finished = true;
            return -1;
        }
        dbgTrace("Ready to receive: " + desc);
        return 0;
    }

    private int saveChunk(ByteBuffer buf) {
        /*dbgTrace();*/

        int dataSize = chunkSize - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE;

        totalChunksReceived++;

        buf.rewind();

        try {
            if (remainBytes <= dataSize) {
                dataSize = (int) remainBytes;
                finished = true;
            }

            if (finished) {
                // Inform core asap: TODO: Arun: 09June2017: A race condition with meemcore receiver thread!
                mListener.onXfrRecvStateChange(xfrId, true);
            }

            thisStream.write(buf.array(), MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE, dataSize);

            // calculate checksum
            digester.update(buf.array(), MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE, dataSize);

            if (finished) {
                thisStream.close();
                dbgTrace("All chunks received: " + desc + ", Total chunks: " + totalChunksReceived);
            }
            remainBytes -= dataSize;
        } catch (Exception ex) {
            dbgTrace("Exception while saving chunk: " + ex.getMessage());

            try {
                thisStream.close();
            } catch (Exception e) {
                dbgTrace("Exception during close: " + e.getMessage());
            }

            if (!thisFile.delete()) {
                dbgTrace("Could not delete file!");
            }

            finished = true;

            return AppConstants.FILE_XFR_ERROR_FILEWRITE;
        }

        return AppConstants.SUCCESS;
    }

    /**
     * See commnets everywhere in this file for the hacks made to support iOS fw (over network)!
     *
     * @param buf
     *
     * @return
     */

    private int saveAppleChunk(ByteBuffer buf) {
        dbgTrace();

        // HACK-O-RAMA!!! We must make the core single threaded! what we got is a xfr control message, which meemcore took as data.
        // what we are about to do is, make the proper message and post it to the front of our own queue!
        // Why is this happening only in Apple? Answer is: In android, data transfer is 2K sized chunks whereas in Apple it is 32K sized
        // chunks - so Android will take more time to assemble a whole 32K packet.
        if (mAppleRecvStateChangeHack_isXfrCtrlExpected) {
            mAppleRecvStateChangeHack_isXfrCtrlExpected = false;
            dbgTrace("saveAppleChunk: mAppleRecvStateChangeHack_isXfrCtrlExpected: rerouting");
            if (buf.get(0) == MMPV2Constants.MMP_FILE_XFR_SUCCESS) {
                Message msg = mHandler.obtainMessage();
                msg.arg1 = MSG_TYPE_FW_SAYS_SUCCESS;

                int cSumLen = buf.get(2);
                byte[] cSumArray = new byte[cSumLen];

                buf.position(3);
                buf.get(cSumArray);
                String cSum = new String(cSumArray);

                FileReceiverV2CtrlMsg ctrlMsg = new FileReceiverV2CtrlMsg(null, (byte) 0);
                ctrlMsg.chkSum = cSum;

                msg.obj = ctrlMsg;
                mHandler.sendMessageAtFrontOfQueue(msg);

                finished = true;
                return AppConstants.SUCCESS;
            }
        }

        int dataSize = chunkSize - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE;
        totalChunksReceived++;

        buf.rewind();

        try {
            if (remainBytes <= dataSize) {
                dataSize = (int) remainBytes;
                finished = true;
            }

            if (finished) {
                mAppleRecvStateChangeHack_isXfrCtrlExpected = true;
                mListener.onXfrRecvStateChange(xfrId, true);
                dbgTrace("saveAppleChunk: Core xfr mode switched to xfr control");
            }

            thisStream.write(buf.array(), MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE, dataSize);

            // calculate checksum
            digester.update(buf.array(), MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE, dataSize);

            if (finished) {
                thisStream.close();
                dbgTrace("saveAppleChunk: All chunks received just now: " + desc + ", Total chunks: " + totalChunksReceived);
            }
            remainBytes -= dataSize;

        } catch (Exception ex) {
            dbgTrace("saveAppleChunk: Exception while saving chunk: " + ex.getMessage());

            try {
                thisStream.close();
            } catch (Exception e) {
                dbgTrace("saveAppleChunk: Exception during close: " + e.getMessage());
            }

            if (!thisFile.delete()) {
                dbgTrace("saveAppleChunk: Could not delete file!");
            }

            finished = true;
            return AppConstants.FILE_XFR_ERROR_FILEWRITE;
        }

        return AppConstants.SUCCESS;
    }


    @SuppressLint("HandlerLeak")
    public void run() {
        Looper.prepare();

        // looper prepare will take some time to finish.
        // carefully read comments below below.
        synchronized (this) {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.arg1) {
                        case MSG_TYPE_UPDATE: {
                            FileReceiverV2CtrlMsg ctrlMsg = (FileReceiverV2CtrlMsg) msg.obj;
                            update(ctrlMsg.fName);
                            break;
                        }
                        case MSG_TYPE_SEND_RTR: {
                            dbgTrace("Sending RTR");
                            if (!sendXfrMessage(MMPV2Constants.MMP_FILE_RECV_XFR_REQUEST)) {
                                finished = true;
                                try {
                                    if (null != thisStream) thisStream.close();
                                } catch (Exception e) {
                                    dbgTrace("Close file exception: " + e.getMessage());
                                }
                                mListener.onXfrError(xfrId, null, AppConstants.FILE_XFR_ERROR_USBWRITE, mResponseCallback);
                                Looper.myLooper().quit();
                            }
                            break;
                        }
                        case MSG_TYPE_PREPARE: {
                            FileReceiverV2CtrlMsg ctrlMsg = (FileReceiverV2CtrlMsg) msg.obj;
                            dbgTrace("Preparing to receive file with size: " + ctrlMsg.fSize + ", cSum: " + ctrlMsg.chkSum);

                            if (0 != prepare(ctrlMsg.fSize, ctrlMsg.chkSum)) {
                                fileError = true;
                            }

                            break;
                        }
                        case MSG_TYPE_DATA: {
                            if (mPlatform == MNetConstants.NW_TCP_PLATFORM_CODE_IOS) {
                                // Now, this is a crappy area. Because of so and so many constraints, fw is handling iOS very crudely.
                                // We are not touching android code (that is why I moved it to the 'else' part) while handling both
                                // in this class.
                                ByteBuffer buf = (ByteBuffer) msg.obj;
                                handleAppleChunk(buf);
                                break;
                            } else { // legacy code to handle chunk from Android
                                // TODO: arun: 19June2017: This is absolute hack! This must be MSG_TYPE_FW_SAYS_SUCCESS
                                // TODO: which we missed due to race condition with usb reader thread. see comment below and in saveChunk().
                                if (finished) {
                                    dbgTrace("HACK1: We have already received all data. So lets call xfr completion/error and die!");
                                    if (!fileError) {
                                        mListener.onXfrCompletion(xfrId, fPath, mResponseCallback);
                                    } else {
                                        mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_FILEWRITE, mResponseCallback);
                                    }
                                    Looper.myLooper().quit();
                                    return;
                                }

                                if (fileError || abortReq) {
                                    dbgTrace("Data received while: fileError: " + fileError + ", abortReq: " + abortReq);
                                }

                                ByteBuffer buf = (ByteBuffer) msg.obj;
                                int result = saveChunk(buf);

                                if (result != AppConstants.SUCCESS) {
                                    dbgTrace("Saving data chunk failed! This will cause MMP_FILE_XFR_ERROR being sent in next status interval.");

                                    if (!finished) {
                                        fileError = true;
                                    } else {
                                        // TODO: arun: 09June2017: This is absolute hack! This must be MSG_TYPE_FW_SAYS_SUCCESS
                                        // TODO: which we missed due to race condition with usb reader thread. see comment in saveChunk().
                                        dbgTrace("HACK2: We have already received all data. So lets call xfr completion/error and die!");
                                        if (!fileError) {
                                            mListener.onXfrCompletion(xfrId, fPath, mResponseCallback);
                                        } else {
                                            mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_FILEWRITE, mResponseCallback);
                                        }
                                        Looper.myLooper().quit();
                                        return;
                                    }

                                    break;
                                }

                                statusIntervalCounter++;

                                if (statusIntervalCounter == MMPV2Constants.MMP_FILE_XFR_STATUS_INTERVAL) {
                                    if (abortReq) {
                                        dbgTrace("Status interval: Sending MMP_FILE_XFR_ABORT for xfrid: " + xfrId);
                                        mListener.onXfrRecvStateChange(xfrId, true); // This is ugly
                                        sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ABORT);
                                    } else if (fileError) {
                                        dbgTrace("Status interval: Sending MMP_FILE_XFR_ERROR for xfrid: " + xfrId);
                                        mListener.onXfrRecvStateChange(xfrId, true); // This is ugly
                                        sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
                                    } else {
                                        dbgTrace("Status interval: MMP_FILE_XFR_CONTINUE for xfrid: " + xfrId);
                                        sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_CONTINUE);
                                    }

                                    statusIntervalCounter = 0;
                                }
                                break;
                            } // handle Android chunk
                        }
                        case MSG_TYPE_FW_SAYS_SUCCESS: {
                            if (finished) {
                                FileReceiverV2CtrlMsg ctrlMsg = (FileReceiverV2CtrlMsg) msg.obj;
                                // TODO: 10May2017: We may comment this out as per last few xfr changes
                                chkSum = ctrlMsg.chkSum;

                                dbgTrace("XFR_SUCCESS receveid from firmware, csum is: " + chkSum);

                                // calculate checksum and report status to firmware
                                byte[] digest = digester.digest();
                                StringBuilder sb = new StringBuilder();
                                for (byte b : digest) {
                                    sb.append(String.format("%02x", b & 0xff));
                                }

                                boolean allOK = false;
                                if (!chkSum.equals(sb.toString())) {
                                    // XXX: HACK for fw
                                    if (fileSize < AppConstants.MAX_FILE_SIZE_4GB) {
                                        dbgTrace("Error: Checksum mismatch for: " + fPath + " Checksum calculated: " + sb.toString());
                                        sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
                                    } else {
                                        dbgTrace("Ignoring checksum mismatch for >4GB file: " + fPath);
                                        allOK = true;
                                    }
                                } else {
                                    dbgTrace("Checksum matched for received file: " + fPath);
                                    allOK = true;
                                }

                                try {
                                    if (null != thisStream) thisStream.close();
                                } catch (Exception e) {
                                    dbgTrace("Close file exception: " + e.getMessage());
                                }

                                if (allOK) {
                                    dbgTrace("Succcessfully received: " + desc);
                                    mListener.onXfrCompletion(xfrId, fPath, mResponseCallback);
                                } else {
                                    mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_RECV_CHECKSUM, mResponseCallback);
                                }
                                Looper.myLooper().quit();

                                return;
                            } else {
                                dbgTrace("BUG: FW says it has sent the file successfully, but we are sure it is not finished yet!");
                            }

                            break;
                        }
                        case MSG_TYPE_STOP: {
                            dbgTrace("FileReceiver: ABORT request for: " + desc + ", current status interval count: " + statusIntervalCounter);
                            abortReq = true;

                            int msgArg = msg.arg2;

                            // caller wants us to quit immediately without caring for xfr protocol (notify abort and weait for 'aborted')
                            if (msgArg == MSG_ARG_DIE_NOW) {
                                dbgTrace("Sudden death demanded with abort (cable disconnected, mostly). Dying now without notifying firmware.");
                                closeAndDeleteAbortedFile();
                                mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_ABORT, mResponseCallback);
                                Looper.myLooper().quit();
                                return;
                            }

                            dbgTrace("Will send MMP_FILE_XFR_ABORT in next status update");
                            break;
                        }
                        case MSG_TYPE_FW_ACK_ABORT: {
                            dbgTrace("MMP_FILE_XFR_ABORTED received for : " + desc);
                            closeAndDeleteAbortedFile();
                            mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_ABORT, mResponseCallback);
                            Looper.myLooper().quit();
                            return;
                        }
                        default: {
                            dbgTrace("Unknown receiver control command from meem core: " + msg.arg1);
                            break;
                        }
                    } // switch
                } // handle message
            }; // new handler

            // looper prepare will take some time to finish.
            // see getSafeHandler function below.
            notifyAll();
        } // synchronized
        Looper.loop();
    }

    /**
     * <outdated>Unfortunately, FW is dealing with USB xfr in a different way for Android and iOS. The difference is mainly in the case of
     * sending ACK (which is required to implement abort feature). Also, Apple is host for meem cable. So, if firmware wants to stop sending
     * and wait for ACK, it must send a short packet to indicate the end of transfer. So, 256th packet from firmware is a dummy short
     * packet, which we must ignore, and, send the ack at that time.</outdated>

     * Update: 18Oct2017: The protocol is changed again for iOS. Now all packets are 32K between cable and iOS. So previous dilemmas of
     * sending short packet does not exists anymore. So, this method has become exactly like that of Android.
     *
     * @param buf
     */
    private void handleAppleChunk(ByteBuffer buf) {
        //dbgTrace("handleAppleChunk: Total chunks (excluding this): " + totalChunksReceived + ", Status counter: " + statusIntervalCounter);

        int result = saveAppleChunk(buf);

        if (result != AppConstants.SUCCESS) {
            dbgTrace("handleAppleChunk: Saving data chunk failed! This will cause MMP_FILE_XFR_ERROR being sent in next status interval.");

            if (!finished) {
                fileError = true;
            } else {
                dbgTrace("handleAppleChunk: HACK2: We have already received all data. So lets call xfr completion/error and die!");
                if (!fileError) {
                    mListener.onXfrCompletion(xfrId, fPath, mResponseCallback);
                } else {
                    mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_FILEWRITE, mResponseCallback);
                }

                Looper.myLooper().quit();
                return;
            }
        }

        statusIntervalCounter++;
        //dbgTrace("handleAppleChunk: Status counter incremented to: " + statusIntervalCounter);

        if (statusIntervalCounter == MMPV2Constants.MMP_FILE_XFR_STATUS_INTERVAL) {
            if (abortReq) {
                dbgTrace("handleAppleChunk: Status interval 255: Sending MMP_FILE_XFR_ABORT for xfrid: " + xfrId);
                mListener.onXfrRecvStateChange(xfrId, true); // This is ugly
                sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ABORT);
            } else if (fileError) {
                dbgTrace("handleAppleChunk: Status interval 255: Sending MMP_FILE_XFR_ERROR for xfrid: " + xfrId);
                mListener.onXfrRecvStateChange(xfrId, true); // This is ugly
                sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
            } else {
                dbgTrace("handleAppleChunk: Status interval 255: MMP_FILE_XFR_CONTINUE for xfrid: " + xfrId);
                sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_CONTINUE);
            }

            statusIntervalCounter = 0;
            dbgTrace("Status counter reset to 0");
        }
    }

    // note: users must get handler only by this interface
    // to handle race conditions with looper.start
    synchronized Handler getSafeHandler() {
        while (mHandler == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                // Ignore and try again.
            }
        }
        return mHandler;
    }

    // send XFR READY/ERROR/ABORT/SUCCESS/FAILURE messages
    private boolean sendXfrMessage(byte code) {
        dbgTrace();

        ByteBuffer xfrMsg = ByteBuffer.allocate(mListener.getPayloadSize());
        xfrMsg.put(code);
        xfrMsg.put(xfrId);

        if (code == MMPV2Constants.MMP_FILE_XFR_ERROR) {
            xfrMsg.putInt(errCode);
        } else if (code == MMPV2Constants.MMP_FILE_RECV_XFR_REQUEST) {
            xfrMsg.put((byte) upid.length());
            xfrMsg.put(upid.getBytes());
            xfrMsg.put(fType);
            xfrMsg.put(fMode);
            xfrMsg.put(catCode);
            xfrMsg.putShort((short) fPath.getBytes().length);
            xfrMsg.put(fPath.getBytes());

            // Arun: 10May2017: final change in xfr (hopefully!)
            xfrMsg.put((byte) meemPath.length());
            xfrMsg.put(meemPath.getBytes());
            xfrMsg.put((byte) chkSum.length());
            xfrMsg.put(chkSum.getBytes());
        }

        return mListener.sendXfrCtrlData(xfrMsg.array());
    }

    private void closeAndDeleteAbortedFile() {
        dbgTrace();

        try {
            if (null != thisStream) thisStream.close();
        } catch (Exception e) {
            dbgTrace("Close file exception: " + e.getMessage());
        }

        if (null != thisFile) {
            if (!thisFile.delete()) {
                dbgTrace("Unable to delete MMP_FILE_XFR_ABORT'ed file: " + desc);
            }
        }
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(TAG, trace);
        GenUtils.logMessageToFile("FileReceiverV2.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
