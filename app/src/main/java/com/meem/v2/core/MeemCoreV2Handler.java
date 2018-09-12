package com.meem.v2.core;

import com.meem.androidapp.UiContext;
import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.v2.mmp.MMPV2;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.meem.androidapp.ContactTrackerWrapper.tag;

/**
 * This class simply creates a thread to invoke MeemCoreV2 methods. The fact that MEEM V2 supports only one transaction at a time helps
 * immensily to simplify the whole design. (Just compare this with original MeemCoreV2Request in V1!)
 * <p>
 * Created by arun on 20/4/17.
 */

public class MeemCoreV2Handler implements MeemCoreV2Listener {
    private static final String TAG = "MeemCoreV2Handler";

    private UiContext mUiCtxt = UiContext.getInstance();

    private ConcurrentLinkedQueue<MeemCoreV2Request> mRequestQueue;
    private MeemCoreV2Request mCurrRequest;

    // get ready for the initial handshaking sequence
    public MeemCoreV2Handler() {
        mRequestQueue = new ConcurrentLinkedQueue<MeemCoreV2Request>();
        mCurrRequest = null;
    }

    /**
     * Queue the given handler for execution.
     *
     * @param request
     *
     * @return boolean
     */
    public synchronized boolean addRequest(MeemCoreV2Request request) {
        request.setFinalizer(new Runnable() {
            @Override
            public void run() {
                MeemCoreV2.getInstance().releaseCable(mUiCtxt.getPhoneUpid());
                onMeemCoreV2RequestFinish();
            }
        });

        // Arun: 02June2015: Major architecture level bug fix here.
        // See also: onMeemCoreV2RequestFinish
        if ((mCurrRequest == null) && mRequestQueue.isEmpty()) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", As current handler, starting request handler: " + request.toString());

            mCurrRequest = request;
            mCurrRequest.start();
            return true;
        } else {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", Queuing request handler: " + request.toString());
            return mRequestQueue.add(request);
        }
    }

    /**
     * Method to notify the current request handler that an abort is requested by the user. See method notifyAbort() of MeemCoreV2Request.
     */
    public synchronized boolean notifyAbortRequest() {
        if (mCurrRequest != null) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", Notifying abort to request handler: " + mCurrRequest.toString());
            return mCurrRequest.notifyAbortRequest();
        } else {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", No request handler to abort. Directly calling core (overkill)");
            return MeemCoreV2.getInstance().abortAllXfr(false);
        }

        // Arun 14June2017: IF ABSOLUTELY NEEDED, WE CAN CALL A CLEANUPO HERE!!!
    }

    // =======================================================
    // =================== MeemCore Listeners ================
    // =======================================================

    @Override
    public synchronized void onCtrlMessage(ByteBuffer pkt) {
        MMPV2 msg = new MMPV2(pkt);

        if (null == mCurrRequest) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", No request handler for the received message");
        } else {
            mCurrRequest.process(msg);
        }
    }

    @Override
    public synchronized void onXfrCompletion(String path) {
        if (null == mCurrRequest) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", No request to deal with XFR completion: " + ((path != null) ? path : "null"));
        } else {
            mCurrRequest.onXfrCompletion(path);
            mCurrRequest.terminate(0);
        }
    }

    @Override
    public synchronized void onXfrError(String path, int status) {
        if (null == mCurrRequest) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", No request to deal with XFR error: " + ((path != null) ? path : "null"));
        } else {
            mCurrRequest.onXfrError(path, status);
            mCurrRequest.terminate(status);
        }
    }

    @Override
    public synchronized void onException(Throwable ex) {
        if (null == mCurrRequest) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", No request to deal with exception: " + ex.getMessage());
        } else {
            mCurrRequest.onException(ex);
            mCurrRequest.terminate(-1);
        }
    }

    public synchronized void onMeemCoreV2RequestFinish() {
        if (null == mCurrRequest) {
            String exMsg = "MeemCoreV2Handler: " + this + ", onMeemCoreV2RequestFinish is called, but current request is null!";
            mUiCtxt.log(UiContext.WARNING, exMsg);

            throw new IllegalStateException(exMsg);
        } else {
            String requestStr = mCurrRequest.toString();
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", Current request: " + requestStr + ": onFinish");
        }

        MeemCoreV2Request request = mRequestQueue.poll();
        if (null != request) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", Starting next request from queue: " + request.getName());

            request.setFinalizer(new Runnable() {
                @Override
                public void run() {
                    MeemCoreV2.getInstance().releaseCable(mUiCtxt.getPhoneUpid());
                    onMeemCoreV2RequestFinish();
                }
            });

            mCurrRequest = request;
            mCurrRequest.start();
        } else {
            // Arun: 02June2015: Major architecture level bug fix here.
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreV2Handler: " + this + ", No more requests in queue");
            mCurrRequest = null;
        }
    }

    // Arun: Abortfix 22May2015
    @Override
    public synchronized boolean onXfrRequest() {
        if (null == mCurrRequest) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", No pending request for xfr!");
            return false;
        } else {
            return mCurrRequest.onXfrRequest();
        }
    }

    /**
     * Warning: Only for debug.
     */
    public synchronized boolean startupSanityCheck() {
        if (mCurrRequest != null || !mRequestQueue.isEmpty()) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreV2Handler: " + this + ", Sanity check on startup failed: Has pending requests!");
            return false;
        }

        return true;
    }

    /**
     * Warning: Recommended only for debugging. Any warning message from this method is an indication of bugs elsewhere.
     */
    public synchronized void cleanup() {
        if (mCurrRequest != null) {
            mUiCtxt.log(UiContext.WARNING, "MeemCoreV2Handler: " + this + ", cleanup: Terminating current request: " + mCurrRequest.toString());
            mCurrRequest.terminate(-1);
        }

        if (!mRequestQueue.isEmpty()) {
            MeemCoreV2Request request;
            while (null != (request = mRequestQueue.poll())) {
                mUiCtxt.log(UiContext.WARNING, "MeemCoreV2Handler: " + this + ", cleanup: Clearing queued request: " + request.toString());
            }
        }
    }

    public synchronized void acquireBigCableLock(final String tag, final ResponseCallback responseCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = MeemCoreV2.getInstance().acquireCable(tag, null);
                MeemEvent evt = new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, null, null, responseCallback);
                evt.setResult(result);
                mUiCtxt.postEvent(evt);
            }
        }).start();
    }

    public synchronized void releaseBigCableLock(final String tag, final ResponseCallback responseCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = MeemCoreV2.getInstance().releaseCable(tag);
                MeemEvent evt = new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, null, null, responseCallback);
                evt.setResult(result);
                mUiCtxt.postEvent(evt);
            }
        }).start();
    }

    public void sendMessageToNetMaster(final int msgCode, final ResponseCallback responseCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = MeemCoreV2.getInstance().sendMessageToNetMaster(msgCode);
                MeemEvent evt = new MeemEvent(EventCode.UI_THREAD_EXECUTE_REQ, null, null, responseCallback);
                evt.setResult(result);
                mUiCtxt.postEvent(evt);
            }
        }).start();
    }
}
