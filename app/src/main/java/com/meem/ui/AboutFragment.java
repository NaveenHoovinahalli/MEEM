package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

/**
 * Created by Naveen on 7/4/2016.
 * This fragment is used to display about MEEM
 */
public class AboutFragment extends Fragment implements View.OnClickListener {

    View mRootView;
    RelativeLayout mMeemWebsite, mLegal;
    MainActivity mMainActivity;

    public AboutFragment() {

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setAppTitle(getResources().getString(R.string.about));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.aboutfrag, container, false);
        init();
        return mRootView;
    }

    private void init() {
        findViewById();
        setClickListener();

    }

    private void setClickListener() {
        mMeemWebsite.setOnClickListener(this);
        mLegal.setOnClickListener(this);
    }

    private void findViewById() {
        mMeemWebsite = (RelativeLayout) mRootView.findViewById(R.id.meemWebsiteRL);
        mLegal = (RelativeLayout) mRootView.findViewById(R.id.legalRl);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.meemWebsiteRL) {

            MeemWebsiteView meemWebsiteView = new MeemWebsiteView();
            mMainActivity.showFragment(meemWebsiteView, true, false);

        } else {

            Legal legal = new Legal();
            mMainActivity.showFragment(legal, true, false);

        }

    }
}
