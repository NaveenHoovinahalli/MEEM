package com.meem.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.meem.androidapp.R;
import com.meem.ui.utils.MeemTextView;


/**
 * Created by SCS on 7/11/2016.
 */
public class HelpDetailsListAdapter extends BaseAdapter {
    String[] mHeader;
    String[] mDescription;
    Context context;
    LayoutInflater inflater;

    public HelpDetailsListAdapter(Context context, String[] mHeader, String[] mDescription) {

        this.context = context;
        this.mHeader = mHeader;
        this.mDescription = mDescription;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return 7;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.helpdescriptionlistitem, null);
            viewHolder = new ViewHolder();
            viewHolder.headerName = (MeemTextView) convertView.findViewById(R.id.itemHeader);
            viewHolder.descriptionName = (MeemTextView) convertView.findViewById(R.id.itemDescription);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.headerName.setText(mHeader[position]);
        viewHolder.descriptionName.setText(mDescription[position]);
        return convertView;

    }

    static class ViewHolder {
        private MeemTextView headerName;
        private MeemTextView descriptionName;
    }
}
