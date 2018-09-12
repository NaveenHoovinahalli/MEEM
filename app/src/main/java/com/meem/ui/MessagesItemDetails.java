package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import java.util.HashMap;

/**
 * Created by Naveen on 8/4/2016.
 */
public class MessagesItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener, View.OnClickListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    View mRootView;
    MainActivity mMainActivity;

    HashMap haspMap;


    Cursor cursor;
    String mVaultId;
    MeemTextView mName, mNumber, mMessage;
    String name, number, message, date, sentdate;
    boolean isMirror;
    int checkSum;
    private RestoreOrShareSmartDataInterface mListener;
    private RelativeLayout mParentView;
    private MeemTextView mDate;
    private ProgressDialog mInitProgress;


    public MessagesItemDetails() {


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        name = getArguments().getString("NAME");
        message = getArguments().getString("MESSAGE");

        checkSum = getArguments().getInt("checksum");
        date = getArguments().getString("DATE", "");
        sentdate = getArguments().getString("SENTDATE", "");
        isMirror = getArguments().getBoolean("isMirror");

        if (!date.equals("")) date = convertMiliSectoDateFormat(Long.parseLong(date));

        if (!sentdate.equals("")) sentdate = convertMiliSectoDateFormat(Long.parseLong(sentdate));


        mRootView = inflater.inflate(R.layout.messagedetails, null);
        init();
        return mRootView;

    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        setOnClickListener();
        setValues();
    }

    private void setValues() {
        mName.setText(name);
        mMessage.setText(message);
        mDate.setText(date);
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
        mMainActivity.setAppTitle(getResources().getString(R.string.messages));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }


    private void findViewById() {

        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mName = (MeemTextView) mRootView.findViewById(R.id.name);
        mNumber = (MeemTextView) mRootView.findViewById(R.id.number);
        mMessage = (MeemTextView) mRootView.findViewById(R.id.message);
        mDate = (MeemTextView) mRootView.findViewById(R.id.date);

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


    public String convertMiliSectoDateFormat(long seconds) {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy");
        Date date = new Date(seconds);
        return DATE_FORMAT.format(date);
    }

    @Override
    public void onRestoreIconListener() {

        ArrayList<SmartDataInfo> smartDataInfoArrayList = new ArrayList<>();
        SmartDataInfo smartDataInfo = new SmartDataInfo();
        smartDataInfo.srcCsum = checkSum;
        smartDataInfoArrayList.add(smartDataInfo);
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mMainActivity.requestSMSManagementPermission(true);
        mListener.onRestoreSmartData(mVaultId, MMPConstants.MMP_CATCODE_MESSAGE, smartDataInfoArrayList, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mInitProgress.dismiss();
                if (result) {
                    mMainActivity.showToast(getString(R.string.success));
                } else {
                    mMainActivity.showToast(getString(R.string.failed));
                }

                mMainActivity.dropSmsManagementPermission();
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

    public void Share() {
        String shareBody = "";

        shareBody = shareBody + " \n" + "Name: " + name + "\n" + "Message: " + message + "\n" + "Date: " + date;

        shareBody = shareBody + "\n";

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_messages)));
    }


    @Override
    public void onClick(View v) {

    }


}
