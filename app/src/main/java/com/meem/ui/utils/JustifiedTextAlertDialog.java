package com.meem.ui.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;

/**
 * Created by arun on 17/8/17.
 */

public class JustifiedTextAlertDialog {
    AlertDialog mAlertDialog;
    Runnable mOnOK;

    public void create(Context activityCtxt, String title, String message, Runnable onOK) {
        mOnOK = onOK;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activityCtxt);
        LayoutInflater inflater = (LayoutInflater) activityCtxt.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.justified_text_dialog, null);
        dialogBuilder.setView(dialogView);

        final JustifiedTextView jtv = (JustifiedTextView) dialogView.findViewById(R.id.text_for_justified_text_dialog);

        dialogBuilder.setTitle(title);
        jtv.setText(message);

        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mAlertDialog.dismiss();
                if(null != mOnOK) mOnOK.run();
            }
        });

        mAlertDialog = dialogBuilder.create();
    }

    public AlertDialog getAlertDialogObject() {
        return mAlertDialog;
    }

    public void show() {
        if(null != mAlertDialog) mAlertDialog.show();
    }

    public void dismiss() {
        if(null != mAlertDialog) mAlertDialog.dismiss();
    }
}
