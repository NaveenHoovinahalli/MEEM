package com.meem.core;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.mmp.messages.MMPConstants;
import com.meem.utils.GenUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * This class is a thread with message queue which will receive data packets from meem core reader thread and write to intended file. You
 * must read MMP spec before trying to go through the code. Meem core object has special control messages to control this thread derived
 * object. See code of MeemCore class.
 *
 * @author Arun T A
 * @29July2015 After 2 years, major bug fix: Absolutely no operations are to be performed after we call XFR listener interfaces of meem core
 * object from here. This is because, onXFRCompletion and onXFRError interfaces of meem core will remove this thread object from its map and
 * after that all objects created by this receiver object will become eligible for GC at any point in time. This will create issues if meem
 * core calls further listeners passing these objects. This is fixed in Meem core by making sure that all listener interfaces are to be
 * called before removing this object from its internal map - but still, just to make the implementation perfect, here also the above rule
 * is followed - i.e. nothing shall be done but looper.quit after calling XFR listener interfaces of meem core object.
 */

class FileReceiver extends Thread {
    public static final int MSG_TYPE_INIT = 1;
    public static final int MSG_TYPE_UPDATE = 2;
    public static final int MSG_TYPE_PREPARE = 3;
    public static final int MSG_TYPE_DATA = 4;
    public static final int MSG_TYPE_STOP = 5;
    private static final String tag = "FileReceiver";
    boolean done = false;
    UiContext mUiCtxt = UiContext.getInstance();
    MessageDigest digester;
    private String desc, fileName;
    private File thisFile;
    private BufferedOutputStream thisStream;
    private byte xfrId;
    private byte expChunkIdx;
    private int chunkSize = 0;
    private long remainBytes = 0;
    private long fileSize = 0;
    private String chkSum;
    private int errCode = MMPConstants.MMP_ERROR_FILE_NOT_FOUND;
    private Handler mHandler;
    private MeemCore mCore;

    private boolean mIsCrossPlatform; // Arun: 15Dec2016: For XFR from iOS vault.

    FileReceiver(MeemCore core) {
        mCore = core;
    }

    @Override
    public String toString() {
        return "FileReceiver [fileName=" + fileName + ", thisFile=" + thisFile + ", xfrId=" + xfrId + ", chunkSize=" + chunkSize + ", remainBytes=" + remainBytes + ", chunkIdx= " + expChunkIdx + ", fileSize=" + fileSize + ", done=" + done + "]" + "\n";
    }

    private void init(String fName, byte xfrId, long fSize, int chSize, byte xOfy, String chkSum, boolean isCrossPlatform) {
        this.xfrId = xfrId;
        fileName = fName;
        chunkSize = chSize;
        fileSize = fSize;
        this.chkSum = chkSum;
        expChunkIdx = 1;
        mIsCrossPlatform = isCrossPlatform;

        if (mIsCrossPlatform) {
            if (GenUtils.hack__isPathOfPhoto(fName)) {
                fileSize -= 32;
            }
        }
    }

    private void update(String fName) {
        fileName += fName;
    }

    private int prepare() {
        try {
            // may happen if there is an unsupported phone which creates issues
            // with our filename hacks for secondary storage.
            if (fileName == null) {
                dbgTrace("BUG: filename is null in receiver");
                sendXfrMessage(MMPConstants.XFR_ERROR);
                return -1;
            }

            // create checksum calculator instance
            try {
                digester = MessageDigest.getInstance("MD5");
            } catch (Exception ex) {
                dbgTrace("ERROR: MD5 digest algorithm not available!");
                sendXfrMessage(MMPConstants.XFR_ERROR);
                return -1;
            }

            desc = fileName + ", size: " + String.valueOf(fileSize) + ", xfrid: " + String.valueOf(xfrId);
            this.setName(desc);

            thisFile = new File(fileName);
            if (!thisFile.exists()) {
                boolean strangeFilesystemBug = false;
                File parentDir = new File(thisFile.getParent());
                // Additional check for removable SDCARD related issues
                if (null != parentDir) {
                    if (!parentDir.exists()) {
                        if (false == parentDir.mkdirs()) {
                            dbgTrace("Directory tree creation problem for: " + fileName + ": Continuing without aborting.");
                        } else {
                            if (!parentDir.isDirectory()) {
                                dbgTrace("WTF: Parent directory created but it is not a directory where file: " + fileName + " was present.");
                                // See: http://codereply.com/answer/65fcjk/android-mkdirs-creates-zero-byte-file-instead-folder.html
                                strangeFilesystemBug = true;
                            } else {
                                dbgTrace("Directory tree created properly for: " + fileName);
                            }
                        }
                    }
                }
                // Arun: Possible bug identified and fixed on: 28July2015
                if (strangeFilesystemBug || false == thisFile.createNewFile()) {
                    dbgTrace("WTF: Could not create (non-existing) file: " + fileName);
                    sendXfrMessage(MMPConstants.XFR_ERROR);
                    return -1;
                }
            } else {
                String log = "Restoring an existing file: " + fileName;
                dbgTrace(log);
            }

            thisStream = new BufferedOutputStream(new FileOutputStream(thisFile));
            remainBytes = fileSize;
        } catch (Exception ex) {
            dbgTrace("Unable to prepare for reception: " + fileName);
            sendXfrMessage(MMPConstants.XFR_ERROR);
            thisFile.delete();
            done = true;
            return -1;
        }
        dbgTrace("Ready to receive: " + desc);
        sendXfrMessage(MMPConstants.XFR_READY);
        return 0;
    }

    // note: chunk size is presently used as 512.
    // it must have been (USB_BUFSIZE - 3). but well...
    private MeemCoreStatus saveChunk(ByteBuffer buf) {
        int dataSize = chunkSize - 3;

        try {
            if (remainBytes <= dataSize) {
                dataSize = (int) remainBytes;
                done = true;
            }

            thisStream.write(buf.array(), 3, dataSize);

            // calculate checksum
            digester.update(buf.array(), 3, dataSize);

            if (done) {
                thisStream.close();
                dbgTrace("Receive finished: " + desc);

                // calculate checksum and report status to firmware
                byte[] digest = digester.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b & 0xff));
                }

                if (!this.chkSum.equals(sb.toString()) && !mIsCrossPlatform) { // TODO: Hack for cross platform support.
                    dbgTrace("Error: Checksum mismatch for: " + this.fileName + " Checksum calculated: " + sb.toString());

                    // notify firmware
                    sendXfrMessage(MMPConstants.XFR_FAILURE);

                    return MeemCoreStatus.XFR_ERROR_RECV_CHECKSUM;
                } else {
                    dbgTrace("Checksum matched for received file: " + this.fileName);

                    // notify firmware
                    sendXfrMessage(MMPConstants.XFR_SUCCESS);

                    // Post a message to UI thread to let it know
                    // TODO: Better way do this is MMPHandler via Core,
                    // which implements MeemCoreListener interface.
                    MeemEvent fileRecv = new MeemEvent(EventCode.FILE_RECEIVED_FROM_MEEM, fileName);
                    mUiCtxt.postEvent(fileRecv);
                }
            }
            remainBytes -= dataSize;
        } catch (Exception ex) {
            dbgTrace("Failed to save: " + desc);
            sendXfrMessage(MMPConstants.XFR_ERROR);
            thisFile.delete();
            done = true;
            return MeemCoreStatus.XFR_ERROR_FILEWRITE;
        }

        return MeemCoreStatus.SUCCESS;
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
                        case MSG_TYPE_INIT: {
                            ReceiverCtrlMsg ctrlMsg = (ReceiverCtrlMsg) msg.obj;
                            init(ctrlMsg.fName, ctrlMsg.xfrId, ctrlMsg.fSize, ctrlMsg.chunkSize, ctrlMsg.xOfy, ctrlMsg.chkSum, ctrlMsg.isCrossPlat);
                            break;
                        }
                        case MSG_TYPE_UPDATE: {
                            ReceiverCtrlMsg ctrlMsg = (ReceiverCtrlMsg) msg.obj;
                            update(ctrlMsg.fName);
                            break;
                        }
                        case MSG_TYPE_PREPARE: {
                            if (0 != prepare()) {
                                done = true;
                                mCore.onXfrError(xfrId, null, MeemCoreStatus.XFR_ERROR_FILEOPEN);
                                Looper.myLooper().quit();
                            }
                            break;
                        }
                        case MSG_TYPE_DATA: {
                            ByteBuffer buf = (ByteBuffer) msg.obj;
                            if (xfrId != buf.get(1)) {
                                dbgTrace("Invalid XFRID: " + String.valueOf(buf.get(1)) + " for: " + desc);
                            } else {
                                // check expected chunk index
                                byte chunkIdx = buf.get(2);
                                if (expChunkIdx != chunkIdx) {
                                    dbgTrace("CRITICAL: DATA LOST: Received chunk index: " + chunkIdx + " expected: " + expChunkIdx + " for xfrid: " + xfrId);

                                    // We have lost data. Send failure to FW and bail out.
                                    sendXfrMessage(MMPConstants.XFR_FAILURE);
                                    thisFile.delete();

                                    // This mark is needed.
                                    done = true;

                                    mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_DATA_LOSS);
                                    Looper.myLooper().quit();
                                    return;
                                }

                                // save the data
                                MeemCoreStatus result = saveChunk(buf);

                                if (done) {
                                    if (result == MeemCoreStatus.SUCCESS) {
                                        mCore.onXfrCompletion(xfrId, thisFile);
                                    } else {
                                        mCore.onXfrError(xfrId, thisFile, result);
                                    }
                                    Looper.myLooper().quit();
                                    return;
                                }

                                // update next expected chunk index. not that
                                // this will wrap to 0 - but 1 is an expected
                                // value of the very first chunk index of the
                                // whole xfr of a file. see init().
                                expChunkIdx++;
                            }
                            break;
                        }
                        case MSG_TYPE_STOP: {
                            dbgTrace("FileReceiver: Stopping: " + desc);
                            try {
                                if (null != thisStream) {
                                    thisStream.close();
                                }
                            } catch (Exception ex) {
                                dbgTrace("Unable to close XFR_ABORT'ed file: " + thisFile.getAbsolutePath());
                            }

                            if (null != thisFile) {
                                if (false == thisFile.delete()) {
                                    dbgTrace("Unable to delete XFR_ABORT'ed file: " + thisFile.getAbsolutePath());
                                }
                            }

                            sendXfrMessage(MMPConstants.XFR_ABORT); // Arun: AbortFix 21May2015
                            dbgTrace("XFR_ABORT sent for XFRID: " + xfrId);
                            done = true;

                            mCore.onXfrError(xfrId, thisFile, MeemCoreStatus.XFR_ERROR_ABORT);
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

    // note: users must get handler only by this interface
    // to handle race conditions with looper.start
    public synchronized Handler getSafeHandler() {
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
        GenUtils.logMessageToFile("FileReceiver.log", trace);
    }

    @SuppressWarnings("unused")
    private void dbgTraceEx(String fName, String trace) {
        Log.d(tag, trace);
        GenUtils.logMessageToFile(fName + ".log", trace);
    }
}
