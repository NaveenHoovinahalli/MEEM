package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.v2.cablemodel.ApplicationListModel;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Naveen on 7/2/2018.
 */

public class AppList extends Fragment implements MainActivity.ApplistBackupListener {

    View mRootView;
    RecyclerView mRecycleView;
    AppListAdapter appListAdapter;
    MainActivity mainActivity;
    ArrayList<ApplicationListModel> mMobileAppList=new ArrayList<>();
    ArrayList<ApplicationListModel> dbList=new ArrayList<>();

    private ProgressDialog mInitProgress;

    ApplicationListInterface mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mRootView=inflater.inflate(R.layout.applist,null);
        init();
        return mRootView;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mainActivity= (MainActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.shoeBackupTV(true);
        mainActivity.setBackupEventListner(this);

        setEventListner(mainActivity.getCablePresenter());

    }

    private void setEventListner(CablePresenter cablePresenter) {

        mListener=  cablePresenter;
    }

    private void init() {

        mRecycleView= (RecyclerView) mRootView.findViewById(R.id.applistrecycle);

        prepareData();

        appListAdapter=new AppListAdapter(dbList ,mainActivity);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mainActivity.getApplicationContext());
        mRecycleView.setLayoutManager(mLayoutManager);
        mRecycleView.setItemAnimator(new DefaultItemAnimator());
        mRecycleView.setAdapter(appListAdapter);

        mInitProgress = CustomDialogProgress.ctor(getContext());
    }

    private void prepareData() {
//        getDatafromDB();
        getDatafromPhone();

        for(ApplicationListModel model: dbList) {
             for(ApplicationListModel model2 : mMobileAppList){
                 if (model2.mPkg.equals(model.mPkg)) {
                     mMobileAppList.remove(model2);
                     break;
                 }
            }
        }

        dbList.addAll(mMobileAppList);

     }

//     TODO : get the db path
    private void getDatafromDB() {
        String DB_FULL_PATH ="/storage/emulated/0/naveen/abc.db";
     SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_FULL_PATH, null, SQLiteDatabase.OPEN_READONLY);
     String query="select * from appdata";
    Cursor cursor= db.rawQuery(query,null);


     if(cursor.getCount()>0){
         while ((cursor.moveToNext())){
             ApplicationListModel applicationListModel=new ApplicationListModel();
             applicationListModel.mName=cursor.getString(0);
             applicationListModel.mPkg=cursor.getString(1);
             applicationListModel.mImg=cursor.getString(2);
             dbList.add(applicationListModel);
         }
        cursor.close();
     }
        db.close();

    }

    @Override
    public void onStop() {
        super.onStop();
        mainActivity.shoeBackupTV(false);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mInitProgress.isShowing())
            mInitProgress.dismiss();

    }


    private static String byteArrToStr64(byte[] imageByteArray) {
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
    }

    public void getDatafromPhone() {
        mMobileAppList.clear();
        int flags = PackageManager.GET_META_DATA | PackageManager.GET_SHARED_LIBRARY_FILES | PackageManager.GET_UNINSTALLED_PACKAGES ;

        PackageManager pm=mainActivity.getPackageManager();
        List<ApplicationInfo> applicationInfo =pm.getInstalledApplications(flags);
        for(ApplicationInfo app : applicationInfo){
            if((app.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
                ApplicationListModel model = new ApplicationListModel();


                Drawable d =  app.loadIcon(mainActivity.getPackageManager());
                BitmapDrawable bitDw = ((BitmapDrawable) d);
                Bitmap bitmap = bitDw.getBitmap();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                String imgstr=byteArrToStr64(stream.toByteArray());

                model.mName=app.loadLabel(mainActivity.getPackageManager()).toString();
                model.mPkg=app.packageName.toString();
                model.mImg=imgstr;

                mMobileAppList.add(model);

            }
        }
    }



    @Override
    public void onBackupTVClickListner() {
        if(mMobileAppList.size() == 0)
            return;

        mainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onUpdateAppInfoToDb(mMobileAppList, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                //TODO : What to do with response ?
                return false;
            }
        });

        mInitProgress.dismiss();
        mainActivity.onBackPressed();

    }


   public interface ApplicationListInterface {
        void  onUpdateAppInfoToDb(ArrayList<ApplicationListModel> applicationListModels, ResponseCallback responseCallback);

    }
}
