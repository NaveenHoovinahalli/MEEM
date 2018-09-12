package com.meem.ui;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by SCS on 11/22/2016.
 */

public class GenDataInfo implements Parcelable {

    public static final Creator<GenDataInfo> CREATOR = new Creator<GenDataInfo>() {
        @Override
        public GenDataInfo createFromParcel(Parcel in) {
            return new GenDataInfo(in);
        }

        @Override
        public GenDataInfo[] newArray(int size) {
            return new GenDataInfo[size];
        }
    };
    String cSum;
    String fPath;
    String fSize;
    boolean isSdcard;
    String destFPath;

    // === for V2 ===
    String meemPath;

    protected GenDataInfo(Parcel in) {
        cSum = in.readString();
        fPath = in.readString();
        fSize = in.readString();
        isSdcard = in.readByte() != 0;
        destFPath = in.readString();
        meemPath = in.readString();
    }

    public GenDataInfo() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cSum);
        dest.writeString(fPath);
        dest.writeString(fSize);
        dest.writeByte((byte) (isSdcard ? 1 : 0));
        dest.writeString(destFPath);
        dest.writeString(meemPath);
    }

    public String getcSum() {
        return cSum;
    }

    public String getfPath() {
        return fPath;
    }

    public String getfSize() {
        return fSize;
    }

    public boolean isSdcard() {
        return isSdcard;
    }

    public String getDestFPath() {
        return destFPath;
    }

    public void setDestFPath(String path) {
        destFPath = path;
    }

    public String getMeemPath() {
        return meemPath;
    }

    public void setMeemPath(String meemPath) {
        this.meemPath = meemPath;
    }
}
