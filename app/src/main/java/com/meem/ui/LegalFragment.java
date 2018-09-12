package com.meem.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.meem.androidapp.R;


/**
 * Created by SCS on 7/4/2016.
 */
public class LegalFragment extends Fragment {

    LinearLayout child;
    LinearLayout parentView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.legalfrag, container, false);
        child = (LinearLayout) mRootView.findViewById(R.id.textView);
        parentView = (LinearLayout) mRootView.findViewById(R.id.parent);

//        final GestureDetectorCompat gd = new GestureDetectorCompat(UiContext.getInstance().getAppContext(), new MeemGestureDetector(this, parentView, child));

        return mRootView;
    }
}
