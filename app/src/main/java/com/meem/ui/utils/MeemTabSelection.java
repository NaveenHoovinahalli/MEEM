package com.meem.ui.utils;

import android.widget.TextView;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

/**
 * Created by Naveen on 8/11/2016.
 * This class is used to set the TextView background,
 */
public class MeemTabSelection {


    MainActivity mainActivity;

    public MeemTabSelection(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setMirror(TextView mirror, TextView mirrorPlus, boolean isGreenMeem) {

        mirror.setBackgroundColor(mainActivity.getResources().getColor(R.color.meemBlack83));
        mirrorPlus.setBackgroundColor(mainActivity.getResources().getColor(R.color.meemBlack95));

        mirror.setTextColor(mainActivity.getResources().getColor(R.color.meemWhite));
        mirrorPlus.setTextColor(mainActivity.getResources().getColor(R.color.meemBlack50));


    }

    public void setMirrorPlus(TextView mirror, TextView mirrorPlus, boolean isGreenMeem) {

        mirrorPlus.setBackgroundColor(mainActivity.getResources().getColor(R.color.meemBlack83));
        mirror.setBackgroundColor(mainActivity.getResources().getColor(R.color.meemBlack95));

        mirrorPlus.setTextColor(mainActivity.getResources().getColor(R.color.meemWhite));
        mirror.setTextColor(mainActivity.getResources().getColor(R.color.meemBlack50));

    }

}
