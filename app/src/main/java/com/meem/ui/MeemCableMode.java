package com.meem.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

/**
 * Created by SCS on 5/22/2017.
 */

public class MeemCableMode extends Fragment implements View.OnClickListener {

    View view;
    LinearLayout one, two, three;
    MainActivity mMainActivity;
    CablePresenter mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.meemcablemode, container, false);
        init();
        return view;
    }

    private void init() {
        one = (LinearLayout) view.findViewById(R.id.one);
        two = (LinearLayout) view.findViewById(R.id.two);
        one.setOnClickListener(this);
        two.setOnClickListener(this);

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) getActivity();
        setEventListner(mMainActivity.getCablePresenter());
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.one:
                showDialogBox(1);
                break;
            case R.id.two:
                showDialogBox(2);
                break;
        }

    }


    private void showDialogBox(final int i) {

        final AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder(getActivity());

        if(i==1){

            alertdialogbuilder.setMessage("Once you press OK,MEEM will disconnect from phone and start communicating with your desktop." +
                    " \n Please make sure MEEM is connected to your desktop before changing to this mode.");

        }else {
            alertdialogbuilder.setMessage("Are you sure you want to proceed?");
        }
        alertdialogbuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //request for change

                if(i==1){
                    mListener.switchToDesktopMode(null);
                }else{
                    mListener.switchToBypassMode(null);
                }
                mMainActivity.onBackPressed();

            }
        });

        alertdialogbuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        });
        AlertDialog alertDialog = alertdialogbuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void setEventListner(CablePresenter eventListner) {
        mListener = eventListner;
    }
}
