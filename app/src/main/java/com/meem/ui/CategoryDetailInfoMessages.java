package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.util.ArrayList;

/**
 * Created by Naveen on 8/4/2016.
 * Display the messages from the database provided by bundle.
 */
public class CategoryDetailInfoMessages extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    static String mVaultId;
    final String TAG = "CategoryDetail";
    View mRootView;
    ListView mList;
    MainActivity mMainActivity;
    MeemTextView mMirrorTV, mMirrorPlusTV;
    boolean isMirror = true;
    SQLiteDatabase db;
    MeemTabSelection mMeemTabSelection;
    boolean isGreenMeem, isSelectMode;
    MessagesCursorAdapter adapter;
    RestoreOrShareSmartDataInterface mListener;
    RelativeLayout mParentView;
    Byte catCode = MMPConstants.MMP_CATCODE_MESSAGE;
    Cursor cursor;
    String mDBPath;
    String mDBFolder;
    ArrayList<SmartDataInfo> mMap;
    DbOperationAsync dbOperationAsync;
    String DB_FULL_PATH;
    private ProgressDialog mInitProgress;
    private String mDBName;


    public CategoryDetailInfoMessages() {

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
            mRootView = inflater.inflate(R.layout.categorydetailsmessageslist, null);
            init();
        }
        return mRootView;
    }

    private void init() {
        mMeemTabSelection = new MeemTabSelection(mMainActivity);
        mInitProgress = CustomDialogProgress.ctor(getContext());
        fetchViewById();
        setTabBaground();
        setClickListner();
        setmListView();

    }

    private void fetchViewById() {
        mList = (ListView) mRootView.findViewById(R.id.list);
        mMirrorPlusTV = (MeemTextView) mRootView.findViewById(R.id.mirrorPlusBtn);
        mMirrorTV = (MeemTextView) mRootView.findViewById(R.id.mirrorBtn);
        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);


    }

    private void openDatabase() {

        if (db != null && db.isOpen()) {
            db.close();
        }

        db = SQLiteDatabase.openDatabase(DB_FULL_PATH, null, SQLiteDatabase.OPEN_READWRITE);

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

    private void closeDatabase() {
        if (db != null && db.isOpen()) db.close();
    }

    private void setTabBaground() {

        if (isMirror) {
            mMeemTabSelection.setMirror(mMirrorTV, mMirrorPlusTV, isGreenMeem);

        } else {
            mMeemTabSelection.setMirrorPlus(mMirrorTV, mMirrorPlusTV, isGreenMeem);

        }
    }

    private void setmListView() {
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        dbOperationAsync = new DbOperationAsync();
        dbOperationAsync.execute();
        closeDatabase();
    }

    private void usingCursorAdapter() {

        mDBPath = mDBFolder;
        AppLocalData appData = AppLocalData.getInstance();

        int isMirr;
        if (isMirror) {
            isMirr = 0;
            mDBName = "_" + mVaultId + "_messages_sync.db";
            DB_FULL_PATH = appData.getMessageV2MirrorDbFullPath(mVaultId);
        } else {
            isMirr = 1;
            mDBName = "_" + mVaultId + "_messages_archive.db";
            DB_FULL_PATH = appData.getMessageV2PlusDbFullPath(mVaultId);

        }


        if (mMainActivity.getCableVersion() == 1) {

            DB_FULL_PATH = mDBPath;
            openDatabase();

            String query = "SELECT checksum as _id,address,date,date_sent,body" +
                    " FROM sms_msg_table" +
                    "  where checksum in (SELECT checksum FROM vault_message_table " +
                    "where upid='" + mVaultId + "' and ismirr=" + isMirr + ")";

            cursor = db.rawQuery(query, null);
        } else {

            GenUtils.copyDbFileToDownloads(mDBName, mDBName);

            cursor = null;

            openDatabase();

            String query = "SELECT checksum as _id,address,date,date_sent,body" +
                    " FROM sms_msg_table";

            cursor = db.rawQuery(query, null);
        }


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

        MessagesItemDetails messagesItemDetails = new MessagesItemDetails();
        Bundle bundle = new Bundle();
        bundle.putString(DetailsFragment.vaultId, mVaultId);
        bundle.putInt("checksum", cursor.getInt(0));
        bundle.putString("NAME", cursor.getString(1));
        bundle.putString("MESSAGE", cursor.getString(4));
        bundle.putString("DATE", cursor.getString(2));
        bundle.putString("SENTDATE", cursor.getString(3));
        bundle.putBoolean("isMirror", isMirror);
        messagesItemDetails.setArguments(bundle);
        mMainActivity.showFragment(messagesItemDetails, true, false);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.mirrorBtn) {
            if (isSelectMode) onBackButtonPressed();
            isMirror = true;
            setTabBaground();
            setmListView();
        } else {
            if (isSelectMode) onBackButtonPressed();
            isMirror = false;
            setTabBaground();
            setmListView();
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
        Share();

    }

    @Override
    public void onRestoreIconListener() {

        mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }

        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mMainActivity.requestSMSManagementPermission(true);
        mListener.onRestoreSmartData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
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

    public void Share() {

        String shareBody = "";
        String line = " ";
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        for (SmartDataInfo smartDataInfo : mMap) {
            cursor.moveToPosition(smartDataInfo.position);


            shareBody = shareBody + " \n" + getString(R.string.name) + " :" + cursor.getString(1) + "\n" + getString(R.string.messages) + " :" + cursor.getString(4) + "\n" + /*getString(R.string.date)+" :" + */ cursor.getString(2); // TODO: Arun: Translation for date.

            shareBody = shareBody + "\n" + line;

        }

        mInitProgress.dismiss();


        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_messages)));
    }

    private void setList() {

        adapter = new MessagesCursorAdapter(getContext(), cursor);
        mList.setAdapter(adapter);
        setAppTitle();
        mInitProgress.dismiss();


    }

    public void setAppTitle() {

        if (cursor == null || cursor.getCount() == 0) {
            mMainActivity.setAppTitle(getResources().getString(R.string.messages));
        } else {
            mMainActivity.setAppTitle(getResources().getString(R.string.messages) + " (" + cursor.getCount() + ")");

        }

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
