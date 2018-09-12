package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTabSelection;
import com.meem.ui.utils.MeemTextView;
import com.meem.utils.GenUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Naveen on 8/4/2016.
 * List out the calender items from the database using the path available from the bundle ,
 * Passing the cursor to the CalenderCursorAdapter.class to parse the data
 */
public class CategoryDetailInfoCalender extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    final String TAG = "CategoryDetailInfoCalender";

    View mRootView;
    ListView mList;
    MainActivity mMainActivity;

    MeemTextView mMirrorTV, mMirrorPlusTV;
    boolean isMirror = true;
    SQLiteDatabase db;
    MeemTabSelection mMeemTabSelection;
    boolean isGreenMeem, isSelectMode;
    CalenderCursorAdapter adapter;
    RestoreOrShareSmartDataInterface mListener;
    String mVaultId;
    String mDBFolder;

    RelativeLayout mParentView;
    Byte catCode = MMPConstants.MMP_CATCODE_CALENDER;
    Cursor cursor;
    ArrayList<SmartDataInfo> mMap;

    DbOperationAsync dbOperationAsync;
    String DB_FULL_PATH;
    private String mDBPath;
    private String mDBName;
    private ProgressDialog mInitProgress;

    public CategoryDetailInfoCalender() {

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        mDBPath = getArguments().getString("DBPATH");
        mDBFolder = mDBPath;

        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.categorydetailscalenderlist, null);
            init();

        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void init() {
        mMeemTabSelection = new MeemTabSelection(mMainActivity);
        mInitProgress = CustomDialogProgress.ctor(getContext());
        fetchViewById();
        setTabBaground();
        setClickListner();
        setListView();

    }

    private void fetchViewById() {
        mList = (ListView) mRootView.findViewById(R.id.list);
        mMirrorPlusTV = (MeemTextView) mRootView.findViewById(R.id.mirrorPlusBtn);
        mMirrorTV = (MeemTextView) mRootView.findViewById(R.id.mirrorBtn);
        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);


    }

    private void setListView() {

        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        dbOperationAsync = new DbOperationAsync();

        dbOperationAsync.execute();

    }

    private void setTabBaground() {

        if (isMirror) {
            mMeemTabSelection.setMirror(mMirrorTV, mMirrorPlusTV, isGreenMeem);

        } else {
            mMeemTabSelection.setMirrorPlus(mMirrorTV, mMirrorPlusTV, isGreenMeem);

        }
    }

    private void openDatabase() {

        if (db != null && db.isOpen()) {
            db.close();
        }

        db = SQLiteDatabase.openDatabase(DB_FULL_PATH, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private void setClickListner() {
        mList.setOnItemClickListener(this);
        mMirrorTV.setOnClickListener(this);
        mMirrorPlusTV.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (isSelectMode) return;

        cursor.moveToPosition(position);

        CalenderItemDetails calenderItemDetails = new CalenderItemDetails();
        Bundle bundle = new Bundle();
        bundle.putString(DetailsFragment.vaultId, mVaultId);
        bundle.putInt("checksum", cursor.getInt(0));
        bundle.putString("title", cursor.getString(6));
        bundle.putString("description", cursor.getString(2));
        bundle.putString("event_location", cursor.getString(3));
        bundle.putString("dtstart", cursor.getString(4));
        bundle.putString("dtend", cursor.getString(5));
        bundle.putBoolean("isMirror", isMirror);
        calenderItemDetails.setArguments(bundle);
        mMainActivity.showFragment(calenderItemDetails, true, false);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.mirrorBtn) {
            if (isSelectMode) onBackButtonPressed();
            isMirror = true;
            setTabBaground();
            setListView();

        } else {
            if (isSelectMode) onBackButtonPressed();
            isMirror = false;
            setTabBaground();
            setListView();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        setAppTitle();
        if (!isSelectMode) {
            mMainActivity.setBackPressDisable(false);
            mMainActivity.showSelectIconsHideShareRestore();
            mMainActivity.setOptionMenuContent(false, false, false, false);
        } else {
            mMainActivity.setBackPressDisable(true);
            mMainActivity.showShareandRestoreIconsHideSelect();
            mMainActivity.setOptionMenuContent(true, true, true, false);
        }
    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainActivity.hideToolbarIcons();

    }

    private void closeDatabase() {
        if (db != null && db.isOpen()) db.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbOperationAsync.cancel(true);
        closeDatabase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mInitProgress.isShowing())
            mInitProgress.dismiss();

    }

    @Override
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {
        mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }
        ShareCalender();

    }

    private void ShareCalender() {


        String shareBody = "";
        String line = " ";
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        for (SmartDataInfo smartDataInfo : mMap) {

            cursor.moveToPosition(smartDataInfo.position);
            shareBody = shareBody + "\n" + line;

            String startDate = cursor.getString(4);
            if (!startDate.equals("")) startDate = convertMiliSectoDateFormat(Long.parseLong(startDate));
            String endDate = cursor.getString(5);
            if (!endDate.equals("")) endDate = convertMiliSectoDateFormat(Long.parseLong(endDate));

            shareBody = shareBody + " \n" + "Event Name: " + cursor.getString(6) + "\n" + "Description: " + cursor.getString(2) + "\n" + "Start Date: " + startDate + "\n" + "End Date  :" + endDate;

            shareBody = shareBody + "\n" + line;

        }

        mInitProgress.dismiss();

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_calendar)));

    }

    public String convertMiliSectoDateFormat(long seconds) {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy");
        Date date = new Date(seconds);
        return DATE_FORMAT.format(date);
    }

    @Override
    public void onRestoreIconListener() {
        ArrayList<SmartDataInfo> mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }

        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onRestoreSmartData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
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
        isSelectMode = true;
        mMainActivity.showShareandRestoreIconsHideSelect();
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setBackPressDisable(true);
        adapter.setCheckBoxVisible(true);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onBackButtonPressed() {

        mMainActivity.showSelectIconsHideShareRestore();
        mMainActivity.setOptionMenuContent(false, false, false, false);
        mMainActivity.setBackPressDisable(false);
        isSelectMode = false;
        adapter.setCheckBoxVisible(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDeleteIconClickListener() {

    }

    private void setList() {
        adapter = new CalenderCursorAdapter(getContext(), cursor);
        mList.setAdapter(adapter);
        setAppTitle();
        mInitProgress.dismiss();
    }

    public void setAppTitle(){

        if(cursor==null || cursor.getCount()==0){
            mMainActivity.setAppTitle(getResources().getString(R.string.calendar));
        }else {
            mMainActivity.setAppTitle(getResources().getString(R.string.calendar) + " ("+cursor.getCount()+")");

        }

    }
    private void usingCursorAdapter() {


        mDBPath = mDBFolder;

        AppLocalData appData = AppLocalData.getInstance();

        int isMirr;
        if (isMirror) {
            isMirr = 0;
            mDBName = "_" + mVaultId + "_calendar_sync.db";
            DB_FULL_PATH = appData.getCalendarV2MirrorDbFullPath(mVaultId);

        } else {
            isMirr = 1;
            mDBName = "_" + mVaultId + "_calendar_archive.db";
            DB_FULL_PATH = appData.getCalendarV2PlusDbFullPath(mVaultId);

        }


        String query;

        if (mMainActivity.getCableVersion() == 1) {

            DB_FULL_PATH = mDBPath;

            openDatabase();

            query = "SELECT checksum as _id,event_timezone,description,event_location,dtstart,dtend,title,calendar_displayname " +
                    "FROM calendar_event_table " +
                    " where checksum in (SELECT checksum FROM vault_calendar_table where upid='" + mVaultId + "' and ismirr=" + isMirr + ")";

        } else {

            GenUtils.copyDbFileToDownloads(mDBName, mDBName);

            openDatabase();

            query = "SELECT checksum as _id,event_timezone,description,event_location,dtstart,dtend,title,calendar_displayname " +
                    " FROM calendar_event_table order by dtstart";
        }
        cursor = db.rawQuery(query, null);

    }

    private class DbOperationAsync extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {

            usingCursorAdapter();
            return null;
        }

        @Override
        protected void onPostExecute(Void mVoid) {

            setList();
        }
    }

}
