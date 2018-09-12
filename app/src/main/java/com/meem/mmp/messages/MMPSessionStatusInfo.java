package com.meem.mmp.messages;

public class MMPSessionStatusInfo {
    byte mHandle;

    byte mCatCode;
    Type mType = Type.INVALID;
    short mProgress;

    public MMPSessionStatusInfo() {
        mCatCode = 0;
        mType = Type.INVALID;
    }

    public MMPSessionStatusInfo(byte catCode, Type type) {
        mCatCode = catCode;
        mType = type;
    }

    public byte getHandle() {
        return mHandle;
    }

    public byte getCatCode() {
        return mCatCode;
    }

    public Type getType() {
        return mType;
    }

    public short getProgress() {
        return mProgress;
    }

    public enum Type {
        INVALID, STARTED, PROGRESS, ENDED
    }
}
