package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Naveen on 8/4/2016.
 *  List out the contact items from the database using the path available from the bundle ,
 *
 */

//TODO vault id
public class CategoryDetailInfoContacts extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    final String TAG = "CategoryDetailInfoContacts";
    View mRootView;
    ListView mList;
    MainActivity mMainActivity;
    MeemTextView mMirrorTV, mMirrorPlusTV;
    boolean isMirror = true;
    SQLiteDatabase db;
    MeemTabSelection mMeemTabSelection;
    boolean isGreenMeem, isSelectMode;
    ContactsCursorAdapter adapter;
    RestoreOrShareSmartDataInterface mListener;
    String mVaultId;
    RelativeLayout mParentView;
    Cursor cursor;
    ArrayList<SmartDataInfo> mMap;
    Byte catCode = MMPConstants.MMP_CATCODE_CONTACT;
    String mDBFolder;
    DbOperationAsync dbOperationAsync;
    String DB_FULL_PATH;
    private DebugTracer mDebug = new DebugTracer("CategoryDetailInfoContacts", "CategoryDetailInfoContacts.log");
    private String mDBPath;
    private String mDBName;
    private ProgressDialog mInitProgress;


    public CategoryDetailInfoContacts() {

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
            mRootView = inflater.inflate(R.layout.categorydetailscontactslist, null);
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

    private void setmListView() {
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        dbOperationAsync = new DbOperationAsync();
        dbOperationAsync.execute();
    }

    private void usingCursorAdapter() {
        dbgTrace("database  path: " + mDBPath);

        mDBPath = mDBFolder;
        AppLocalData appData = AppLocalData.getInstance();
        int isMirr;
        if (isMirror) {
            isMirr = 0;
            mDBName = "_" + mVaultId + "_contacts_sync.db";
            DB_FULL_PATH = appData.getContactsV2MirrorDbFullPath(mVaultId);
        } else {
            isMirr = 1;
            mDBName = "_" + mVaultId + "_contacts_archive.db";
            DB_FULL_PATH = appData.getContactsV2PlusDbFullPath(mVaultId);
        }


        if (mMainActivity.getCableVersion() == 1) {
            DB_FULL_PATH = mDBPath;
            openDatabase();

            //BugId:9 ,Changed the query to select required category

            String query1="SELECT DISTINCT c.checksum as _id,c.displayname,p.number,e.email_id,p.type" +
                    "                    FROM contacts_table as c left outer join phone_list_table as p    on c.checksum=p.checksum" +
                    "                    left outer join email_list_table as e   on c.checksum=e.checksum" +
                    "                     where c.checksum in (SELECT checksum FROM vault_contacts_table where upid='" + mVaultId + "' and ismirr=" + isMirr + ")"+
                    "                   group by c.checksum order by c.displayname";

            cursor = db.rawQuery(query1, null);
        } else {

            GenUtils.copyDbFileToDownloads(mDBName, mDBName);


            openDatabase();

//BugId:9 ,Changed the query to select required category
            String query = "SELECT DISTINCT c.checksum as _id,c.displayname,p.number,e.email_id,p.type,c.photobitmap" +
                    "                    FROM contacts_table as c left outer join phone_list_table as p" +
                    "                    on c.checksum=p.checksum  left outer join email_list_table as e on c.checksum=e.checksum" +
                    "                    where c.checksum in (select checksum from vault_contacts_table)  group by _id order by c.displayname";

            cursor = db.rawQuery(query, null);
        }


    }

    private void setTabBaground() {

        if (isMirror) {
            mMeemTabSelection.setMirror(mMirrorTV, mMirrorPlusTV, isGreenMeem);
        } else {
            mMeemTabSelection.setMirrorPlus(mMirrorTV, mMirrorPlusTV, isGreenMeem);
        }
    }

    private void openDatabase() {


        Log.d("DBPATH", "DB FinalPath::" + mDBPath);

        dbgTrace("database file : " + mDBPath);


        if (db != null && db.isOpen()) {
            dbgTrace("Closing database : " + mDBPath);

            db.close();
        }


        db = SQLiteDatabase.openDatabase(DB_FULL_PATH, null, SQLiteDatabase.OPEN_READONLY);

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

    private void setClickListner() {
        mList.setOnItemClickListener(this);
        mMirrorTV.setOnClickListener(this);
        mMirrorPlusTV.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (isSelectMode) return;

        cursor.moveToPosition(position);
        int checksum = cursor.getInt(0);

        if (mMainActivity.getCableVersion() == 1) {

            ContactItemDetails contactItemDetails = new ContactItemDetails();
            Bundle bundle = new Bundle();
            bundle.putString(DetailsFragment.vaultId, mVaultId);
            bundle.putString("DBPATH", mDBPath);
            bundle.putInt("checksum", checksum);
            bundle.putBoolean("isMirror", isMirror);
            contactItemDetails.setArguments(bundle);
            mMainActivity.showFragment(contactItemDetails, true, false);
        } else {

            ContactItemDetailsV2 contactItemDetailsV2 = new ContactItemDetailsV2();
            Bundle bundle = new Bundle();
            bundle.putString(DetailsFragment.vaultId, mVaultId);
            bundle.putString("DBPATH", DB_FULL_PATH);
            bundle.putInt("checksum", checksum);
            bundle.putBoolean("isMirror", isMirror);
            bundle.putString("name", cursor.getString(1));
            bundle.putString("number", cursor.getString(2));
            bundle.putInt("email", cursor.getInt(3));
            bundle.putString("bmp", cursor.getString(5));
            contactItemDetailsV2.setArguments(bundle);

            mMainActivity.showFragment(contactItemDetailsV2, true, false);


        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.mirrorBtn) {
            if (isSelectMode) onBackButtonPressed();
            isMirror = true;
            setTabBaground();
            setmListView();

        } else if (v.getId() == R.id.mirrorPlusBtn) {
            if (isSelectMode) onBackButtonPressed();
            isMirror = false;
            setTabBaground();
            setmListView();
        } else {
            onCancelService();
        }

    }

    private void onCancelService() {

    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setAppTitle();
        setEventListner(mMainActivity.getCablePresenter());
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
        ShareContact();
    }

    @Override
    public void onRestoreIconListener() {
        mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }


        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onRestoreSmartData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                mInitProgress.dismiss();
                DetailsFragment.enableDisableView(mParentView, true);
                mMainActivity.enableToolbarClick(true);
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

    public void ShareContact() {
        String shareBody = "";
        String line = " ";
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");

        for (SmartDataInfo smartDataInfo : mMap) {

            cursor.moveToPosition(smartDataInfo.position);
            shareBody = shareBody + "\n" + line;

            shareBody = shareBody + " \nName: " + cursor.getString(1) + "\n" + "Contact No: " + cursor.getString(2) + "\n" + "Email: " + cursor.getString(3);

            shareBody = shareBody + "\n" + line;

        }

        mInitProgress.dismiss();

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_contacts)));

    }

    private void setList() {

        adapter = new ContactsCursorAdapter(getContext(), cursor);
        mList.setAdapter(adapter);
        setAppTitle();
        mInitProgress.dismiss();
    }

    public void setAppTitle() {

        if (cursor == null || cursor.getCount() == 0) {
            mMainActivity.setAppTitle(getResources().getString(R.string.contacts));
        } else {
            mMainActivity.setAppTitle(getResources().getString(R.string.contacts) + " (" + cursor.getCount() + ")");

        }

    }


    private void dbgTrace(String trace) {
        GenUtils.logMessageToFile("CategoryDetailInfoContacts.log", trace);
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace() {
        String method = Thread.currentThread().getStackTrace()[3].getMethodName();
        dbgTrace(method);
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
