package com.meem.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.meem.androidapp.R;
import com.meem.fwupdate.UpdateInfo;
import com.meem.ui.utils.MeemTextView;

import java.util.ArrayList;


/**
 * Created by SCS on 7/13/2016.
 */
public class FirmwareListAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    ArrayList<UpdateInfo> updateInfoArrayList;
    MeemTextView mTitle, mDesc;

    public FirmwareListAdapter(Context context, ArrayList<UpdateInfo> updateInfoArrayList) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.updateInfoArrayList = updateInfoArrayList;


    }

    @Override
    public int getCount() {
        return updateInfoArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = inflater.inflate(R.layout.firmwarelistitem, null);
        mTitle = (MeemTextView) convertView.findViewById(R.id.title);
        mDesc = (MeemTextView) convertView.findViewById(R.id.desc);
        mTitle.setText(updateInfoArrayList.get(position).mFwDescText);
        mDesc.setText(updateInfoArrayList.get(position).mFwNewVersion);

        return convertView;
    }


}
