package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.core.MeemCoreListener;
import com.meem.core.MeemCoreStatus;
import com.meem.mmp.control.MMPHandler.MMPHandlerListener;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class acts as the glue between MeemCore and the application. This class essentially handles all *control messages* that are to be
 * handled once MEEM device is connected. For this, internally it uses MMP handler objects, which are nothing but threads working on a
 * predefined message sequence of sending and receiving control messages. Note that this class is dealing with 3 threads. One is the UI
 * thread who calls the constructor, the other is the current MMP handler thread, and the other is the unnamed thread created by MeemCore
 * object for each of the received control messages. So, this class is heavily synchronized - but the sync overhead is minimal as control
 * messages are never many in any type of MMP session.
 *
 * @author Arun T A
 */
public class MeemCoreHandler implements MeemCoreListener, MMPHandlerListener {
    private UiContext mUiCtxt = UiContext.getInstance();

    private ConcurrentLinkedQueue<MMPHandler> mHandlerQueue;
    private MMPHandler mCurrHandler;

    // get ready for the initial handshaking sequence
    public MeemCoreHandler() {
        mHandlerQueue = new ConcurrentLinkedQueue<MMPHandler>();
        mCurrHandler = null;
    }

    /**
     * Queue the given handler for execution.
     *
     * @param handler
     *
     * @return boolean
     */
    public synchronized boolean addHandler(MMPHandler handler) {
        handler.setListener(this);

        // Arun: 02June2015: Major architecture level bug fix here.
        // See also: onMMPHandlerFinish
        if ((mCurrHandler == null) && mHandlerQueue.isEmpty()) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", As current handler, starting mmp handler: " + handler.getName());
            mCurrHandler = handler;
            mCurrHandler.start();
            return true;
        } else {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", Queuing mmp handler: " + handler.getName());
            return mHandlerQueue.add(handler);
        }
    }

    /**
     * Method to notify the current MMP handler that an abort is requested by the user. See method notifyAbort() of MMPHandler.
     */
    public boolean notifyAbortRequest() {
        if (mCurrHandler != null) {
            return mCurrHandler.notifyAbortRequest();
        }

        return false;
    }

    // =======================================================
    // =================== MeemCore Listeners ================
    // =======================================================

    @Override
    public synchronized void onCtrlMessage(ByteBuffer pkt) {
        MMPCtrlMsg msg = new MMPCtrlMsg(pkt);

        if (null == mCurrHandler) {
            // TODO: Remove this hack by removing this initial handshake stupidity altogether from MMP.
            if (msg.getMessageCode() != MMPConstants.MMP_INTERNAL_HACK_BYPASS_CABLE_AUTH) {
                msg.sendNack();
            }
            mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", No mmp handler for the received message code: " + String.valueOf(msg.getMessageCode()));
        } else {
            mCurrHandler.process(msg);
        }
    }

    @Override
    public synchronized void onXfrCompletion(File file) {
        if (null == mCurrHandler) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", No mmp handler for XFR completion: " + ((file != null) ? file.getPath() : "null"));
        } else {
            mCurrHandler.onXfrCompletion(file);
        }
    }

    @Override
    public synchronized void onXfrError(File file, MeemCoreStatus status) {
        if (null == mCurrHandler) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", No mmp handler for XFR error: " + ((file != null) ? file.getPath() : "null"));
        } else {
            mCurrHandler.onXfrError(file, status);
        }
    }

    @Override
    public synchronized void onException(Throwable ex) {
        if (null == mCurrHandler) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", No mmp handler for exception: " + ex.getMessage());
        } else {
            mCurrHandler.onException(ex);
        }
    }

    @Override
    public synchronized void onMMPHandlerFinish(boolean status) {
        if (null == mCurrHandler) {
            String exMsg = "MeemCoreHandler: " + this + ", onMMPHandlerFinish is called, but current mmp handler is null!";
            mUiCtxt.log(UiContext.WARNING, exMsg);

            throw new IllegalStateException(exMsg);
        } else {
            String curHandlerName = mCurrHandler.getName();

            if (!status) {
                mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", Current mmp handler: " + curHandlerName + ": reports error on finish.");
            } else {
                mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", Current mmp handler: " + curHandlerName + ": reports success on finish.");
            }
        }

        MMPHandler handler = mHandlerQueue.poll();
        if (null != handler) {
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", Starting next mmp handler from queue: " + handler.getName());
            handler.setListener(this);
            mCurrHandler = handler;
            mCurrHandler.start();
        } else {
            // Arun: 02June2015: Major architecture level bug fix here.
            mUiCtxt.log(UiContext.DEBUG, "MeemCoreHandler: " + this + ", No more mmp handlers in queue");
            mCurrHandler = null;
        }
    }

    // Arun: Abortfix 22May2015
    @Override
    public synchronized boolean onXfrRequest() {
        if (null == mCurrHandler) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", No mmp handler for xfr request");
            return false;
        } else {
            return mCurrHandler.onXfrRequest();
        }
    }

    /**
     * Warning: Only for debug.
     */
    public synchronized boolean startupSanityCheck() {
        if (mCurrHandler != null || !mHandlerQueue.isEmpty()) {
            mUiCtxt.log(UiContext.ERROR, "MeemCoreHandler: " + this + ", Sanity check on startup failed: Has pending handlers!");
            return false;
        }

        return true;
    }

    /**
     * Warning: Only for debug.
     */
    public synchronized void cleanup() {
        if (mCurrHandler != null) {
            mUiCtxt.log(UiContext.WARNING, "MeemCoreHandler: " + this + ", cleanup: Terminating current handler: " + mCurrHandler.getName());
            mCurrHandler.terminate(0);
        }

        if (!mHandlerQueue.isEmpty()) {
            MMPHandler handler;
            while (null != (handler = mHandlerQueue.poll())) {
                mUiCtxt.log(UiContext.WARNING, "MeemCoreHandler: " + this + ", cleanup: Clearing queued handler: " + handler.getName());
            }
        }
    }
}
