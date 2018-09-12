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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Naveen on 8/26/2016.
 * Using cursorAdapter to fetch the data from the database ,
 */
public class CalenderCursorAdapter extends CursorAdapter {
    CheckBox checkBox;
    /*hashMap is used to hold the selected item SmartdataInfo*/
    HashMap<Integer, SmartDataInfo> hashMap = new HashMap<>();
    Cursor mCursor;
    boolean isCheckBoxVisible;
    MeemTextView mEventName, mStartDate, mEnddate;
    private String startDate;
    private String endDate;


    public CalenderCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mCursor = cursor;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.calenderdetaillistitem, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        mEventName = (MeemTextView) view.findViewById(R.id.eventName);
        mStartDate = (MeemTextView) view.findViewById(R.id.startDate);
        mEnddate = (MeemTextView) view.findViewById(R.id.endDate);

        if (isCheckBoxVisible) checkBox.setVisibility(View.VISIBLE);
        else checkBox.setVisibility(View.GONE);

        if (hashMap.get(cursor.getPosition()) != null) {
            checkBox.setChecked(true);
        } else checkBox.setChecked(false);

        checkBox.setTag(cursor.getPosition());

        mEventName.setText(cursor.getString(6));

        startDate = cursor.getString(4);
        if (!(startDate == null) && !startDate.equals("")) startDate = convertMiliSectoDateFormat(Long.parseLong(startDate));
        endDate = cursor.getString(5);
        if (!(endDate == null) && !endDate.equals("")) endDate = convertMiliSectoDateFormat(Long.parseLong(endDate));

        mStartDate.setText(startDate);
        mEnddate.setText(endDate);


        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    if (hashMap.size() >= 246) {
                        Toast.makeText(context, "Please select less than 246 item", Toast.LENGTH_SHORT).show();
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

    public String convertMiliSectoDateFormat(long seconds) {

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy");
        Date date = new Date(seconds);
        return DATE_FORMAT.format(date);
    }
}
