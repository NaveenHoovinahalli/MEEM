package com.meem.ui;

import android.annotation.SuppressLint;
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


/**
 * Created by SCS on 7/4/2016.
 */
@SuppressLint("ValidFragment")
public class HelpNWFragment extends Fragment implements View.OnClickListener {


    public static String helpItem = "HELPITEM";
    public String[] mHelpNames;

    ListView mListView;
    boolean isBackStack = true;
    View rootView;
    ProgressBar mPbar;
    private MainActivity mMainActivity;

    public HelpNWFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setAppTitle(getResources().getString(R.string.help));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.helpfrag, container, false);
        }
        return rootView;
    }


    private void init() {


        mHelpNames = new String[]{getString(R.string.wat_is_meem_nw), getString(R.string.terminology),getString(R.string.how_it_works),getString(R.string.getting_started),getString(R.string.using_meem_computing),getString(R.string.platform_supported),getString(R.string.sms_message_music)};

        mListView = (ListView) rootView.findViewById(R.id.helpList);
        mPbar = (ProgressBar) rootView.findViewById(R.id.pBar);
        mPbar.setVisibility(View.VISIBLE);
        mListView.setAdapter(new HelpListAdapter(getActivity(), mHelpNames));
//        mListView.addFooterView(new View(getActivity()), null, true);
        mPbar.setVisibility(View.GONE);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Bundle bundle = new Bundle();
                bundle.putInt(helpItem, position);
                HelpNWDescriptionFragment helpDescriptionFragment = new HelpNWDescriptionFragment();
                helpDescriptionFragment.setArguments(bundle);

                mMainActivity.showFragment(helpDescriptionFragment, isBackStack, false);

            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(new Bundle());
    }

    @Override
    public void onClick(View v) {


    }
}
