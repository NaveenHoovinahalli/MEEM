package com.meem.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Toast;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.ui.utils.MeemTextView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SCS on 8/26/2016.
 */
public class DocumentsCursorAdapter extends CursorAdapter {
    CheckBox checkBox;
    HashMap<Integer, GenDataInfo> hashMap = new HashMap<>();
    Cursor mCursor;
    boolean isCheckBoxVisible;
    MeemTextView mTitle, mSize;
    File file;
    MainActivity mMainActivity;

    public DocumentsCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mCursor = cursor;
        mMainActivity = (MainActivity) context;
    }

    public static String convertBtoKB(long size) {
        if (size <= 0) return "--";
        double temp = ((double) size / 1024) / 1024;
        if (temp < .01) return "0.01";
        return new DecimalFormat("#0.00").format(temp);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.documentsdetaillistitem, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        mTitle = (MeemTextView) view.findViewById(R.id.title);
        mSize = (MeemTextView) view.findViewById(R.id.size);

        if (isCheckBoxVisible) checkBox.setVisibility(View.VISIBLE);
        else checkBox.setVisibility(View.GONE);

        if (hashMap.get(cursor.getPosition()) != null) {
            checkBox.setChecked(true);
        } else checkBox.setChecked(false);

        checkBox.setTag(cursor.getPosition());

        file = new File(cursor.getString(1));
        mTitle.setText(file.getName());

        mSize.setText(convertBtoKB(Long.parseLong(cursor.getString(5))) + "MB");


        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    if (hashMap.size() >= 254) {
                        Toast.makeText(context, "Hashmapsize <=254", Toast.LENGTH_SHORT).show();
                        checkBox.setChecked(false);
                        return;
                    }
                    mCursor.moveToPosition((Integer) v.getTag());
                    GenDataInfo genDataInfo = new GenDataInfo();
                    genDataInfo.fPath = mCursor.getString(1);
                    genDataInfo.cSum = mCursor.getString(3);
                    /*genDataInfo.isSdcard = Boolean.parseBoolean(mCursor.getString(4));*/ //Arun: 16June2017
                    genDataInfo.isSdcard = ((cursor.getInt(4) == 0) ? false : true);
                    if (mCursor.getColumnCount() > 6)
                        genDataInfo.meemPath = mCursor.getString(6);

                    hashMap.put((Integer) v.getTag(), genDataInfo);
                } else hashMap.remove((Integer) v.getTag());

            }
        });

    }

    public void setCheckBoxVisible(boolean b) {
        if (b) isCheckBoxVisible = true;
        else isCheckBoxVisible = false;
    }

    public ArrayList<GenDataInfo> getSelectedItems() {
        ArrayList<GenDataInfo> genDataInfoArrayList = new ArrayList<>();

        for (Integer key : hashMap.keySet()) {
            hashMap.get(key);
            genDataInfoArrayList.add(hashMap.get(key));

        }
        return genDataInfoArrayList;
    }
}
