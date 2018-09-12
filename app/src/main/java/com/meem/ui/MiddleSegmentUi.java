package com.meem.ui;

import android.app.Activity;
import android.widget.FrameLayout;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.utils.DebugTracer;

import java.util.LinkedHashMap;

/**
 * @author arun
 */
public class MiddleSegmentUi implements AutoBackupCountDownUi.AutoBackupCountDownUiListener, DisconnectedStateUi.DisconnectedStateUiListener, VirginCablePinSetupUi.VirginCablePinSetupUiListener, UnregisteredPhoneAuthUi.UnregisteredPhoneAuthUiListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MiddleSegmentUi";
    UiContext mUiCtxt = UiContext.getInstance();
    DebugTracer mDbg = new DebugTracer(TAG, "MiddleSegmentUi.log");

    FrameLayout mViewRoot;

    AutoBackupCountDownUi mAutoBackupCountDownUi;
    DisconnectedStateUi mDisconnectedStateUi;
    VirginCablePinSetupUi mVirginCablePinSetupUi;
    UnregisteredPhoneAuthUi mUnregisteredPhoneAuthUi;

    MiddleSegmentListener mListener;

    public MiddleSegmentUi(FrameLayout viewRoot) {
        mViewRoot = viewRoot;
    }

    public void setEventListener(MiddleSegmentListener listener) {
        mListener = listener;
    }

    public boolean create() {
        mDbg.trace();

        setWidthAndHeight();
        return true;
    }

    private void setWidthAndHeight() {
        mDbg.trace();

        FrameLayout.LayoutParams llp = (FrameLayout.LayoutParams) mViewRoot.getLayoutParams();
        llp.width = (int) mUiCtxt.getScreenWidthPix();
        mViewRoot.setLayoutParams(llp);
    }

    // -------------------------------------------------------------------
    // ------------------ Start: Disconnected state Ui stuff -------------
    // -------------------------------------------------------------------

    public void showDisconnectedStateUi() {
        mDbg.trace();

        // TODO: well, here things turns a bit ugly. user disconnected the cable when disconnectedstateui was waiting to be removed
        // esp. during thumbnail refresh time. To avoid this, remove thumbnail processing progress ui from disconnectedstateui
        if (mDisconnectedStateUi != null && mDisconnectedStateUi.isShowingCableConnected()) {
            mDisconnectedStateUi.showCableDisconnected();
        } else {
            if (mDisconnectedStateUi != null) {
                final MiddleSegmentUi thisInstance = this;
                mDisconnectedStateUi.destroy(new Runnable() {
                    @Override
                    public void run() {
                        mDisconnectedStateUi = new DisconnectedStateUi(mViewRoot, thisInstance);
                        mDisconnectedStateUi.create();
                    }
                });
            } else {
                mDisconnectedStateUi = new DisconnectedStateUi(mViewRoot, this);
                mDisconnectedStateUi.create();
            }
        }
    }

    public void showCableConnected() {
        mDisconnectedStateUi.showCableConnected();
    }

    /*public void showInitializingProgressBar(int percent) {
        mDisconnectedStateUi.showInitializingProgressBar(percent);
    }

    public void hideInitializingProgressBar() {
        mDisconnectedStateUi.hideInitializingProgressBar();
    }*/

    public void removeDisconnectedStateUi() {
        mDisconnectedStateUi.destroy(null);
    }

    @Override
    public void onDisconnectedStateUiFinish() {
        mListener.onDisconnectedStateUiFinish();
    }

    // -------------------------------------------------------------------
    // ------------------ End: Disconnected state Ui stuff -------------
    // -------------------------------------------------------------------


    // -------------------------------------------------------------------
    // ------------------ Start: Virgin cable pin setup stuff ------------
    // -------------------------------------------------------------------

    public void showVirginCablePinSetupUi() {
        mVirginCablePinSetupUi = new VirginCablePinSetupUi(mViewRoot, this);
        mVirginCablePinSetupUi.create();
    }

	// Naveen: added new parameter HashMap for recovery.
    // Arun: 05July2018: integrated pin recovery with backend.
    @Override
    public void onVirginCablePinEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mListener.onVirginCablePinSetupEntry(pin, recoveryAnswers, responseCallback);
    }

    public void removeVirginCablePinSetupUi() {
        mVirginCablePinSetupUi.destroy(new Runnable() {
            @Override
            public void run() {
                mListener.onVirginCablePinSetupUiFinish();
            }
        });
    }

    // -------------------------------------------------------------------
    // ------------------ End: Virgin cable pin setup stuff ------------
    // -------------------------------------------------------------------


    // -------------------------------------------------------------------
    // ------------------ Start: New phone authentication stuff ----------
    // -------------------------------------------------------------------

    public void showUnregisteredPhoneAuthUi() {
        mUnregisteredPhoneAuthUi = new UnregisteredPhoneAuthUi(mViewRoot, this);
        mUnregisteredPhoneAuthUi.create();
    }

    @Override
    public void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback) {
        mListener.onUnregisteredPhoneAuthEntry(pin, responseCallback);
    }

	// Arun: 05July2018: Validate the pin recovery answers with cable.
    @Override
    public void onValidateRecoveryQuestions(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback) {
        mListener.onValidateRecoveryAnswers(recoveryAnswers, responseCallback);
    }

    public void removeUnregisteredPhoneAuthUi() {
        mUnregisteredPhoneAuthUi.destroy(new Runnable() {
            @Override
            public void run() {
                mListener.onUnregisteredPhoneAuthUiFinish();
            }
        });
    }


    // -------------------------------------------------------------------
    // ------------------ End: New phone authentication stuff ------------
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // ------------------ Start: Autobackup countdown stuff --------------
    // -------------------------------------------------------------------

    public void showAutoBackupCountDownUi() {
        mAutoBackupCountDownUi = new AutoBackupCountDownUi(mViewRoot, this);
        mAutoBackupCountDownUi.create();
    }

    @Override
    public void onAutoBackupCountDownEnd(boolean startAutoBackup) {
        mListener.onAutoBackupCountDownEnd(startAutoBackup);
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mListener.onAutoBackupCountDownUiFinish();
    }

    public void removeAutoBackupCountDownUi() {
        mAutoBackupCountDownUi.destroy();
    }


    // -------------------------------------------------------------------
    // ------------------ End: Autobackup countdown stuff --------------
    // -------------------------------------------------------------------

    @Override
    public Activity getActivity() {
        return mListener.getActivity();
    }

    public void animateNetworkSearch(boolean start) {
        if(null != mDisconnectedStateUi) {
            if(start) {
                mDisconnectedStateUi.setNetworkSearchTextBusy();
                mDisconnectedStateUi.startWifiIconAnimation();
            } else {
                mDisconnectedStateUi.setNetworkSearchTextNormal();
                mDisconnectedStateUi.stopWifiIconAnimation();
            }

            mDisconnectedStateUi.enableWifiIconClick(!start);
        }
    }

    /**
     * ViewController must implement this interface.
     */
    public interface MiddleSegmentListener {
        void onAutoBackupCountDownEnd(boolean startAutoBackup);
        void onAutoBackupCountDownUiFinish();

        void onDisconnectedStateUiFinish();

        void onVirginCablePinSetupEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
        void onVirginCablePinSetupUiFinish();

        void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback);
        void onUnregisteredPhoneAuthUiFinish();

        void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);

        Activity getActivity();
    }
}
