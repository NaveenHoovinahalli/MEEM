package com.meem.mmp.messages;

public class MMPSingleFileSpec {
    MMPUpid mUpid;
    MMPFPath mFPath;
    MMPFPath mDestFPath;
    byte mCatCode;
    boolean mIsMirror;

    // === for V2 ===
    String mMeemPath;

    public MMPSingleFileSpec(MMPUpid upid, byte catCode, boolean isMirror, MMPFPath fpath, MMPFPath destFPath) {
        mUpid = upid;
        mFPath = fpath;
        mCatCode = catCode;
        mIsMirror = isMirror;
        mDestFPath = destFPath;
    }

    public MMPUpid getUpid() {
        return mUpid;
    }

    public MMPFPath getFPath() {
        return mFPath;
    }

    public byte getCatCode() {
        return mCatCode;
    }

    public boolean isMirror() {
        return mIsMirror;
    }

    public MMPFPath getDestFPath() {
        return mDestFPath;
    }

    public String getMeemPath() {
        return mMeemPath;
    }

    public void setMeemPath(String meemPath) {
        mMeemPath = meemPath;
    }

    public String toString() {
        return "" + "Cat: " + mCatCode + ": " + mFPath + ", " + mDestFPath;
    }
}
