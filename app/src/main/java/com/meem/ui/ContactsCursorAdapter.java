package com.meem.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Toast;

import com.meem.androidapp.R;
import com.meem.ui.utils.MeemTextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SCS on 8/26/2016.
 */
public class ContactsCursorAdapter extends CursorAdapter {
    CheckBox checkBox;
    HashMap<Integer, SmartDataInfo> hashMap = new HashMap<>();
    Cursor mCursor, mCursor2;
    boolean isCheckBoxVisible;
    MeemTextView mName, mNumber;


    public ContactsCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mCursor = cursor;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.contactdetaillistitem, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        mName = (MeemTextView) view.findViewById(R.id.name);
        mNumber = (MeemTextView) view.findViewById(R.id.number);
        mName.setText(cursor.getString(1));
        mNumber.setText(cursor.getString(2));

        if (isCheckBoxVisible) checkBox.setVisibility(View.VISIBLE);
        else checkBox.setVisibility(View.GONE);

        if (hashMap.get(cursor.getPosition()) != null) {
            checkBox.setChecked(true);
        } else checkBox.setChecked(false);

        checkBox.setTag(cursor.getPosition());


        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    if (hashMap.size() >= 246) {
                        Toast.makeText(context, "Select less than 246 items ", Toast.LENGTH_SHORT).show();
                        checkBox.setChecked(false);
                        return;
                    }
                    mCursor.moveToPosition((Integer) v.getTag());
                    SmartDataInfo smartDataInfo = new SmartDataInfo();
                    smartDataInfo.srcCsum = cursor.getInt(0);
                    smartDataInfo.position = (int) v.getTag();

                    hashMap.put((Integer) v.getTag(), smartDataInfo);
                } else hashMap.remove((Integer) v.getTag());

            }
        });

    }

    public void setCheckBoxVisible(boolean b) {
        if (b) isCheckBoxVisible = true;
        else isCheckBoxVisible = false;
    }


    public ArrayList<SmartDataInfo> getSelectedItems() {
        ArrayList<SmartDataInfo> smartDataInfoArrayList = new ArrayList<>();

        for (Integer key : hashMap.keySet()) {
            hashMap.get(key);
            smartDataInfoArrayList.add(hashMap.get(key));

        }
        return smartDataInfoArrayList;
    }
}
