package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.businesslogic.GenericDataThumbnailDatabase;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTabSelection;
import com.meem.ui.utils.MeemTextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by SCS on 8/4/2016.
 * * List out the document items from the database using the path available from the bundle ,
 */
public class CategoryDetailInfoDocuments extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, MainActivity.ToolBarItemClickListener {


    final String TAG = "CategoryDetailInfoDocuments";
    View mRootView;
    ListView mList;
    MainActivity mMainActivity;

    MeemTextView mMirrorTV, mMirrorPlusTV;
    boolean isMirror = true;
    SQLiteDatabase db;
    MeemTabSelection mMeemTabSelection;
    boolean isGreenMeem, isSelectMode;
    DocumentsCursorAdapter adapter;
    String mTableNameSuffix, mTableNamePrefix;
    RestoreOrShareGenDataInterface mListener;
    String mVaultId;
    RelativeLayout mParentView;
    Byte catCode = MMPConstants.MMP_CATCODE_DOCUMENTS;
    String TABLE_NAME;


    boolean isFromDelete;
    DbOperationAsync dbOperationAsync;
    private Cursor cursor;
    private String mDBPath;
    private ProgressDialog mInitProgress;

    private boolean isTableExists=false;

    public CategoryDetailInfoDocuments() {

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
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.categorydetailsdocumentslist, null);
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
        dbOperationAsync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void closeDatabase() {
        if (db != null && db.isOpen()) db.close();
    }

    private void openDatabase() {
        if (db == null)
            db = SQLiteDatabase.openDatabase(mDBPath, null, SQLiteDatabase.OPEN_READWRITE);
        if (db != null) {
            if (!db.isOpen()) {
                db = SQLiteDatabase.openDatabase(mDBPath, null, SQLiteDatabase.OPEN_READWRITE);
            }
        }
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

    private void usingCursorAdapter() {
        openDatabase();
        TABLE_NAME = "";

        String query;
        int backupMode;

        if (isMirror) {
            mTableNameSuffix = "_doc_int_mirror";
            backupMode = 0;
        } else {
            mTableNameSuffix = "_doc_int_plus";
            backupMode = 1;
        }

        mTableNamePrefix = "_" + mVaultId;

        if (mMainActivity.getCableVersion() == 1) {
            TABLE_NAME = mTableNamePrefix + mTableNameSuffix;

            //            BugId:6 , Checking table before fetching data from table
            query="select name from sqlite_master where type='table' and name='"+TABLE_NAME+"'";
            cursor=db.rawQuery(query,null);
            if(cursor.getCount()>0)
                isTableExists = true;
            else isTableExists=false;


            query = "SELECT srcCsum || srcFilePath as _id,srcFilePath,thumbImage,srcCsum,sdcard,filesize from " + TABLE_NAME + " where fwAck=1 ";
        } else {
            isTableExists=true;

            TABLE_NAME = mTableNamePrefix + "_doc_int_genericdata";

            String TABLE_NAME2 = mTableNamePrefix + "_doc_int_genericdata_t";
            String cam_table = mTableNamePrefix + "_doc_ext_genericdata";
            String cam_table_t = mTableNamePrefix + "_doc_ext_genericdata_t";


            query = "SELECT csum || phone_path as _id" +
                    ",phone_path,thumb_image,csum,sdcard,size,meem_path from " + TABLE_NAME + " where  " +
                    "backup_mode=" + backupMode + " and  meem_path in (select  meem_path from " + TABLE_NAME2 + " where fw_ack=1 ) " +
                    "union all " +
                    "SELECT csum || phone_path as _id ,phone_path,thumb_image,csum,sdcard,size,meem_path from " + cam_table + " where  " +
                    "backup_mode=" + backupMode + " and  meem_path in (select  meem_path from " + cam_table_t + " where fw_ack=1 )  ";
        }

        if(isTableExists)
        cursor = db.rawQuery(query, null);


    }

    private void setClickListner() {
        mList.setOnItemClickListener(this);
        mMirrorTV.setOnClickListener(this);
        mMirrorPlusTV.setOnClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isSelectMode) return;
        getClickedItem(position);

    }

    private void getClickedItem(int position) {

        ArrayList<GenDataInfo> genDataInfoArrayList = new ArrayList<>();
        cursor.moveToPosition(position);
        GenDataInfo genDataInfo = new GenDataInfo();
        genDataInfo.fPath = cursor.getString(1);
        genDataInfo.cSum = cursor.getString(3);
        genDataInfo.isSdcard = ((cursor.getInt(4) == 0) ? false : true);
//        genDataInfo.isSdcard = Boolean.parseBoolean(cursor.getString(4));
        if (cursor.getColumnCount() > 6)
            genDataInfo.meemPath = cursor.getString(6);
        genDataInfoArrayList.add(genDataInfo);

        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");

        mListener.onShareGenData(mVaultId, catCode, genDataInfoArrayList, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mInitProgress.dismiss();

                if (result) {
                    ArrayList<GenDataInfo> genDataInfoArrayList1 = new ArrayList<GenDataInfo>();
                    genDataInfoArrayList1 = (ArrayList<GenDataInfo>) info;
                    DocumentsItemDetails documentsItemDetails = new DocumentsItemDetails();
                    Bundle bundle = new Bundle();
                    bundle.putString(DetailsFragment.vaultId, mVaultId);
                    bundle.putBoolean("isMirror", isMirror);
                    bundle.putParcelable("GENOBJECT", genDataInfoArrayList1.get(0));
                    documentsItemDetails.setArguments(bundle);
                    mMainActivity.showFragment(documentsItemDetails, true, false);
                } else {
                    mMainActivity.showToast(getString(R.string.failed));
                }

                return false;
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        openDatabase();
        setAppTitle();


        if (!isSelectMode) {
            mMainActivity.setBackPressDisable(false);
            mMainActivity.showSelectIconsHideShareRestore();
            mMainActivity.setOptionMenuContent(false, false, false, false);
        } else {
            mMainActivity.setBackPressDisable(true);
            mMainActivity.showShareandRestoreIconsHideSelect();
            mMainActivity.setOptionMenuContent(true, true, true, true);
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
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {
        ArrayList<GenDataInfo> mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");

        mListener.onShareGenData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mInitProgress.dismiss();
                if (result) {
                    Share(info);
                } else {

                }

                return false;
            }
        });
    }

    @Override
    public void onRestoreIconListener() {
        ArrayList<GenDataInfo> mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onRestoreGenData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
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

    public void Share(Object info) {
        ArrayList<GenDataInfo> genDataInfoArrayList = new ArrayList<>();
        genDataInfoArrayList = (ArrayList<GenDataInfo>) info;


        // Share Intent
        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);

        // Type of file to share
        share.setType("image/jpeg");

        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (GenDataInfo genDataInfo : genDataInfoArrayList) {
            File fileIn = new File(genDataInfo.destFPath);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        // Show the social share chooser list
        startActivity(Intent.createChooser(share, getResources().getString(R.string.share_photos)));

    }

    @Override
    public void onSelectIconClickListener() {
        isSelectMode = true;
        mMainActivity.showShareandRestoreIconsHideSelect();
        mMainActivity.setOptionMenuContent(true, true, true, true);
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
        if (isFromDelete) {
            isFromDelete = false;
            setmListView();
        }
    }

    @Override
    public void onDeleteIconClickListener() {

        final ArrayList<GenDataInfo> mMap = adapter.getSelectedItems();
        if (mMap.size() <= 0) {
            Toast.makeText(getActivity(), R.string.please_select_any_items, Toast.LENGTH_SHORT).show();
            return;
        }
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onDeleteGenData(mVaultId, catCode, mMap, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {

                if (result) {
                    String cksm, path;
                    ArrayList<GenDataInfo> genDataInfoArrayList = new ArrayList<GenDataInfo>();
                    genDataInfoArrayList = (ArrayList<GenDataInfo>) info;
                    openDatabase();

                    for (GenDataInfo genDataInfo : genDataInfoArrayList) {
                        cksm = genDataInfo.cSum;
                        path = genDataInfo.fPath;

                        if (true) {
                            db.delete(TABLE_NAME, "srcCsum=? and srcFilePath=? and fwAck=?", new String[]{cksm, path, "1"});
                        } else {
                            db.delete(TABLE_NAME, "srcCsum=? and srcFilePath=? ", new String[]{cksm, path});

                        }
                        GenericDataThumbnailDatabase genDBClass = new GenericDataThumbnailDatabase(mVaultId);
                        genDBClass.setThumbNailDbVersion(genDBClass.getThumbnailDbVersion() + 1);
                    }

                    mInitProgress.dismiss();
                    isFromDelete = true;
                    onBackButtonPressed();
                } else {
                    Toast.makeText(getContext(), "Delete Failed", Toast.LENGTH_SHORT).show();
                }

                mInitProgress.dismiss();
                return false;
            }
        });

    }


    private void setList() {

        if(isTableExists) {
            adapter = new DocumentsCursorAdapter(getContext(), cursor);
            mList.setAdapter(adapter);
        }


        setAppTitle();
        mInitProgress.dismiss();


    }

    public void setAppTitle() {

        if (cursor == null || cursor.getCount() == 0) {
            mMainActivity.setAppTitle(getResources().getString(R.string.documents));
        } else {
            mMainActivity.setAppTitle(getResources().getString(R.string.documents) + " (" + cursor.getCount() + ")");

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
