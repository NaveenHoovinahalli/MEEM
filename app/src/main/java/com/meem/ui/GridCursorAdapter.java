package com.meem.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SCS on 8/26/2016.
 */
public class GridCursorAdapter extends CursorAdapter {
    ImageView imageView;
    CheckBox checkBox;
    HashMap<Integer, GenDataInfo> hashMap = new HashMap<>();
    Cursor mCursor;
    boolean isCheckBoxVisible;
    MainActivity mMainActivity;


    public GridCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mCursor = cursor;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.videodetaillistitem, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        imageView = (ImageView) view.findViewById(R.id.grid_image);
        checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        if (isCheckBoxVisible) checkBox.setVisibility(View.VISIBLE);
        else checkBox.setVisibility(View.GONE);

        if (hashMap.get(cursor.getPosition()) != null) {
            checkBox.setChecked(true);
        } else checkBox.setChecked(false);


        byte[] bob = cursor.getBlob(2);
        // Arun: 02Dec2016: added null check.
        if (bob == null) {
            return;
        }

        imageView.setImageBitmap(BitmapFactory.decodeByteArray(bob, 0, bob.length));
        checkBox.setTag(cursor.getPosition());

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
                    genDataInfo.isSdcard = (mCursor.getInt(4) == 0) ? false : true;
                    if (mCursor.getColumnCount() > 6)
                        genDataInfo.meemPath = cursor.getString(6);
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
