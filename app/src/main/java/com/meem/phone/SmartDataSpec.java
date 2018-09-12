package com.meem.phone;

/**
 * Created by arun on 17/11/16.
 */

import com.meem.mmp.mml.MMLCategory;

/**
 * Class representing the smart data to be processed (to generate thumbnail db for it).
 */
public class SmartDataSpec {
    public String mUpid;
    public byte mCatCode;
    public String mMirrorPath;
    public String mPlusPath;

    public SmartDataSpec(String upid, byte catCode, String mirrorPath, String plusPath) {
        mUpid = upid;
        mCatCode = catCode;
        mMirrorPath = mirrorPath;
        mPlusPath = plusPath;
    }

    public String toString() {
        return "Upid: " + mUpid + ": " + MMLCategory.toSmartCatString(mCatCode) +
                ((mMirrorPath == null) ? " <No Mirror> " : " <Mirror> ") +
                ((mPlusPath == null) ? "<No Plus>" : "<Plus> ");
    }
}
