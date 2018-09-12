package com.meem.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.R;
import com.meem.ui.utils.MeemTextView;
import com.meem.utils.GenUtils;
import com.meem.utils.MicroTimeStamp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by SCS on 7/14/2016.
 */
public class HelpPagerAdapter extends Fragment implements View.OnClickListener {

    View rootView;
    WebView mWeb;
    int position;
    MeemTextView mDesc, mButton,mTextOne,mTextTwo,mTextThree,mTextFour,mTextFive,mTextSix,mTextSeven,mTextEight;
    EditText mMailInfo;
    ScrollView mScrollView;
    String BASE_URL;
    String mPhoneName,mVersion;
    String shareBody = "";



    public HelpPagerAdapter() {

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        position = getArguments().getInt("HELPITEM");
        rootView = inflater.inflate(R.layout.helppageritem, null);

        init();
        return rootView;
    }

    private void init() {
        findViewByID();
        String locale = getActivity().getResources().getConfiguration().locale.getCountry();
        locale = locale.toLowerCase();

        if (locale.equals("de") || locale.equals("es") || locale.equals("fr") || locale.equals("it")) {
            locale = locale.toLowerCase() + "_";
        } else {
            locale = "us_";
        }
        BASE_URL = "file:///android_asset/html/" + locale;
        setHtml();
        mButton.setOnClickListener(this);
    }


    private void setHtml() {

        mWeb.setVisibility(View.VISIBLE);
        mScrollView.setVisibility(View.GONE);
        setTVVisibility(false);

        switch (position) {
            case 0:
                html0();
                break;
            case 1:
                html1();
                break;
            case 2:
                html2();
                break;
            case 3:
                html3();
                break;
            case 4:
                html4();
                break;
            case 5:
                html5();
                break;
            case 6:
                html6();
                break;
            case 7:
                html7();
                break;
            case 8:
                html8();
                break;
            case 9:
                html9();
                break;
            case 10:
                html10();
                break;
            case 11:
                html11();
                break;
            case 12:
                html12();
                break;
            case 13:
                html13();
                break;
            case 14:
                html14();
                break;
            case 15:
                html15();
                break;
            case 16:
                html16();
                break;
            case 17:
                html17();
                break;
            case 18:
                html18();
                break;

        }
    }

    private void setTVVisibility(boolean b) {
        if(b){

            mTextOne.setVisibility(View.VISIBLE);
            mTextTwo.setVisibility(View.VISIBLE);
            mTextThree.setVisibility(View.VISIBLE);
            mTextFour.setVisibility(View.VISIBLE);
            mTextFive.setVisibility(View.GONE);
            mTextSix.setVisibility(View.VISIBLE);
            mTextSeven.setVisibility(View.GONE);
            mTextEight.setVisibility(View.VISIBLE);
            mMailInfo.setVisibility(View.VISIBLE);

        }else {

            mTextOne.setVisibility(View.GONE);
            mTextTwo.setVisibility(View.GONE);
            mTextThree.setVisibility(View.GONE);
            mTextFour.setVisibility(View.GONE);
            mTextFive.setVisibility(View.GONE);
            mTextSix.setVisibility(View.GONE);
            mTextSeven.setVisibility(View.GONE);
            mTextEight.setVisibility(View.GONE);
            mMailInfo.setVisibility(View.GONE);

        }
    }


    private void html0() {
        mWeb.loadUrl(BASE_URL + "WhatisMEEM.html");
    }

    private void html1() {
        mWeb.loadUrl(BASE_URL + "MyPersonalDataonMEEM.html");

    }

    private void html2() {
        mWeb.loadUrl(BASE_URL + "UnderstandingtheApp.html");

    }

    private void html3() {
        mWeb.loadUrl(BASE_URL + "IsMyMEEMBackingUp.html");

    }

    private void html4() {
        mWeb.loadUrl(BASE_URL + "AutomaticBackup.html");

    }

    private void html5() {
        mWeb.loadUrl(BASE_URL + "OriginalDeviceManualBackupandRestore.html");

    }

    private void html6() {
        mWeb.loadUrl(BASE_URL + "ChangingAutomaticSyncSettings.html");

    }

    private void html7() {
        mWeb.loadUrl(BASE_URL + "MultipleDevices.html");
    }

    private void html8() {
        mWeb.loadUrl(BASE_URL + "CopyDataFromAnAdditionalDevice.html");

    }

    private void html9() {
        mWeb.loadUrl(BASE_URL + "TransferringSelectedItemsBetweenDevices.html");

    }

    private void html10() {
        mWeb.loadUrl(BASE_URL + "Summary.html");

    }

    private void html11() {
        mWeb.loadUrl(BASE_URL + "SharingSavedItemstoOtherApps.html");


    }

    private void html12() {

        mWeb.loadUrl(BASE_URL + "Settings.html");

    }

    private void html13() {
        mWeb.loadUrl(BASE_URL + "meemdesktop.html");

    }
    private void html14() {
        mWeb.loadUrl(BASE_URL + "meemdatacablemode.html");

    }
    private void html15() {
        mWeb.loadUrl(BASE_URL + "meemnetwork.html");

    }
    private void html16() {
        mWeb.loadUrl(BASE_URL + "have_an_issue.html");

    }
    private void html17() {
        mWeb.setVisibility(View.GONE);
        mScrollView.setVisibility(View.VISIBLE);
        setTVVisibility(true);

        mPhoneName = Build.BRAND.toUpperCase();
        mVersion= Build.VERSION.RELEASE;


        mTextSix.setText("Device info: "+mPhoneName + "  "+mVersion);
        mDesc.setText(getString(R.string.diagnostic_msg));
        mDesc.setVisibility(View.GONE);
        mButton.setText(getString(R.string.diagnostic_button));
    }

    private void html18() {

        mWeb.setVisibility(View.GONE);
        mScrollView.setVisibility(View.VISIBLE);

        mDesc.setText(getString(R.string.contactus_msg));
        mButton.setText(getString(R.string.help14));


    }




    private void findViewByID() {
        mWeb = (WebView) rootView.findViewById(R.id.helpWeb);
        mDesc = (MeemTextView) rootView.findViewById(R.id.tvDesc);
        mButton = (MeemTextView) rootView.findViewById(R.id.tvButton);
        mScrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        mTextOne= (MeemTextView) rootView.findViewById(R.id.textOne);
        mTextTwo= (MeemTextView) rootView.findViewById(R.id.textTwo);
        mTextThree= (MeemTextView) rootView.findViewById(R.id.textThree);
        mTextFour= (MeemTextView) rootView.findViewById(R.id.textFour);
        mTextFive= (MeemTextView) rootView.findViewById(R.id.textFive);
        mTextSix= (MeemTextView) rootView.findViewById(R.id.textSix);
        mTextSeven= (MeemTextView) rootView.findViewById(R.id.textSeven);
        mTextEight= (MeemTextView) rootView.findViewById(R.id.textEight);
        mMailInfo= (EditText) rootView.findViewById(R.id.etOne);

    }

    @Override
    public void onClick(View v) {

        if (position == 18) {

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ProductSpecs.CONTACT_EMAIL});

            startActivity(intent);
        } else {



            shareBody = mTextSix.getText() + "\n \n "  + mMailInfo.getText()+ "\n\n" ;

            sendLogFiles();
        }


    }

    private void sendLogFiles() {
        String logFiles[] = {
                "MainActivity.log",
                "CableInitV2.log",

                "Backup.log",
                "BackupV2.log",

                "Restore.log",
                "RestoreV2.log",

                "Session.log",
                "SessionV2.log",

                "SessionManager.log",
                "SessionManagerV2.log",

                "Contacts.log",
                "ContactsV2.log",

                "Calenders.log",
                "CalendersV2.log",

                "Messages.log",
                "MessagesV2.log",

                "Storage.log",
                "MeemCable.log",

                "MeemCore.log",
                "MeemCoreV2.log",

                "AccessoryFragment.log",
                "Accessory.log",

                "FileSender.log",
                "FileSenderV2.log",

                "FileReceiver.log",
                "FileReceiverV2.log",

                "CableDriver.log",
                "CableDriverV1.log",
                "CableDriverV2.log",

                "CablePresenter.log",
                "CablePresenterV1.log",
                "CablePresenterV2.log",

                "Configdb.log",
                "SecureDb.log",
                "CableModelBuilder.log",

                "GenUtils.log",
                "InitSequence.log"
        };

        String meemFiles[] = {"pinf.mml", "mstat.mml", "vcfg.mml", "sesd.mml", "datd.mml", "Firmware.log"};

        File appLocalDataFolder;
        String appLocalDataFolderPath;

        if (ProductSpecs.MEEM_APP_LOCAL_DATA_PRIVATE) {
            appLocalDataFolder = getActivity().getApplicationContext().getFilesDir();
            if (appLocalDataFolder == null) {
                appLocalDataFolder = getActivity().getApplicationContext().getFilesDir();
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

        String caseID = GenUtils.genUniqueString(MicroTimeStamp.INSTANCE.get(), 8);
        String subject = "Meem Android: Diagnostic report (id: " + caseID + ")";

        email(getActivity(), ProductSpecs.CONTACT_EMAIL, null, subject, shareBody +"The diagnostic log files are attached. Please add your remarks here (if any) and then send this mail.\nThis diagnostic report id is: " + caseID + "\n", attaches);
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
