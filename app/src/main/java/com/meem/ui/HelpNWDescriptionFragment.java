package com.meem.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;


/**
 * Created by SCS on 7/11/2016.
 */
public class HelpNWDescriptionFragment extends Fragment implements ViewPager.OnPageChangeListener {

    public String[] mHelpNames;
    int mHelpItem;
    MainActivity mMainActivity;
    private PagerAdapter mPagerAdapter;
    private ViewPager mPager;


    public HelpNWDescriptionFragment() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mHelpItem = getArguments().getInt("HELPITEM");
        View rootView = inflater.inflate(R.layout.helpdescriptionmain, container, false);
        mPager = (ViewPager) rootView.findViewById(R.id.helpDescriptionPager);
        mHelpNames = new String[]{getString(R.string.wat_is_meem_nw), getString(R.string.terminology),getString(R.string.how_it_works),getString(R.string.getting_started),getString(R.string.using_meem_computing),getString(R.string.platform_supported),getString(R.string.sms_message_music)};

        initAdapter();
        return rootView;
    }

    private void initAdapter() {

        mPagerAdapter = new ScreenSlidePagerAdapter(getActivity().getSupportFragmentManager(), mHelpItem);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mHelpItem);
        mPager.addOnPageChangeListener(this);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setAppTitle(mHelpNames[mHelpItem]);

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mMainActivity.setAppTitle(mHelpNames[position]);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        int mPosition;


        public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fragmentManager, int mHelpItem) {

            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("HELPITEM", position);
            HelpNWPagerAdapter helpPagerAdapter = new HelpNWPagerAdapter();
            helpPagerAdapter.setArguments(bundle);
            return helpPagerAdapter;
        }

        @Override
        public int getCount() {

            return 7;

        }


    }

}
