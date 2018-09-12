package com.meem.androidapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

/**
 * This class deals with the document provider framework support for SDCARD available in Android version Lollipop and up. On Lollipop and
 * up, this class essentially checks/requests for SDCARD write access using a system intent and wait for the result. Once user selects the
 * SDCARD folder - preferably root folder - to which the he grants us write access is obtained from onActivityResult, this activity will
 * broadcast it to MainActivity. From main activity, this information will be updated to Storage class.
 *
 * @author Arun T A
 * @see developer.android.com/guide/topics/providers/document-provider.html #client
 * @see stackoverflow.com/questions/26744842/how-to-use-the-new-sd-card-access -api-presented-for-lollipop
 * <p>
 * 15Dec2015
 */

@SuppressLint("InlinedApi")
public class SdCardAccessActivity extends Activity {
    private static final int REQUEST_CODE_SDCARD_ALL_ACCESS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(getApplicationContext(), "Select SDCARD root", Toast.LENGTH_LONG).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_CODE_SDCARD_ALL_ACCESS);
        } else {
            // TODO
        }
    }

    @SuppressLint("NewApi")
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == REQUEST_CODE_SDCARD_ALL_ACCESS) {
            Uri treeUri = null;
            if (resultCode == Activity.RESULT_OK) {
                // get Uri from Storage Access Framework.
                treeUri = resultData.getData();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                String pickedPath = pickedDir.getName();

                // Persist access permissions. TODO: check if we need to & this with resultData.getFlags()
                int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                    Toast.makeText(getApplicationContext(), pickedPath, Toast.LENGTH_LONG).show();
                }
            }
        }

        finish();
    }
}
