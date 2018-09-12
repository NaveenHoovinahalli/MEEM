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
public class HelpListAdapter extends BaseAdapter {

    String[] mHelpNames;
    Context context;
    MeemTextView mTextView;

    public HelpListAdapter(Context context, String[] mHelpNames) {

        this.context = context;
        this.mHelpNames = mHelpNames;

    }

    @Override
    public int getCount() {
        return mHelpNames.length;
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

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.helplistitem, null);
        mTextView = (MeemTextView) convertView.findViewById(R.id.textViewHelp);
        mTextView.setText(mHelpNames[position]);


        return convertView;
    }
}
