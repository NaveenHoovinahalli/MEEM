package com.meem.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;
import com.meem.ui.utils.RoundedImageView;

import java.util.ArrayList;

/**
 * Created by Naveen on 8/4/2016.
 * Selected contact information is displayed here .
 */
public class ContactItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener, View.OnClickListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    View mRootView;
    MainActivity mMainActivity;


    Cursor cursor;
    String mVaultId;
    Byte catCode = MMPConstants.MMP_CATCODE_CONTACT;
    int checksum;
    SQLiteDatabase db;
    MeemTextView mName, mMobile, mHome, mWork, mEmail;
    String name, mobile, work, home, email;
    byte[] bob;
    boolean isMirror = true;
    private RestoreOrShareSmartDataInterface mListener;
    private RelativeLayout mParentView;
    private RoundedImageView mImage;
    private String mDBPath;
    private ProgressDialog mInitProgress;

    public ContactItemDetails() {

    }

    private static byte[] str64TobyteArr(String imageDataString) {
        return Base64.decode(imageDataString, Base64.DEFAULT);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        isMirror = getArguments().getBoolean("isMirror");
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        mDBPath = getArguments().getString("DBPATH");
        checksum = getArguments().getInt("checksum");
        mRootView = inflater.inflate(R.layout.contactdetails, null);
        init();
        return mRootView;

    }

    private void openDatabase() {

        if (db != null && db.isOpen()) {
            return;
        }

        db = SQLiteDatabase.openDatabase(mDBPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    private void closeDatabase() {
        if (db != null && db.isOpen()) db.close();

    }

    public void queryFromBD() {

        String query = "SELECT DISTINCT c.checksum as _id,c.displayname,p.number,e.email_id,p.type,c.photobitmap\n" +
                "               FROM contacts_table as c left outer join phone_list_table as p \n" +
                "                on c.checksum=p.checksum \n" +
                "               left outer join email_list_table as e \n" +
                "                on c.checksum=e.checksum  \n" +
                "               where c.checksum =" + checksum;

        cursor = db.rawQuery(query, null);


        while (cursor.moveToNext()) {
            name = (cursor.getString(1));
            email = (cursor.getString(3));
            String bob1 = cursor.getString(5);
            if (bob1 != null && !bob1.equals(""))
                bob = str64TobyteArr(bob1);
            setNumber(cursor.getInt(4));
        }

    }

    private void setNumber(int type) {
//        type is used to differentiate  between home,mobile and work
        if (type == 1) {
            home = (cursor.getString(2));
        } else if (type == 2) {
            mobile = (cursor.getString(2));
        } else {
            work = (cursor.getString(2));
        }
    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        closeDatabase();
        openDatabase();
        setOnClickListener();
        CheckPermission();
        queryFromBD();
        setValues();
    }

    private void setValues() {


        mName.setText(name);
        mMobile.setText(mobile);
        mHome.setText(home);
        mWork.setText(work);
        mEmail.setText(email);
        if (bob != null && !bob.equals("")) {
            mImage.setImageBitmap(BitmapFactory.decodeByteArray(bob, 0, bob.length));
        }

    }

    private void setOnClickListener() {
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        openDatabase();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.showShareAndRestoreIcon(true);
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getResources().getString(R.string.contacts));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    private void findViewById() {

        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mName = (MeemTextView) mRootView.findViewById(R.id.name);
        mMobile = (MeemTextView) mRootView.findViewById(R.id.mobileNo);
        mHome = (MeemTextView) mRootView.findViewById(R.id.homeNo);
        mWork = (MeemTextView) mRootView.findViewById(R.id.workNo);
        mEmail = (MeemTextView) mRootView.findViewById(R.id.mail);
        mImage = (RoundedImageView) mRootView.findViewById(R.id.profile);

    }


    @Override
    public void onPause() {
        super.onPause();
        closeDatabase();
        mMainActivity.showShareAndRestoreIcon(false);
        mMainActivity.setOptionMenuContent(false, false, false, false);
    }


    @Override
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {
        ShareContact();

    }

    @Override
    public void onRestoreIconListener() {
        ArrayList<SmartDataInfo> smartDataInfoArrayList = new ArrayList<>();
        SmartDataInfo smartDataInfo = new SmartDataInfo();
        smartDataInfo.srcCsum = checksum;
        smartDataInfoArrayList.add(smartDataInfo);

        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onRestoreSmartData(mVaultId, catCode, smartDataInfoArrayList, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mInitProgress.dismiss();
                if (result) {
                    mMainActivity.showToast(getString(R.string.success));
                } else {
                    mMainActivity.showToast(getString(R.string.failed));
                }
                return false;
            }
        });
    }

    @Override
    public void onSelectIconClickListener() {

    }

    @Override
    public void onBackButtonPressed() {


    }

    @Override
    public void onDeleteIconClickListener() {

    }


    public void ShareContact() {

        String shareBody = "";
        String line = " ";
        shareBody = shareBody + " \nName: " + name + "\n" + "Contact No: " + mobile + "\n" + "Email: " + email;


        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_contacts)));

    }


    @Override
    public void onClick(View v) {

    }

    public void CheckPermission() {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "Permission Missing");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        } else Log.d("Permission", "Permission Available");


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getContext(), "Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Denied", Toast.LENGTH_SHORT).show();

                }
        }
    }


}
