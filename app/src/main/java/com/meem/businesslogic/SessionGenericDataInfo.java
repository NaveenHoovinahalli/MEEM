package com.meem.businesslogic;

import com.meem.mmp.mml.MMLCategory;

import java.util.ArrayList;

public class SessionGenericDataInfo {
    int mGenCatMask, mGenPlusCatMask;

    public SessionGenericDataInfo(int genCatMask, int genPlusCatMask) {
        mGenCatMask = genCatMask;
        mGenPlusCatMask = genPlusCatMask;
    }

    public int getGenCatMask() {
        return mGenCatMask;
    }

    public int getGenPlusCatMask() {
        return mGenPlusCatMask;
    }

    public ArrayList<Byte> getCatCodes() {
        return MMLCategory.getGenericCatCodesArray(mGenCatMask);
    }
}
