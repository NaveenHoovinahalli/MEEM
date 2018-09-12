package com.meem.ui;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.viewmodel.CableInfo;
import com.meem.viewmodel.CategoryInfo;
import com.meem.viewmodel.PhoneInfo;
import com.meem.viewmodel.VaultInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Use the {@link HomeFragment#newInstance} factory method to create an instance of this fragment. <p> Note: At any point in time,if someone
 * finds that phoneInfo and cableInfo objects in this fragment to be null, then consider passing those objects in a bundle (just like in the
 * original template code for fragments) </p>
 */
public class HomeFragment extends android.support.v4.app.Fragment implements HomeViewController.HomeViewControllerListener {
    private static String TAG = "HomeFragment";

    FrameLayout mPhoneFrameLayout, mMiddleFrameLayout, mMeemFrameLayout;
    UiContext mUiCtxt = UiContext.getInstance();
    MainActivity mMainActivity;
    View mMainView;
    private HomeViewController mViewController;
    private HomeFragmentInterface mListener;

    private ProgressDialog mInitProgress;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    /**
     * CablePresenter is the listener for home fragment
     *
     * @param listener
     */
    public void setEventListener(HomeFragmentInterface listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mMainView == null) {
            mMainView = inflater.inflate(R.layout.fragment_home_screen, container, false);

            mPhoneFrameLayout = (FrameLayout) mMainView.findViewById(R.id.home_phonesegment_framelayout);
            mMiddleFrameLayout = (FrameLayout) mMainView.findViewById(R.id.home_middlesegment_framelayout);
            mMeemFrameLayout = (FrameLayout) mMainView.findViewById(R.id.home_meemsegment_framelayout);

            mViewController = new HomeViewController((FrameLayout) mMainView, mPhoneFrameLayout, mMiddleFrameLayout, mMeemFrameLayout);
            mViewController.setEventListener(this);
        }

        return mMainView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // should not set title when session is going on (home view is locked during session)
        if (null != mViewController && !mViewController.getUiLockState()) {
            mMainActivity.setAppTitle(getResources().getString(R.string.home));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(new Bundle());
    }

    /*
     * Public interfaces follows
     */
    public void showCableConnected() {
        mViewController.showCableConnected();
    }

    public void removeDisconnectedStateUi() {
        mViewController.removeDisconnectedStateUi();
    }

    /*public void showInitializingProgressBar(int percent) {
        mViewController.showInitializingProgressBar(percent);
    }

    public void hideInitializingProgressBar() {
        mViewController.hideInitializingProgressBar();
    }*/

    public void showAutoBackupCountDown() {
        mViewController.showAutobackupCountDown();
    }

    public void removeAutoBackupCountDown() {
        mViewController.removeAutoBackupcountDown();
    }

    public void showSegmentsOnCableConnection() {
        mViewController.showCableConnectionSequence();
    }

    public void onVirginCableConnection() {
        mViewController.showVirginCablePinSetupUi();
    }

    public void onUnregisteredPhoneConnection() {
        mViewController.showUnregisteredPhoneAuthUi();
    }

    public void onCableDisconnection() {
        if (null != mInitProgress) {
            mInitProgress.dismiss();
            mInitProgress = null;
        }

        mViewController.showCableDisconnectionSequence();
    }

    public void update(PhoneInfo phoneInfo, CableInfo cableInfo) {
        mViewController.update(phoneInfo, cableInfo);
    }

    public void onSessionStart(VaultInfo vault, ArrayList<CategoryInfo> catInfos) {
        mViewController.setUiLockState(true);
        mViewController.startSessionAnimations(vault, catInfos);
    }

    public void onSessionProgressUpdate(MMPSessionStatusInfo sessionStatusInfo) {
        byte cat = sessionStatusInfo.getCatCode();
        MMPSessionStatusInfo.Type statustype = sessionStatusInfo.getType();
        if (statustype == MMPSessionStatusInfo.Type.ENDED) {
            mViewController.stopSessionAnimForCat(cat);
        }
    }

    public void onSessionEnd(boolean result, String msg) {
        mViewController.stopSessionAnimations();
        mViewController.setUiLockState(false);
    }

    public void onUserAbortRequest() {
        mListener.onAbortRequest();
    }

    // --------------------------------------------------------------------
    // --------------Start: view controller listener interface impl.
    // --------------------------------------------------------------------

    @Override
    public Activity getMainActivity() {
        return mMainActivity;
    }

    @Override
    public void onDropOnPhone(String srcUpid) {
        mListener.onDropOnPhone(srcUpid);
    }

    @Override
    public void onDropOnMirror() {
        mListener.onDropOnMirror();
    }

    @Override
    public void onCategorySetSwipeToMirror(ArrayList<CategoryInfo> catInfoList) {
        mListener.onCategorySetSwipeToMirror(catInfoList);
    }

    @Override
    public void onCategorySetSwipeToPhone(String srcUpid, ArrayList<CategoryInfo> catInfoList) {
        mListener.onCategorySetSwipeToPhone(srcUpid, catInfoList);
    }

    /**
     * Updated Arun: 06June2017: In V2, this method will result in updating configdb, sending it to cable, fetching it back, rebuilding
     * model etcis taking time. IF we are not protecting the gui from user, he can initiate many things which will end up in a crash because
     * of incomplete model.
     */
    @Override
    public void onUpdateVault(VaultInfo vault, final ResponseCallback responseCallback) {

        mInitProgress = CustomDialogProgress.ctor(getContext());
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");

        mListener.onUpdateVault(vault, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                // can be null if cable disconnected during display.
                if (mInitProgress != null) {
                    mInitProgress.dismiss();
                }

                if (null != responseCallback) {
                    return responseCallback.execute(result, info, extraInfo);
                } else {
                    return result;
                }
            }
        });
    }

    @Override
    public void onAbortRequest() {
        mListener.onAbortRequest();
    }

    @Override
    public void onVirginCablePinSetupEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mListener.onVirginCablePinSetupEntry(pin, recoveryAnswers, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                responseCallback.execute(result, info, extraInfo);
                if (result) {
                    mViewController.removeVirginCablePinSetupUi();
                }

                return result;
            }
        });
    }

    @Override
    public void onVirginCablePinSetupUiFinished() {
        mListener.onVirginCablePinSetupUiFinished();
    }

    @Override
    public void onUnregisteredPhoneAuthEntry(String pin, final ResponseCallback responseCallback) {
        mListener.onUnregisteredPhoneAuthEntry(pin, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                responseCallback.execute(result, info, extraInfo);

                if (result) {
                    mViewController.removeUnregisteredPhoneAuthUi();
                }

                return false;
            }
        });
    }

    @Override
    public void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, final ResponseCallback responseCallback) {
        mListener.onValidateRecoveryAnswers(recoveryAnswers, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                responseCallback.execute(result, info, extraInfo);

                if (result) {
                    mViewController.removeUnregisteredPhoneAuthUi();
                }

                return false;
            }
        });
    }

    @Override
    public void onUnregisteredPhoneAuthUiFinished() {
        mListener.onUnregisteredPhoneAuthUiFinished();
    }

    @Override
    public void onAutoBackupCountDownEnd() {
        mListener.onAutoBackupCountDownEnd();
    }

    @Override
    public void onAutoBackupCountDownUiFinish() {
        mListener.onAutoBackupCountDownUiFinish();
    }

    @Override
    public void onDisconnectedStateUiFinished() {
        if (mListener != null)
            mListener.onDisconnectedStateUiFinished();
    }

    // --------------------------------------------------------------------
    // --------------End: view controller listener interface impl.
    // --------------------------------------------------------------------

    // --------------------------------------------------------------------
    // -------------- The cable initializing progress bar
    // --------------------------------------------------------------------
    public void showCableInitProgressBar() {
        if (null != mInitProgress) {
            return;
        }

        mInitProgress = new ProgressDialog(getActivity());

        mInitProgress.setMessage(mUiCtxt.getAppContext().getResources().getString(R.string.please_wait_initilizing_cable));
        mInitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mInitProgress.setIndeterminate(true);
        mInitProgress.setCancelable(false);

        mInitProgress.show();
    }

    public void hideCableInitProgressBar() {
        if (null != mInitProgress) {
            mInitProgress.dismiss();
            mInitProgress = null;
        }
    }

    public void animateNetworkSearch(boolean start) {
        mViewController.animateNetworkSearch(start);
    }

    public void onCableAcquireRequest(String upid) {
        mViewController.onCableAcquireRequest(upid);
    }

    public void onCableReleaseRequest(String upid) {
        mViewController.onCableReleaseRequest(upid);
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     *
     * @param upid
     */
    public void onRemoteClientStart(String upid) {
        mViewController.onRemoteClientStart(upid);
    }

    /**
     * upid null means this phone is the net client. So, all meem ui will be updated.
     *
     * @param upid
     */
    public void onRemoteClientQuit(String upid) {
        mViewController.onRemoteClientQuit(upid);
    }

    /**
     * To be implemented by CablePresenter. The rule is, if it is related to cable, implement it in CablePresenter.
     */
    public interface HomeFragmentInterface {
        void onDropOnPhone(String srcUpid);
        void onDropOnMirror();
        void onCategorySetSwipeToMirror(ArrayList<CategoryInfo> catInfoList);
        void onCategorySetSwipeToPhone(String srcUpid, ArrayList<CategoryInfo> catInfoList);
        void onUpdateVault(VaultInfo vault, ResponseCallback responseCallback);
        void onAbortRequest();

        void onDisconnectedStateUiFinished();

        void onAutoBackupCountDownEnd();
        void onAutoBackupCountDownUiFinish();

        void onVirginCablePinSetupEntry(String pin, LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
        void onVirginCablePinSetupUiFinished();

        void onUnregisteredPhoneAuthEntry(String pin, ResponseCallback responseCallback);
        void onUnregisteredPhoneAuthUiFinished();

        void onValidateRecoveryAnswers(LinkedHashMap<Integer, String> recoveryAnswers, ResponseCallback responseCallback);
    }
}
