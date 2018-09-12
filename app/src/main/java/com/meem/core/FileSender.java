package com.meem.core;

import android.util.Log;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.messages.MMPConstants;
import com.meem.utils.GenUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

/**
 * @author Arun T A
 */

class FileSender extends Thread {
    private static final String tag = "FileSender";
    UiContext mUiCtxt = UiContext.getInstance();
    MeemCore mCore;
    private String fPath, desc;
    private File thisFile;
    private BufferedInputStream thisStream;
    private ByteBuffer pkt;
    private long currChunk = 1;
    private long totChunks = 0;
    private long chunkSize;
    private byte xfrId;
    private long fSize = 0;
    private boolean mStopFlag;
    private int errCode = MMPConstants.MMP_ERROR_FILE_NOT_FOUND;

    FileSender(MeemCore core) {
        mCore = core;
        mStopFlag = false;
    }

    public void init(String fName, byte xfrId, long fSize, int chunkSize, byte xOfy) {
        this.xfrId = xfrId;
        fPath = fName;

        this.xfrId = xfrId;
        this.fPath = fName;

        this.chunkSize = chunkSize;
        this.fSize = fSize;
    }

    // update file path from multi part message
    public void update(String fName) {
        fPath += fName;
    }

    public int prepare() {
        try {
            desc = fPath + ", size: " + String.valueOf(fSize) + ", xfrid: " + String.valueOf(xfrId);
            this.setName(desc);

            thisFile = new File(fPath);

            // 21Aug2015: See same dated fix below.
            /**
             * When a file of size greater than 2GB is read using
             * BufferedInputStream using its default buffer size of 8194 bytes,
             * the read will return less than requested bytes when stream buffer
             * has less than requested bytes. E.g. if MMP packet size is 2048,
             * payload size will be 2048 - 3 = 2045, where 3 is the XFR header
             * size. Then 5th read will return only 12 bytes and thats it. So,
             * instead of going for slightly complicated logic of reading
             * multiple times for same chunk, the InputBufferStream is
             * constructed with a buffer of size which is an integral multiple
             * of the exact payload size.
             */
            final int inputStreamBufferSize = (MMPConstants.MMP_PKT_SIZE - 3) * MMPConstants.MMP_XFR_FILE_BUFFERED_STREAM_READ_SIZE_MULTIPLIER;
            thisStream = new BufferedInputStream(new FileInputStream(thisFile), inputStreamBufferSize);

            currChunk = 1;

            totChunks = fSize / (chunkSize - 3);
            if (thisFile.length() % (chunkSize - 3) != 0) {
                totChunks++;
            }

            pkt = ByteBuffer.allocate(MMPConstants.MMP_PKT_SIZE);
        } catch (Exception ex) {
            dbgTrace("Unable to get ready for transfer: " + desc + ": Exception: " + ex.getMessage());
            sendXfrMessage(MMPConstants.XFR_ERROR);
            mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_FILEOPEN);
            return -1;
        }

        mStopFlag = false;
        dbgTrace("Ready for sending: " + desc);
        sendXfrMessage(MMPConstants.XFR_READY);
        return 0;
    }

    public void run() {
        dbgTrace("Sending: " + fPath);

        final int dataPayloadSize = MMPConstants.MMP_PKT_SIZE - 3;

        try {
            while (currChunk <= totChunks) {
                if (mStopFlag) {
                    Log.w(tag, "Aborting file send");
                    mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_ABORT);
                    sendXfrMessage(MMPConstants.XFR_ABORT); // Arun: AbortFix 21May2015 was ABORTED
                    dbgTrace("XFR_ABORTED sent for XFRID: " + xfrId);
                    break;
                }

                pkt.clear();
                pkt.put(MMPConstants.XFR_FILEDATA);
                pkt.put(xfrId);
                pkt.put((byte) (currChunk & 0x000000FF));

                // 21Aug2015: Must do this. See same dated comment above.
                int ret = 0;
                if (dataPayloadSize != (ret = thisStream.read(pkt.array(), 3, dataPayloadSize))) {
                    if (0 != thisStream.available()) {
                        String msg = "BUG: Incomplete read: " + ret + "bytes for xfr chunk: " + currChunk + " for: " + desc;
                        dbgTrace(msg);
                        throw new IllegalStateException(msg);
                    }
                }

                if (true != mCore.writeUsb(pkt.array())) {
                    dbgTrace("Send stopping on critical usb error: " + desc);
                    mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_USBWRITE);
                    return;
                }
                currChunk++;
            }

            // Post a message to UI thread to let it know
            // TODO: Better way do this is MMPHandler via Core,
            // which implements MeemCoreListener interface.
            MeemEvent fileSent = new MeemEvent(EventCode.FILE_SENT_TO_MEEM, fPath);
            mUiCtxt.postEvent(fileSent);
        } catch (Exception ex) {
            dbgTrace("Exception during file send\n" + ex.getMessage());
            sendXfrMessage(MMPConstants.XFR_ERROR);
            mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_FILEREAD);
            return;
        }

        dbgTrace("Finished sending file: " + desc);

        try {
            thisStream.close();
        } catch (Exception ex) {
            dbgTrace("Exception during file send cleanup: " + desc + ": " + ex.getMessage());
        }
        mCore.onXfrCompletion(xfrId, thisFile);
        return;
    }

    public void stopSender() {
        mStopFlag = true;
    }

    // send XFR READY/ERROR/ABORT messages
    private boolean sendXfrMessage(byte code) {
        ByteBuffer xfrMsg = ByteBuffer.allocate(6);
        xfrMsg.put(code);
        xfrMsg.put(xfrId);

        if (code == MMPConstants.XFR_ERROR) {
            xfrMsg.putInt(errCode);
        }

        return mCore.writeUsb(xfrMsg.array());
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("FileSender.log", trace);
    }
}
