package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.meem.androidapp.AppPreferences;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;
import com.meem.viewmodel.CableInfo;

import java.text.DecimalFormat;


/**
 * Created by SCS on 7/4/2016.
 */
public class SettingsFragment extends Fragment implements View.OnClickListener {

    View mRootView;
    RelativeLayout mPinChangeRL, mPinResetRL, mNameRL, mFirmwareRL, mDelete,mNWSettingRL,mDisconectNWRL,mMasterDataRL,mAppList;
    MainActivity mMainActivity;
    boolean isBackStack = true;
    Switch mSoundSwitch, mBackupSwitch,mNWSwitch,mDisconectSwitch, mShareDataOnNwSwitch;
    MeemTextView mTotalSize, mAvailableSize, mFirmwareV, mModel, mCableName,mMasterData;
    CableInfo cableInfo;
    ProgressBar mPbar;
    RelativeLayout mParentView;
    private SettingInterface mListener;
    private ProgressDialog mInitProgress;

    DetailsFragment detailsFragment;


    public SettingsFragment() {

    }

    public static String convertKBtoGB(long size) {
        if (size <= 0) return "--";
        double temp = ((double) size / 1024) / 1024;
        if (temp < .01) return "0.01";

        return new DecimalFormat("#0.00").format(temp).replace(',', '.');
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.settingsfrag, null);
        init();
        return mRootView;
    }

    private void init() {

        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewId();
        setOnClick();


    }

    @Override
    public void onStop() {
        super.onStop();
        if (mInitProgress != null)
            mInitProgress.dismiss();
    }


    private void setValues() {

        mTotalSize.setText("" + convertKBtoGB(cableInfo.mCapacityKB) + "GB");

        mAvailableSize.setText("" + convertKBtoGB(cableInfo.mFreeSpaceKB) + "GB");

        mFirmwareV.setText("" + cableInfo.fwVersion);
        mModel.setText(cableInfo.mSerialNo);
        mCableName.setText(cableInfo.mName);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSoundSwitch.setChecked(sharedPreferences.getBoolean(AppPreferences.SOUND_STATUS, true));
        mBackupSwitch.setChecked(sharedPreferences.getBoolean(AppPreferences.BACKUP_NOTIFICATION_STATUS, true));
    }

    @Override
    public void onResume() {
        super.onResume();
        cableInfo = mMainActivity.getCablePresenter().getCableInfo();
        if(mMainActivity.getCableVersion()==2){
            mNWSettingRL.setVisibility(View.VISIBLE);
            mNWSwitch.setChecked(mMainActivity.isNetworkFeatureEnabled());
            mAppList.setVisibility(View.VISIBLE);
        }else {
            mNWSettingRL.setVisibility(View.GONE);
        }
        mMainActivity.setAppTitle(getResources().getString(R.string.settings));
        if (cableInfo != null) {
            setEventListner(mMainActivity.getCablePresenter());
            setValues();
        }

        if(mMainActivity.isMeemConnectedOverNetwork()){
            mPinResetRL.setVisibility(View.GONE);
            mPinChangeRL.setVisibility(View.GONE);
        }else {
            mPinResetRL.setVisibility(View.VISIBLE);
            mPinChangeRL.setVisibility(View.VISIBLE);
        }

        if(mMainActivity.isMasterOfNetwork()) {
            mMasterDataRL.setVisibility(View.VISIBLE);
            mShareDataOnNwSwitch.setChecked(mMainActivity.getCablePresenter().getPolicyIsNetworkSharingEnabled());
        }
        else {
            mMasterDataRL.setVisibility(View.GONE);
        }

    }

    public void setEventListner(SettingInterface settingInterface) {
        mListener = settingInterface;
    }

    private void setOnClick() {
        mPinChangeRL.setOnClickListener(this);
        mPinResetRL.setOnClickListener(this);
        mNameRL.setOnClickListener(this);
        mFirmwareRL.setOnClickListener(this);
//        mSoundSwitch.setOnCheckedChangeListener(this);
        mSoundSwitch.setOnClickListener(this);
        mBackupSwitch.setOnClickListener(this);
        mDelete.setOnClickListener(this);
        mDisconectSwitch.setOnClickListener(this);
        mNWSwitch.setOnClickListener(this);
        mShareDataOnNwSwitch.setOnClickListener(this);
        mAppList.setOnClickListener(this);
    }

    private void findViewId() {

        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mPinChangeRL = (RelativeLayout) mRootView.findViewById(R.id.rLPinChange);
        mPinResetRL = (RelativeLayout) mRootView.findViewById(R.id.rLPinReset);
        mNameRL = (RelativeLayout) mRootView.findViewById(R.id.rLName);
        mFirmwareRL = (RelativeLayout) mRootView.findViewById(R.id.rLFirmware);
        mSoundSwitch = (Switch) mRootView.findViewById(R.id.soundSwitch);
        mBackupSwitch = (Switch) mRootView.findViewById(R.id.backupSwitch);
        mTotalSize = (MeemTextView) mRootView.findViewById(R.id.totalSize);
        mAvailableSize = (MeemTextView) mRootView.findViewById(R.id.availableSize);
        mFirmwareV = (MeemTextView) mRootView.findViewById(R.id.firmwareV);
        mModel = (MeemTextView) mRootView.findViewById(R.id.phoneModel);
        mCableName = (MeemTextView) mRootView.findViewById(R.id.cableName);
        mPbar = (ProgressBar) mRootView.findViewById(R.id.pBar);
        mDelete = (RelativeLayout) mRootView.findViewById(R.id.rLdelete);
        mDisconectSwitch= (Switch) mRootView.findViewById(R.id.nwDisconnectSwitch);
        mNWSwitch= (Switch) mRootView.findViewById(R.id.nwSwitch);

        mNWSettingRL= (RelativeLayout) mRootView.findViewById(R.id.nwRL);
        mDisconectNWRL= (RelativeLayout) mRootView.findViewById(R.id.nwDisconRL);

        mMasterDataRL= (RelativeLayout) mRootView.findViewById(R.id.nwMasterRL);
        mShareDataOnNwSwitch = (Switch) mRootView.findViewById(R.id.nwMasterDataSwitch);

        mAppList= (RelativeLayout) mRootView.findViewById(R.id.rLAppList);

    }

    @Override
    public void onClick(View v) {
        Fragment fragment = null;


        switch (v.getId()) {
            case R.id.rLPinChange:
                fragment = new PinChangeFragment();
                callFragmentReplace(fragment);
                break;
            case R.id.rLPinReset:
                showAlertDialoge();
               /* fragment = new PinResetFragment();
                callFragmentReplace(fragment);*/
                break;
            case R.id.rLName:
                fragment = new ProfileNameFragment();
                Bundle bundle = new Bundle();
                bundle.putString("NAME", cableInfo.mName);
                fragment.setArguments(bundle);
                callFragmentReplace(fragment);
                break;
            case R.id.rLFirmware:
                fragment = new FirmwareFragment();
                callFragmentReplace(fragment);
                break;
            case R.id.rLdelete:
                detailsFragment = new DetailsFragment();
                Bundle bundle1 = new Bundle();
                bundle1.putBoolean("FROMSETTING", true);
                detailsFragment.setArguments(bundle1);
                mMainActivity.showFragment(detailsFragment, true, false);
                break;
            case R.id.soundSwitch:
                updateSoundSettings();
                break;
            case R.id.backupSwitch:
                updateBackupNotificationSettings();
                break;
            case R.id.nwSwitch:
                updateNWSettings();
                break;
            case R.id.nwDisconnectSwitch:
                disconnectfromNW();
                break;
            case R.id.nwMasterDataSwitch:
                setMasterPrivacy();
                break;
            case R.id.rLAppList:
                fragment=new AppList();
                callFragmentReplace(fragment);
                break;
        }
    }

    public DetailsFragment getDetailFragmentObj(){
        return detailsFragment;
    }

    public void setMasterPrivacy(){


        showInitializingProgressBar(0, mInitProgress, "");

        // remember: ui is to enable/disable sharing. CablePresenter method is for privacy. so isChecked is to be reversed.
        mMainActivity.getCablePresenter().setPolicyIsNetworkSharingEnabled(mShareDataOnNwSwitch.isChecked(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mInitProgress.dismiss();
                return false;
            }
        });
    }

    private void showAlertDialoge() {
        AlertDialog.Builder alertDialoge = new AlertDialog.Builder(getActivity());
        alertDialoge.setMessage(getString(R.string.reset_warning));
        alertDialoge.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetCable();
            }
        });
        alertDialoge.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mInitProgress.dismiss();
                mMainActivity.onBackPressed();
            }
        });

        AlertDialog alertDialog = alertDialoge.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    private void resetCable() {

        showInitializingProgressBar(0, mInitProgress, "");

        mListener.onResetCable(new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                if (!result) {
                    mInitProgress.dismiss();
                    mMainActivity.showToast(getString(R.string.failed));
                }


                return false;
            }
        });
    }

    private void showInitializingProgressBar(int i, ProgressDialog mInitProgress, String s) {
        mInitProgress.setMessage("Test");
        mInitProgress.setTitle("Hello");
        mInitProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mInitProgress.setIndeterminate(true);
        mInitProgress.setCancelable(false);
        mInitProgress.setProgress(i);
        mInitProgress.show();
    }


    private void updateBackupNotificationSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.edit().putBoolean(AppPreferences.BACKUP_NOTIFICATION_STATUS, mBackupSwitch.isChecked()).commit();

        if (mBackupSwitch.isChecked()) {
            mMainActivity.SetBadgeAlarm();
        } else {
            mMainActivity.removeBadge();
        }
    }

    private void updateNWSettings() {
        if(mNWSwitch.isChecked()) {
            mMasterDataRL.setVisibility(View.VISIBLE);

            mMainActivity.startMeemNetServer();
        }
        else {
            mMasterDataRL.setVisibility(View.GONE);

            mMainActivity.stopMeemNetServer();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.edit().putBoolean(AppPreferences.NW_SETTING_STATUS, mNWSwitch.isChecked()).commit();
    }

    private void disconnectfromNW(){
        Toast.makeText(getContext(),"Disconnect From NW ,Need an API ", Toast.LENGTH_SHORT).show();
    }

    private void updateSoundSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.edit().putBoolean(AppPreferences.SOUND_STATUS, mSoundSwitch.isChecked()).commit();

/*        mPbar.setVisibility(View.VISIBLE);
        DetailsFragment.enableDisableView(mParentView,false);
        mListener.onSoundUpdate(mSoundSwitch.isChecked(), new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mPbar.setVisibility(View.GONE);
                DetailsFragment.enableDisableView(mParentView,true);

                return false;
            }
        });*/
    }

    private void callFragmentReplace(Fragment fragment) {
        mMainActivity.showFragment(fragment, isBackStack, false);
    }

    public interface SettingInterface {
        void onSoundUpdate(boolean isOn, ResponseCallback responseCallback);
        void onResetCable(ResponseCallback responseCallback);
    }
}
