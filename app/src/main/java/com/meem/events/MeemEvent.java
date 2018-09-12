package com.meem.events;

/**
 * Class that is used exclusively for sending events across modules working in different thread contexts. Mainly used by backend modules to
 * send events to UI thread. Created by arun on 22/2/16.
 * <p/>
 * See com.meem.mmp.control.MMPHandler for the contract on usage between UI classes and backend.
 */
public class MeemEvent {
    EventCode mCode;

    int arg0;
    int arg1;
    Object mInfo;
    Object mExtraInfo;
    boolean mResult;
    ResponseCallback mResponseCallback;

    public MeemEvent() {
        mCode = EventCode.INVALID;
    }

    public MeemEvent(EventCode code) {
        mCode = code;
        mInfo = null;
        mResponseCallback = null;
    }

    public MeemEvent(EventCode code, Object info) {
        mCode = code;
        mInfo = info;
        mResponseCallback = null;
    }

    public MeemEvent(EventCode code, Object info, Object extraInfo) {
        mCode = code;
        mInfo = info;
        mExtraInfo = extraInfo;
        mResponseCallback = null;
    }

    public MeemEvent(EventCode code, Object info, ResponseCallback responseCallback) {
        mCode = code;
        mInfo = info;
        mResponseCallback = responseCallback;
    }

    public MeemEvent(EventCode code, Object info, Object extraInfo, ResponseCallback responseCallback) {
        mCode = code;
        mInfo = info;
        mExtraInfo = extraInfo;
        mResponseCallback = responseCallback;
    }

    public EventCode getCode() {
        return mCode;
    }

    public void setCode(EventCode code) {
        mCode = code;
    }

    public int getArg0() {
        return arg0;
    }

    public void setArg0(int arg0) {
        this.arg0 = arg0;
    }

    public int getArg1() {
        return arg1;
    }

    public void setArg1(int arg1) {
        this.arg1 = arg1;
    }

    public boolean getResult() {
        return mResult;
    }

    public void setResult(boolean result) {
        mResult = result;
    }

    public Object getInfo() {
        return mInfo;
    }

    public void setInfo(Object info) {
        mInfo = info;
    }

    public ResponseCallback getResponseCallback() {
        return mResponseCallback;
    }

    public void setResponseCallback(ResponseCallback responseCallback) {
        mResponseCallback = responseCallback;
    }

    public Object getExtraInfo() {
        return mExtraInfo;
    }

    public void setExtraInfo(Object extraInfo) {
        mExtraInfo = extraInfo;
    }
}
