package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

/**
 * Created by SCS on 8/12/2016.
 */
public class Legal extends Fragment implements View.OnClickListener {

    final String TYPE = "type";
    View mRootView;
    MainActivity mMainActivity;
    RelativeLayout eula, firmware, warranty;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.legal, null, false);
        init();
        return mRootView;
    }

    private void init() {
        findViewById();
        setClickListener();
    }

    private void setClickListener() {
        eula.setOnClickListener(this);
        firmware.setOnClickListener(this);
        warranty.setOnClickListener(this);
    }

    private void findViewById() {
        eula = (RelativeLayout) mRootView.findViewById(R.id.eula);
        firmware = (RelativeLayout) mRootView.findViewById(R.id.firmware);
        warranty = (RelativeLayout) mRootView.findViewById(R.id.warranty);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mMainActivity = (MainActivity) context;
    }

    @Override
    public void onClick(View v) {
        LegalDetailsFragment legalDetailsFragment = new LegalDetailsFragment();
        Bundle bundle = new Bundle();

        switch (v.getId()) {
            case R.id.eula:
                bundle.putString(TYPE, "EULA");
                legalDetailsFragment.setArguments(bundle);
                break;
            case R.id.firmware:
                bundle.putString(TYPE, "FIRMWARE");
                legalDetailsFragment.setArguments(bundle);
                break;

            case R.id.warranty:
                bundle.putString(TYPE, "WARRANTY");
                legalDetailsFragment.setArguments(bundle);
                break;
        }
        mMainActivity.showFragment(legalDetailsFragment, true, false);
    }
}
