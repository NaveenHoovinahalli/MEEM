package com.meem.androidapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogSenderActivity extends Activity {
    private static final String tag = "LogSenderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (action.equals("com.meem.androidapp.CHECK_LOCAL_DATA_PRIVACY")) {
            Log.d(tag, "Intent: Check local data priv");
            setResult(ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE ? 1 : -1);
        } else if (action.equals("com.meem.androidapp.SEND_DEBUG_LOGS")) {
            Log.d(tag, "Intent: Send log files");
            sendLogFiles();
        }

        finish();
    }

    private void sendLogFiles() {
        String logFiles[] = {"MainActivity.log", "Backup.log", "Restore.log", "Storage.log", "MeemCable.log", "MeemCore.log", "AccessoryFragment.log", "Accessory.log", "FileSender.log", "FileReceiver.log"};
        String meemFiles[] = {"pinf.mml", "mstat.mml", "vstat.mml", "sesd.mml", "datd.mml", "Firmware.log"};

        File appLocalDataFolder;
        String appLocalDataFolderPath;

        if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            appLocalDataFolder = getApplicationContext().getFilesDir();
            if (appLocalDataFolder == null) {
                appLocalDataFolder = getApplicationContext().getFilesDir();
            }
        } else {
            appLocalDataFolder = Environment.getExternalStorageDirectory();
        }

        if (appLocalDataFolder == null) {
            return;
        }

        appLocalDataFolderPath = appLocalDataFolder.getAbsolutePath();

        String appRootFolderPath = appLocalDataFolderPath + File.separator + "MeemAndroid" + File.separator;
        String appLogFolderPath = appLocalDataFolderPath + File.separator + "MeemAndroid/Logs" + File.separator;

        ArrayList<String> attaches = new ArrayList<String>();

        File rootFolder = new File(appRootFolderPath);
        if (rootFolder.exists()) {
            for (String meemFile : meemFiles) {
                String meemFilePath = appRootFolderPath + meemFile;

                File fileObj = new File(meemFilePath);
                if (fileObj.exists() && !fileObj.isDirectory()) {
                    attaches.add(meemFilePath);
                }
            }
        }

        File logFolder = new File(appLogFolderPath);
        if (logFolder.exists()) {
            for (String logFile : logFiles) {
                String logFilePath = appLogFolderPath + logFile;

                File fileObj = new File(logFilePath);
                if (fileObj.exists() && !fileObj.isDirectory()) {
                    attaches.add(logFilePath);
                }
            }
        }

        Log.d("MeemAndroid", attaches.toString());

        email(this, ProductSpecs.CONTACT_EMAIL, null, "[Meem Android] Debug logs", "Meem debug log files (that are available) are attached. You can add your remarks here and then just send this mail.", attaches);
    }

    public void email(Context context, String emailTo, String emailCC, String subject, String emailText, List<String> filePaths) {
        // need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailTo});
        /*emailIntent.putExtra(android.content.Intent.EXTRA_CC,
                new String[] { emailCC });*/
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailText);

        // has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();

        // convert from paths to Android friendly Parcelable Uri's
        for (String file : filePaths) {
            Log.d("MeemAndroid", "Attaching file: " + file);
            File fileIn = new File(file);
            Uri u;
            if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
                u = FileProvider.getUriForFile(context, "com.meem.androidapp.fileprovider", fileIn);
            } else {
                u = Uri.fromFile(fileIn);
            }
            uris.add(u);
        }

        Log.d("MeemAndroid", "Attached total: " + uris.size() + " files to email.");

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Sorry, No email application was found", Toast.LENGTH_SHORT).show();
        }
    }
}
