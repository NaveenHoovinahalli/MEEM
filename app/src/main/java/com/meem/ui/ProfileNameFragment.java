package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;


/**
 * Created by Naveen on 7/18/2016.
 * Class used to change the cable name
 */
public class ProfileNameFragment extends Fragment implements MainActivity.ToolBarItemClickListener {


    View mRootView;
    EditText mNameEt;
    MainActivity mMainActivity;
    String mCableName;
    CableNameInterface mListener;
    ProgressBar mPbar;
    String mVaultID;

    public ProfileNameFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mCableName = getArguments().getString("NAME");
        mVaultID = getArguments().getString("VAULT_ID");

        mRootView = inflater.inflate(R.layout.profilenamefragment, null);
        mNameEt = (EditText) mRootView.findViewById(R.id.nameET);
        mPbar = (ProgressBar) mRootView.findViewById(R.id.pBar);
        mNameEt.setText(mCableName);
        mNameEt.requestFocus();
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        mMainActivity.setBackPressDisable(true);
        setEventListner(mMainActivity.getCablePresenter());

    }

    private void setEventListner(CablePresenter profileNameInterface) {
        mListener = profileNameInterface;
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainActivity.setBackPressDisable(false);

    }

    @Override
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {

    }

    @Override
    public void onRestoreIconListener() {

    }

    @Override
    public void onSelectIconClickListener() {

    }

    @Override
    public void onBackButtonPressed() {
        validateName();
    }

    @Override
    public void onDeleteIconClickListener() {

    }

    private void validateName() {

        if (mNameEt.getText().toString().equals(mCableName)) {
            mMainActivity.setBackPressDisable(false);
            mMainActivity.onBackPressed();
        } else {
            mPbar.setVisibility(View.VISIBLE);
            mListener.onUpdateCableName(mNameEt.getText().toString(), new ResponseCallback() {
                @Override
                public boolean execute(boolean result, Object info, Object extraInfo) {
                    mPbar.setVisibility(View.GONE);
                    if (result) {
                        mMainActivity.setBackPressDisable(false);
                        mMainActivity.onBackPressed();
                    } else {

                    }
                    return false;
                }
            });
        }
    }

    public interface CableNameInterface {
        void onUpdateCableName(String name, ResponseCallback responseCallback);
    }
}
