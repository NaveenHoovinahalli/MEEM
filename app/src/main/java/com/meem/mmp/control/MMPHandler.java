package com.meem.mmp.control;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.core.MeemCoreStatus;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPCtrlMsg;

import java.io.File;

/**
 * This class provides an abstraction of MMP message handling using a thread. The derived classes are supposed to implement the actual
 * command processing and event based notifications to the top layer modules.
 * <p/>
 * 04Aug2016: Arun: added response sending from here to reflect the changes in event mechanism in app version 2 design.
 *
 * @author Arun T A
 */
public abstract class MMPHandler extends Thread {
    static final int MSG_TYPE_MMP = 0x01;
    static final int MSG_TYPE_CTR = 0x02;
    static final int CTR_CODE_TERMINATE = 0xFF;
    static final int CTR_CODE_NOTIFY_ABORT = 0xAA;
    protected UiContext mUiCtxt = UiContext.getInstance();
    Handler mHandler;
    // In present design, this is invariably MeemCoreHandler.
    MMPHandlerListener mListener;
    CountDownTimer mTimer;
    private String mName = "MMPHandler";
    private int mCmdCode, mRespCode;
    private ResponseCallback mResponseCallback;

    /**
     * Name is to be set as the thread name. Passing null is safe.
     *
     * @param name
     */
    public MMPHandler(String name, int cmdCode, ResponseCallback responseCallback) {
        super();

        mTimer = null;
        if (null != name) {
            mName = name;
            super.setName(mName);
        }

        mResponseCallback = responseCallback;
    }

    /**
     * View level classes in design shall never call this function as it is reserved for MeemCoreHandler.
     *
     * @param listener The listener object for this MMPHandler (which is-a thread)
     */
    final public void setListener(MMPHandlerListener listener) {
        selfCheck();
        mListener = listener;
    }

    /**
     * This function is to be used by MeemCore handler to post an MMP message to this sequence to advance the sequence by a step. This
     * function is completely thread-safe - as this will be called by an anonymous thread created by MeemCore upon reception of a control
     * message.
     *
     * @param mmpMsg The MMP Message - be a command, response or ack/nack
     *
     * @return boolean.
     */
    final public boolean process(MMPCtrlMsg mmpMsg) {
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
                                mUiCtxt.log(UiContext.DEBUG, "MMPHandler: " + mName + ": Is getting terminated!");

                                if (null != mListener) {
                                    mListener.onMMPHandlerFinish(false);
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
                        if (false == onMMPMessage((MMPCtrlMsg) msg.obj)) {
                            if (null != mListener) {
                                mListener.onMMPHandlerFinish(true);
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
        // TODO: I hope this change will make the dieEarly flag useless. Revisit here after tests.
        if (false == kickStart()) {
            mUiCtxt.log(UiContext.DEBUG, "MMPHandler: " + mName + ": Kickstart returns negative");
            if (null != mListener) {
                mListener.onMMPHandlerFinish(false);
                Looper.myLooper().quit();
            }
        }

        // everything is fine so far. start timer and loop-ho!
        mTimer = new CountDownTimer(ProductSpecs.MMP_CTRL_COMMAND_TIMEOUT_MS, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (null != mListener) {
                    if (false == onMMPTimeout()) {
                        mListener.onMMPHandlerFinish(false);
                        Looper.myLooper().quit();
                        return;
                    }
                }
            }

        }.start();

        Looper.loop();
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
    abstract protected boolean kickStart();

    /**
     * Function to process MMP message. All MMP level error handling is done internally through events. The return value of this function
     * determines whether the handler is complete or not. If a particular implementation has more commands/responses/actions to do, then it
     * must return true. If this function returns false, the handler thread quits.
     *
     * @param msg The MMP control message
     *
     * @return boolean. See documentation of the function.
     */
    abstract protected boolean onMMPMessage(MMPCtrlMsg msg);

    /**
     * Function to handle timeout. All MMP control path objects must implement this. If this function returns false, the handler quits.
     */
    abstract protected boolean onMMPTimeout();

    /**
     * This function will get invoked when the requested XFR is completed successfully. Override if needed. Anyway, the control path will
     * take care of the status of file transfer (if any).
     *
     * @param file The file that completely transferred (can be null)
     *
     * @return void
     */
    protected void onXfrCompletion(File file) {
        mUiCtxt.log(UiContext.DEBUG, "MMPHandler: " + mName + ": XFR completed: " + ((file == null) ? "null!" : file.getAbsolutePath()));
    }

    /**
     * This function will be invoked on transfer error of the specified file. Override if needed. Anyway, the control path will take care of
     * the status of file transfer (if any).
     *
     * @param file   The file whose transfer resulted in error (can be null)
     * @param status The status code describing error
     *
     * @return void
     */
    protected void onXfrError(File file, MeemCoreStatus status) {
        mUiCtxt.log(UiContext.ERROR, "MMPHandler: " + mName + " encountered XFR error: " + ((file == null) ? "null!" : file.getAbsolutePath()) + ": " + ((null != status) ? status.toString() : "Unknown error"));
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
        String info = "MMPHandler: " + mName + " encountered MeemCore exception: " + ex.getMessage();
        evt.setInfo(info);
        mUiCtxt.postEvent(evt);

        terminate(-1);
    }

    /**
     * MMP handlers can override this method to implement their on abort notification handling. At present (17Sept2014) only ExecuteSession
     * overrides this method.
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

    // MMPHandler functions shall never be invoked from self thread context.
    // It may even cause deadlock on onException cases.
    private void selfCheck() {
        if (this == currentThread()) {
            throw new IllegalStateException("MMPHandler public functions shall never be invoked from self. It will deadlock.");
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

    public interface MMPHandlerListener {
        public void onMMPHandlerFinish(boolean status);
    }
}
