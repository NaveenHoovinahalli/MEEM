package com.meem.ui.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import com.meem.androidapp.R;


/**
 * Created by SCS on 12/16/2016.
 */

public class CustomDialogProgress extends ProgressDialog {

    private TextView mMsgText;

    public CustomDialogProgress(Context context) {
        super(context);
    }


    public CustomDialogProgress(Context context, int theme) {
        super(context, theme);
    }

    public static ProgressDialog ctor(Context context) {
        ProgressDialog dialog = new CustomDialogProgress(context);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progressbarwithmessage);
        mMsgText = (TextView) findViewById(R.id.progressMsg);
    }

    public void setCustomMessage(String msg) {
        mMsgText.setText(msg);
    }
}
