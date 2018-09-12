package com.meem.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Naveen on 8/4/2016.
 * It will display individual calender item
 */
public class CalenderItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener, View.OnClickListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    View mRootView;
    MainActivity mMainActivity;

    String mVaultId;
    boolean isMirror = true;
    int checkSum;
    Byte catCode = MMPConstants.MMP_CATCODE_CALENDER;
    MeemTextView mTitle, mDesc, mStartdate, mEnddate, mLocation;
    String startDate, endDate;
    private RestoreOrShareSmartDataInterface mListener;
    private ProgressDialog mInitProgress;


    public CalenderItemDetails() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fetchBundleValues();
        mRootView = inflater.inflate(R.layout.calenderdetails, null);
        init();
        return mRootView;

    }

    private void fetchBundleValues() {

    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        setOnClickListener();
        CheckPermission();
        setValues();
    }

    private void setValues() {
        isMirror = getArguments().getBoolean("isMirror");
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        checkSum = getArguments().getInt("checksum");
        mTitle.setText(getArguments().getString("title", ""));
        mDesc.setText(getArguments().getString("description", ""));

        startDate = getArguments().getString("dtstart", "");
        if (!startDate.equals("")) startDate = convertMiliSectoDateFormat(Long.parseLong(startDate));
        endDate = getArguments().getString("dtend", "");
        if (!endDate.equals("")) endDate = convertMiliSectoDateFormat(Long.parseLong(endDate));

        mStartdate.setText(startDate);
        mEnddate.setText(endDate);
        mLocation.setText(getArguments().getString("event_location", ""));

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
                } else {

                }
        }
    }


    private void setOnClickListener() {
    }


    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.showShareAndRestoreIcon(true);
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getResources().getString(R.string.calendar));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }


    private void findViewById() {


        mTitle = (MeemTextView) mRootView.findViewById(R.id.eventName);
        mDesc = (MeemTextView) mRootView.findViewById(R.id.eventDesc);
        mStartdate = (MeemTextView) mRootView.findViewById(R.id.startDate);
        mEnddate = (MeemTextView) mRootView.findViewById(R.id.endDate);
        mLocation = (MeemTextView) mRootView.findViewById(R.id.location);

    }


    @Override
    public void onPause() {
        super.onPause();
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

        Share();
    }

    @Override
    public void onRestoreIconListener() {

        ArrayList<SmartDataInfo> smartDataInfoArrayList = new ArrayList<>();
        SmartDataInfo smartDataInfo = new SmartDataInfo();
        smartDataInfo.srcCsum = checkSum;
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


    @Override
    public void onClick(View v) {

    }

    public String convertMiliSectoDateFormat(long seconds) {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy");
        Date date = new Date(seconds);
        return DATE_FORMAT.format(date);
    }

    private void Share() {

        String shareBody = " \n" + "Event Name: " + getArguments().getString("title", "") + "\n" + "Description: " + getArguments().getString("description", "") + "\n" + "Start Date: " + startDate + "\n" + "End Date  :" + endDate;

        shareBody = shareBody + "\n";

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_calendar)));
    }


}
