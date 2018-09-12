package com.meem.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Not used.
 *
 * @author Arun T A
 */

public class UiUtils {
    public static void showMessageBox(Context context, String title, String message) {
        new AlertDialog.Builder(context).setMessage(message).setTitle(title).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(true).create().show();
    }
}
