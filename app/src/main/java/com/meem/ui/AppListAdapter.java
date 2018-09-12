package com.meem.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.ui.utils.MeemTextView;
import com.meem.v2.cablemodel.ApplicationListModel;

import java.util.List;

/**
 * Created by Naveen on 7/2/2018.
 */

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.MyViewHolder> {


   List<ApplicationListModel> mAppInfo;
    MainActivity mMainActivity;
    byte[] bob;



    public class MyViewHolder extends RecyclerView.ViewHolder {
        public MeemTextView mAppName;
        ImageView mAppIcon;
        Button mStatus;
        public MyViewHolder(View itemView) {
            super(itemView);
            mAppName= (MeemTextView) itemView.findViewById(R.id.appname);
            mAppIcon= (ImageView) itemView.findViewById(R.id.appicon);
            mStatus= (Button) itemView.findViewById(R.id.button);
        }
    }

        public AppListAdapter(List<ApplicationListModel> list, MainActivity mainActivity){
        mMainActivity=mainActivity;
        mAppInfo=list;
    }


    @Override
    public AppListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView= LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item,parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AppListAdapter.MyViewHolder holder, final int position) {

            final String pkg=mAppInfo.get(position).mPkg;
        holder.mAppName.setText(mAppInfo.get(position).mName);
            String imgstr=mAppInfo.get(position).mImg;
        if (imgstr != null && !imgstr.equals("")){
            bob = str64TobyteArr(imgstr);


        holder.mAppIcon.setImageBitmap(BitmapFactory.decodeByteArray(bob,0,bob.length));
        }

        if(isAppInstalled(pkg))
            holder.mStatus.setText("OPEN");
        else holder.mStatus.setText("INSTALL");

        holder.mStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isAppInstalled(pkg)){
                    Intent launch=mMainActivity.getPackageManager().getLaunchIntentForPackage(pkg);
                    mMainActivity.startActivity(launch);
                }else{
                    goToPlayStore( pkg);

                }
            }
        });

    }

    private void goToPlayStore(String pkg) {

            try {
                mMainActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
            } catch (android.content.ActivityNotFoundException anfe) {
                mMainActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }


    }

    @Override
    public int getItemCount() {
        return mAppInfo.size();
    }


    private static byte[] str64TobyteArr(String imageDataString) {
        return Base64.decode(imageDataString, Base64.DEFAULT);
    }

        private boolean isAppInstalled(String pkg){
                PackageManager pm=mMainActivity.getPackageManager();
                try {
                    pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                    return true;
                }catch (PackageManager.NameNotFoundException e){

                }
                return false;
        }


}
