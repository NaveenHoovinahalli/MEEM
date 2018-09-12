package com.meem.mmp.messages;

/**
 * Created by arun on 17/11/16.
 */

public class MMPSingleSmartDataSpec {
    MMPUpid mUpid;
    byte mCatCode;

    MMPFPath mMirrorFPath;
    MMPFPath mPlusFPath;

    public MMPSingleSmartDataSpec(MMPUpid upid, byte catCode, MMPFPath mirfpath, MMPFPath plusFPath) {
        mUpid = upid;
        mMirrorFPath = mirfpath;
        mCatCode = catCode;
        mPlusFPath = plusFPath;
    }

    public MMPUpid getUpid() {
        return mUpid;
    }

    public MMPFPath getMirrorFPath() {
        return mMirrorFPath;
    }

    public byte getCatCode() {
        return mCatCode;
    }

    public MMPFPath getPlusFPath() {
        return mPlusFPath;
    }
}
