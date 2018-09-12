package com.meem.v2.cablemodel;

/**
 * Created by SCS on 3/16/2017.
 */

public class PhoneDbModel {
    public String mUpid;
    public String mName;
    public String mOpetr;
    public String mLang;
    public String mPltfrm;
    public String mVer;
    public String mBrand;
    public String mMod_name;
    public int mSize;
    public int mRsvd1;
    public String mRsvd2;

    @Override
    public boolean equals(Object other) {
        boolean eq = false;

        if (other != null && other instanceof PhoneDbModel) {
            eq = this.mUpid.equals(((PhoneDbModel) other).mUpid);
        }

        return eq;
    }
}
