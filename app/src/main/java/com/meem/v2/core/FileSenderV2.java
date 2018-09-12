package com.meem.v2.core;

import com.meem.androidapp.AppConstants;
import com.meem.events.ResponseCallback;
import com.meem.utils.GenUtils;
import com.meem.utils.VersionString;
import com.meem.v2.mmp.MMPV2Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import meem.org.apache.commons.lang3.mutable.MutableBoolean;

import static com.meem.v2.mmp.MMPV2Constants.MMP_FILE_XFR_TYPE_DATA;
import static com.meem.v2.mmp.MMPV2Constants.MMP_FILE_XFR_TYPE_FW_BINARY;

/**
 * @author Arun T A
 */

class FileSenderV2 extends Thread {
    private static final String TAG = "FileSenderV2";
    private static final Object mXfrSuccessMonitor = new Object();
    private String upid, fPath, desc, uniqName, chkSum;
    private BufferedInputStream thisStream;
    private ByteBuffer pkt;
    private long currChunk = 1;
    private long totChunks = 0;
    private long chunkSize = 0;
    private long fSize = 0;
    private long rowId = 0;
    private byte xfrId, fType, fMode, catCode;
    private boolean mAbortReqFlag, mXfrAbortedFlag;
    private int statusIntervalCounter = 0;
    private boolean mDieNow; // :)
    private boolean mXfrSuccessFlag = false;

    // For MD5 hash, which can take a long time.
    private MutableBoolean mAbortFlagObject;

    private FileXfrV2Listener mListener;
    private ResponseCallback mResponseCallback;

    FileSenderV2(FileXfrV2Listener core) {
        mListener = core;
        mAbortReqFlag = false;
        mAbortFlagObject = new MutableBoolean(false);
    }

    public void init(byte xfrId, String upid, byte fType, byte fMode, byte catCode, String fName, String uniqName, String cSum, long rowid, int chunkSize, ResponseCallback uicb) {
        dbgTrace();

        this.xfrId = xfrId;
        this.upid = upid;
        this.fType = fType;
        this.fMode = fMode;
        this.catCode = catCode;
        this.fPath = fName;
        this.chunkSize = chunkSize;
        this.mResponseCallback = uicb;

        this.uniqName = uniqName;
        this.chkSum = cSum;
        this.rowId = rowid;
    }

    // update file path from multi part message
    public void update(String fName) {
        fPath += fName;
    }

    public int prepare() {
        dbgTrace();

        try {
            File thisFile = new File(fPath);

            // 21Aug2015: See same dated fix below.
            /*
             * When a file of size greater than 2GB is read using
             * BufferedInputStream using its default buffer size of 8194 bytes,
             * the read will return less than requested bytes when stream buffer
             * has less than requested bytes. E.g. if MMP packet size is 2048,
             * payload size will be 2048 - HeaderSize = 2045, where HeaderSize
             * is the XFR header size. Then 5th read will return only 12 bytes
             * and thats it. So, instead of going for slightly complicated logic
             * of reading multiple times for same chunk, the InputBufferStream
             * is constructed with a buffer of size which is an integral
             * multiple of the exact payload size.
             */
            final int inputStreamBufferSize = (mListener.getPayloadSize() - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE)
                    * MMPV2Constants.MMP_XFR_FILE_BUFFERED_STREAM_READ_SIZE_MULTIPLIER;
            thisStream = new BufferedInputStream(new FileInputStream(thisFile), inputStreamBufferSize);

            currChunk = 1;

            fSize = thisFile.length();

            desc = fPath + ", size: " + String.valueOf(fSize) + ", xfrid: " + String.valueOf(xfrId) + ", chkSum: " + chkSum;
            this.setName(desc);

            totChunks = fSize / (chunkSize - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE);
            if (fSize % (chunkSize - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE) != 0) {
                totChunks++;
            }

            pkt = ByteBuffer.allocate(mListener.getPayloadSize());
        } catch (Exception ex) {
            dbgTrace("Unable to get ready for transfer: " + desc + ": Exception: " + ex.getMessage());
            try {
                if (null != thisStream) thisStream.close();
            } catch (Exception e) {
                dbgTrace("Close file exception: " + e.getMessage());
            }
            mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_FILEOPEN, mResponseCallback);
            return -1;
        }

        mAbortReqFlag = false;
        dbgTrace("Sending XFR RTS: " + desc);
        sendXfrMessage(MMPV2Constants.MMP_FILE_SEND_XFR_REQUEST);
        return 0;
    }

    public void run() {
        dbgTrace("run: sending: " + fPath);

        final int dataPayloadSize = mListener.getPayloadSize() - MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE;

        try {
            while (currChunk <= totChunks) {

                if (mDieNow || (mAbortReqFlag && mXfrAbortedFlag)) {
                    dbgTrace("Exiting thead, because: die: " + mDieNow + ", abortReq: " + mAbortReqFlag + ", abortAcked: " + mXfrAbortedFlag);

                    try {
                        if (null != thisStream) thisStream.close();
                    } catch (Exception e) {
                        dbgTrace("Close file exception: " + e.getMessage());
                    }
                    mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_ABORT, mResponseCallback);
                    break;
                }

                if (statusIntervalCounter == MMPV2Constants.MMP_FILE_XFR_STATUS_INTERVAL) {
                    // Must read the comment on top of this method. No data must be sent if abort is sent.
                    if (!checkAndProcessAbortRequests()) {
                        continue;
                    }

                    statusIntervalCounter = 0;
                }

                pkt.clear();

                // 21Aug2015: Must do this. See same dated comment above.
                int ret;
                if (dataPayloadSize != (ret = thisStream.read(pkt.array(), MMPV2Constants.MMP_FILE_XFR_PKT_HEADER_SIZE, dataPayloadSize))) {
                    if (0 != thisStream.available()) {
                        String msg = "BUG: Incomplete read: " + ret + "bytes for xfr chunk: " + currChunk + " for: " + desc;
                        dbgTrace(msg);
                        throw new IllegalStateException(msg);
                    }
                }

                if (!mListener.sendXfrData(pkt.array())) {
                    dbgTrace("Send stopping on critical usb error: " + desc);
                    try {
                        if (null != thisStream) thisStream.close();
                    } catch (Exception e) {
                        dbgTrace("Close file exception: " + e.getMessage());
                    }
                    mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_USBWRITE, mResponseCallback);
                    return;
                }

                currChunk++;
                statusIntervalCounter++;
            }

            dbgTrace("Sent all chunks: " + totChunks + ". Waiting for xfr success from fw");

            synchronized (mXfrSuccessMonitor) {
                while (!mXfrSuccessFlag) {
                    try {
                        mXfrSuccessMonitor.wait();
                    } catch (Exception e) {
                        dbgTrace("Wait exception while waiting for xfr succeeded: " + desc);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            dbgTrace("Exception during file send\n" + ex.getMessage());
            sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ERROR);
            try {
                if (null != thisStream) thisStream.close();
            } catch (Exception e) {
                dbgTrace("Close file exception: " + e.getMessage());
            }
            mListener.onXfrError(xfrId, fPath, AppConstants.FILE_XFR_ERROR_FILEREAD, mResponseCallback);
            return;
        }

        dbgTrace("Finished sending file: " + desc);

        try {
            thisStream.close();
        } catch (Exception ex) {
            dbgTrace("Exception during file send cleanup: " + desc + ": " + ex.getMessage());
        }

        try {
            if (null != thisStream) thisStream.close();
        } catch (Exception e) {
            dbgTrace("Close file exception: " + e.getMessage());
        }

        mListener.onXfrCompletion(xfrId, fPath, mResponseCallback);
    }

    void stopSender() {
        dbgTrace();
        mAbortReqFlag = true;
        mAbortFlagObject.setTrue();
    }

    /**
     * Should be called
     */
    void dieNow() {
        dbgTrace();

        mDieNow = true;
        mAbortReqFlag = true;
        mAbortFlagObject.setTrue();
        synchronized (mXfrSuccessMonitor) {
            mXfrSuccessFlag = true;
            mXfrSuccessMonitor.notifyAll();
        }
    }

    void notifyXfrAbortAckedByFw() {
        dbgTrace();
        mXfrAbortedFlag = true;
    }

    void notifyXfrSuccessByFw() {
        dbgTrace();

        synchronized (mXfrSuccessMonitor) {
            mXfrSuccessFlag = true;
            mXfrSuccessMonitor.notifyAll();
        }
    }

    // send XFR REQUEST/ERROR/ABORT messages
    private boolean sendXfrMessage(byte code) {
        dbgTrace();

        ByteBuffer xfrMsg = ByteBuffer.allocate(mListener.getPayloadSize());
        xfrMsg.order(ByteOrder.BIG_ENDIAN);

        xfrMsg.put(code);
        xfrMsg.put(xfrId);

        if (code == MMPV2Constants.MMP_FILE_XFR_ERROR) {
            xfrMsg.putInt(AppConstants.FILE_XFR_ERROR_FILEOPEN);
        } else if (code == MMPV2Constants.MMP_FILE_SEND_XFR_REQUEST) {
            xfrMsg.put((byte) upid.length());
            xfrMsg.put(upid.getBytes());
            xfrMsg.put(fType);
            xfrMsg.put(fMode);
            xfrMsg.put(catCode);
            xfrMsg.putLong(fSize);
            xfrMsg.putShort((short) fPath.getBytes().length);
            xfrMsg.put(fPath.getBytes());
            xfrMsg.put((byte) uniqName.length());
            xfrMsg.put(uniqName.getBytes());
            xfrMsg.put((byte) chkSum.length());
            xfrMsg.put(chkSum.getBytes());

            if (fType == MMP_FILE_XFR_TYPE_FW_BINARY) {
                String fName = fPath.substring(fPath.lastIndexOf('/'));
                int idx1 = fName.indexOf('_') + 1;
                int idx2 = fName.lastIndexOf('_');
                String ver = fName.substring(idx1, idx2);

                dbgTrace("Updating fw/bl version: " + ver);

                VersionString vs = new VersionString(ver);

                xfrMsg.put((byte) vs.major());
                xfrMsg.put((byte) vs.minor());
                xfrMsg.putShort((short) vs.build());
                xfrMsg.putShort((short) vs.trial());
            }

            if (fType == MMP_FILE_XFR_TYPE_DATA) {
                xfrMsg.putInt((int) rowId);
            }
        }

        return mListener.sendXfrCtrlData(xfrMsg.array());
    }

    private boolean checkAndProcessAbortRequests() {
        dbgTrace();

        if (mAbortReqFlag) {
            dbgTrace("Sending XFR_ABORT for: " + desc);
            sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_ABORT);

            // XXX: If abort request is sent, then we must NOT send any data packet (fw freezes).
            dbgTrace("Waiting for fw ack for abort");
            while (!mXfrAbortedFlag && !mDieNow) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    // nothing
                }
            }
            dbgTrace("Got fw ack for abort");
            return false;
        } else {
            sendXfrMessage(MMPV2Constants.MMP_FILE_XFR_CONTINUE);
            return true;
        }
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(TAG, trace);
        GenUtils.logMessageToFile("FileSenderV2.log", trace);
    }

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
    }
}
