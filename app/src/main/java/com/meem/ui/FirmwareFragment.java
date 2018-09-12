package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.fwupdate.UpdateInfo;
import com.meem.ui.utils.MeemTextView;

import java.util.ArrayList;


/**
 * Created by SCS on 7/18/2016.
 */
public class FirmwareFragment extends Fragment implements AdapterView.OnItemClickListener {

    View mRootView;
    ListView mListView;
    MainActivity mMainActivity;
    ArrayList<UpdateInfo> updateInfoArrayList = new ArrayList<>();
    ProgressBar mPbar;
    MeemTextView mNoFW;

    public FirmwareFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
        updateInfoArrayList = mMainActivity.getFwUpdateMgr().getUpdateList();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.firmwarefragmentlist, null);
        init();
        return mRootView;
    }

    private void init() {
        fetchViewId();

        if (updateInfoArrayList.size() > 0) {
            setList();
            setClickListener();
        } else {
            mListView.setVisibility(View.GONE);
            mNoFW.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setAppTitle(getString(R.string.firmware));

    }


    private void setClickListener() {
        mListView.setOnItemClickListener(this);
    }

    private void fetchViewId() {
        mListView = (ListView) mRootView.findViewById(R.id.firmwareList);
        mPbar = (ProgressBar) mRootView.findViewById(R.id.pBar);
        mNoFW = (MeemTextView) mRootView.findViewById(R.id.noFWTv);
    }


    private void setList() {

        mListView.setAdapter(new FirmwareListAdapter(mMainActivity, updateInfoArrayList));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//        TODO : Change the KEY value
        FirmwareItemDetails firmwareItemDetails = new FirmwareItemDetails();
        Bundle bundle = new Bundle();
        bundle.putString("FIRMWARE_KEY", "KEY");
        firmwareItemDetails.setArguments(bundle);
        mMainActivity.showFragment(firmwareItemDetails, true, false);

    }


}
