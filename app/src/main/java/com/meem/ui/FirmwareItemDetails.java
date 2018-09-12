package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.ui.utils.MeemTextView;

/**
 * Created by SCS on 11/17/2016.
 */

public class FirmwareItemDetails extends Fragment implements View.OnClickListener {

    View mRootView;
    MeemTextView mUpdate;
    MainActivity mMainActivity;
    RelativeLayout mProgressBar;
    RelativeLayout mParentView;
    String mKey;
    boolean isUpdateStarted;
    TextView mProgressMsg;
    private FirmwareInterface mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mKey = getArguments().getString("FIRMWARE_KEY");
        mRootView = inflater.inflate(R.layout.firmware_item_details, container, false);
        init();
        return mRootView;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    private void init() {
        mUpdate = (MeemTextView) mRootView.findViewById(R.id.updateFirmware);
        mProgressBar = (RelativeLayout) mRootView.findViewById(R.id.lProbressBar);
        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mProgressMsg = (TextView) mRootView.findViewById(R.id.progressMsg);


        mUpdate.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setEventListener(mMainActivity.getCablePresenter());
        mMainActivity.setAppTitle(getString(R.string.firmware_update));

    }

    public void setEventListener(FirmwareInterface listener) {

        mListener = listener;
    }

    @Override
    public void onClick(View v) {
        isUpdateStarted = true;
        setProgressMessageandButton(true, "");
        mListener.onFirmwareUpdate(mKey, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                isUpdateStarted = false;
                setProgressMessageandButton(false, "");
                return false;
            }
        });
    }

    public void setProgressMessageandButton(boolean showProgress, String message) {

        if (!showProgress) {
            DetailsFragment.enableDisableView(mParentView, true);
            mProgressBar.setVisibility(View.GONE);
            mMainActivity.setSelectandBackIconeClickable(true);
            mMainActivity.setBackPressDisable(false);

            return;
        }
        DetailsFragment.enableDisableView(mParentView, false);
        mMainActivity.setSelectandBackIconeClickable(false);
        mMainActivity.setBackPressDisable(true);

        if (message.equals("")) message = getString(R.string.progress_msg);

        mProgressBar.setVisibility(View.VISIBLE);
        mProgressMsg.setText(message);

    }


    public interface FirmwareInterface {
        void onFirmwareUpdate(String id, ResponseCallback responseCallback);
    }


}
