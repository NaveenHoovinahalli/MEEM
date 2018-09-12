package com.meem.v2.core;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.v2.mmp.MMPV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 20/4/17.
 */

abstract public class MeemCoreV2Request extends Thread {
    static final int MSG_TYPE_MMP = 0x01;

    static final int MSG_TYPE_CTR = 0x02;
    static final int CTR_CODE_TERMINATE = 0xFF;
    static final int CTR_CODE_NOTIFY_ABORT = 0xAA;
    protected UiContext mUiCtxt = UiContext.getInstance();
    protected String mName = "MeemCoreV2Request";
    protected int mCmdCode, mRespCode;
    protected ResponseCallback mResponseCallback;
    private CountDownTimer mTimer;
    private Handler mHandler;
    private Runnable mOnFinish;

    public MeemCoreV2Request(String name, int cmdCode, ResponseCallback responseCallback) {
        super();

        mTimer = null;
        if (null != name) {
            mName = name;
            super.setName(mName);
        }

        mCmdCode = cmdCode;
        mResponseCallback = responseCallback;
    }

    /**
     * This will be called when this thread terminates. As poer design, this is always used by MeemCoreV2Handler
     *
     * @param onFinish
     */
    public void setFinalizer(Runnable onFinish) {
        mOnFinish = onFinish;
    }

    /**
     * This function is to be used by MeemCore handler to post an MMP message to this sequence to advance the sequence by a step. This
     * function is completely thread-safe - as this will be called by an anonymous thread created by MeemCore upon reception of a control
     * message.
     *
     * @param mmpMsg The MMPV2 Message - be a command or response
     *
     * @return boolean.
     */
    final public boolean process(MMPV2 mmpMsg) {
        selfCheck();

        Handler h = getSafeHandler();
        Message sysMsg = h.obtainMessage();
        sysMsg.arg1 = MSG_TYPE_MMP;
        sysMsg.obj = mmpMsg;
        return h.sendMessage(sysMsg);
    }

    /**
     * This synchronous function must be the only one to be used to stop and terminate a sequence object. Even though this is a thread
     * object, stop/interrupt etc shall not be used as they are buttercups for bugs.
     *
     * @param reason The reason because of which the sequence is terminated. This parameter may be used in MMP messages if its applicable.
     *
     * @return boolean
     */
    final public boolean terminate(int reason) {
        selfCheck();

        Handler h = getSafeHandler();
        Message sysMsg = h.obtainMessage();
        sysMsg.arg1 = MSG_TYPE_CTR;
        sysMsg.arg2 = CTR_CODE_TERMINATE;
        return h.sendMessage(sysMsg);
    }

    /**
     * This method shall be called (usually from main thread) to make MMP handlers handle asynchronous aborts requested by the user. At
     * present (17sept2014) only SESSION_ABORT command is defined in MMP and hence only ExecuteSession MMP handler needs to handle the
     * CTR_CODE_NOTIFYABORT command.
     *
     * @return No one cares now. Recommended to return true if handled.
     */
    final public boolean notifyAbortRequest() {
        selfCheck();

        Handler h = getSafeHandler();
        Message sysMsg = h.obtainMessage();
        sysMsg.arg1 = MSG_TYPE_CTR;
        sysMsg.arg2 = CTR_CODE_NOTIFY_ABORT;
        return h.sendMessage(sysMsg);
    }

    /**
     * Internal function used in caller's thread context to get handler of this thread
     *
     * @return Internal handler.
     */
    private synchronized Handler getSafeHandler() {
        selfCheck();

        while (mHandler == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                // Ignore and try again.
            }
        }
        return mHandler;
    }

    /**
     * Function to start the sequence, before entering into run loop. All MMP level error handling is done internally through listener
     *
     * @return boolean.
     */
    protected boolean kickStart() {
        return MeemCoreV2.getInstance().acquireCable(mUiCtxt.getPhoneUpid(), null);
    }

    /**
     * Function to process MMP message. All MMP level error handling is done internally through events. The return value of this function
     * determines whether the handler is complete or not. If a particular implementation has more commands/responses/actions to do, then it
     * must return true. If this function returns false, the handler thread quits.
     *
     * @param msg The MMP control message
     *
     * @return boolean. See documentation of the function.
     */
    protected boolean onMMPMessage(MMPV2 msg) {
        return true;
    }

    /**
     * Function to handle timeout. All MMP control path objects must implement this. If this function returns false, the handler quits.
     */
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Request: " + mName + ": TIMEOUT for: " + mCmdCode);
        return true;
    }

    /**
     * This function will get invoked when the requested XFR is completed successfully. Override if needed. Anyway, the control path will
     * take care of the status of file transfer (if any).
     *
     * @param path The file that completely transferred (can be null)
     *
     * @return void
     */
    protected void onXfrCompletion(String path) {
        mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Request: " + mName + ": XFR completed: " + ((path == null) ? "null!" : path));
    }

    /**
     * This function will be invoked on transfer error of the specified file. Override if needed. Anyway, the control path will take care of
     * the status of file transfer (if any).
     *
     * @param path   The file whose transfer resulted in error (can be null)
     * @param status The status code describing error
     *
     * @return void
     */
    protected void onXfrError(String path, int status) {
        mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Request: " + mName + " encountered XFR error: " + ((path == null) ? "null!" : path) + ": " + status);
    }

    /**
     * Function will be invoked on any exceptions raised during execution. Override if needed.
     *
     * @param ex The exception.
     *
     * @return void
     */
    protected void onException(Throwable ex) {
        MeemEvent evt = new MeemEvent(EventCode.CRITICAL_ERROR);
        String info = "MeemCoreV2Request: " + mName + " encountered MeemCore exception: " + ex.getMessage();
        evt.setInfo(info);
        mUiCtxt.postEvent(evt);

        terminate(-1);
    }

    /**
     * MMP handlers can override this method to implement their on abort notification handling.
     *
     * @return don't care.
     */
    protected boolean onAbortNotification() {
        return false;
    }

    /**
     * Arun: Abortfix 22May2015
     * <p/>
     * Currently this method is overridden only by ExecuteSession to handle a user requested abort.
     *
     * @return true to continue with file xfr.
     */
    protected boolean onXfrRequest() {
        return true;
    }

    // MeemCoreV2Request functions shall never be invoked from self thread context.
    // It may even cause deadlock on onException cases.
    private void selfCheck() {
        if (this == currentThread()) {
            // throw new IllegalStateException("MeemCoreV2Request public functions shall never be invoked from self. It will deadlock.");
            postLog(UiContext.WARNING, "MeemCoreV2Request public functions shall never be invoked from self. It may deadlock.");
        }
    }

    public void postResult(boolean result, int respCode, Object info, Object extraInfo) {
        MeemEvent event = new MeemEvent(EventCode.MMP_RESPONSE_FROM_BACKEND);
        event.setResult(result);
        event.setArg0(mCmdCode);
        event.setArg1(respCode);
        event.setInfo(info);
        event.setExtraInfo(extraInfo);
        event.setResponseCallback(mResponseCallback);

        mUiCtxt.postEvent(event);
    }

    public void postXfrResult(String path, boolean isSend, boolean result) {
        EventCode code;

        if (isSend) {
            code = result ? EventCode.FILE_SEND_SUCCEEDED : EventCode.FILE_SEND_FAILED;
        } else {
            code = result ? EventCode.FILE_RECEIVE_SUCCEEDED : EventCode.FILE_RECEIVE_FAILED;
        }
        MeemEvent event = new MeemEvent(code, (path != null) ? path : "filenotopenedyet", mResponseCallback);
        event.setResult(result);

        mUiCtxt.postEvent(event);
    }

    public void postLog(int type, String msg) {
        mUiCtxt.log(type, msg);
    }

    /**
     * This function will first setup the looper and handler for this thread object. Then it will call the kickStart abstract function so
     * that derived sequences has a chance to send a command (if needed) to start the real sequence.
     * <p/>
     * The timeout mechanism implemented gives one chance to decide for the derived handlers to quite or ignore timeouts. See onMMPTimeout's
     * return value.
     */
    @SuppressLint("HandlerLeak")
    @Override
    final public void run() {
        Looper.prepare();

        synchronized (this) {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.arg1 == MSG_TYPE_CTR) {
                        switch (msg.arg2) {
                            case CTR_CODE_TERMINATE:
                                mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Request: " + mName + ": is getting terminated");

                                if (null != mOnFinish) {
                                    mOnFinish.run();
                                }
                                if (null != mTimer) {
                                    mTimer.cancel();
                                }

                                Looper.myLooper().quit();
                                break;
                            case CTR_CODE_NOTIFY_ABORT:
                                onAbortNotification();
                                break;
                            default:
                                throw new RuntimeException("MMPHandler: Invalid control message code: " + msg.arg2);
                        }
                    } else {
                        if (false == onMMPMessage((MMPV2) msg.obj)) {
                            if (null != mOnFinish) {
                                mOnFinish.run();
                            }
                            if (null != mTimer) {
                                mTimer.cancel();
                            }

                            mUiCtxt.log(UiContext.DEBUG, "MMPHandler: " + mName + ": Finished gracefully");
                            Looper.myLooper().quit();
                        }
                    }
                } // handle message
            }; // new handler

            // looper prepare will take some time to finish.
            // see getSafeHandler function below.
            notifyAll();
        } // synchronized

        // Arun: 01June2015: Moved this to be after the handler creation so that any exceptions
        // that may caused by kickStart implementations need the handler in their callbacks.
        if (false == kickStart()) {
            mUiCtxt.log(UiContext.DEBUG, "MMPHandler: " + mName + ": Kickstart returns negative");
            if (null != mOnFinish) {
                mOnFinish.run();
                Looper.myLooper().quit();
            }

            return; // Arun: Added on 14Aug2017: To get rid of warnings about sending messages on a dead thread.
        }

        // everything is fine so far. start timer and loop-ho!
        mTimer = new CountDownTimer(ProductSpecs.MMP_CTRL_COMMAND_TIMEOUT_MS, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (null != mOnFinish) {
                    if (false == onMMPTimeout()) {
                        mOnFinish.run();
                        Looper.myLooper().quit();
                        return;
                    }
                }
            }

        }.start();

        Looper.loop();
    }

    @Override
    public String toString() {
        return mName;
    }

    /**
     * Arun: 26June2017: This is added for Meem Network.
     * For all legacy derived classes, this shall NOT be overridden and must return false;
     *
     * Will be overridden and retirned true by remote requests, implemented for meem network.
     */
    protected boolean onDataMessage(ByteBuffer data) {
        return false;
    }
}
