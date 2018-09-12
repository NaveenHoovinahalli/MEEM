package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;
import com.meem.v2.core.MeemCoreV2Request;

/**
 * Created by arun on 20/4/17.
 */

public class MMPV2RecvFile extends MeemCoreV2Request {

    String upid;
    byte fType;
    byte fMode;
    byte catCode;
    String path;
    String meemPath;
    String cSum;

    boolean aborting;

    public MMPV2RecvFile(String upid, byte fType, byte fMode, byte catCode, String path, String meemPath, String cSum, ResponseCallback responseCallback) {
        super("MMPV2RecvFile", 1, responseCallback);

        this.upid = upid;
        this.fType = fType;
        this.fMode = fMode;
        this.catCode = catCode;
        this.path = path;
        this.meemPath = meemPath;
        this.cSum = cSum;

        aborting = false;
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        if (path == null || meemPath == null) {
            postXfrResult(null, false, false);
            return false;
        }

        return MeemCoreV2.getInstance().receiveFile(upid, fType, fMode, catCode, path, meemPath, cSum, null);
    }

    @Override
    protected void onXfrCompletion(String path) {
        super.onXfrCompletion(path);
        postXfrResult(path, false, true);
    }

    @Override
    protected void onXfrError(String path, int status) {
        super.onXfrError(path, status);
        postXfrResult(path, false, false);
    }

    @Override
    protected void onException(Throwable ex) {
        super.onException(ex);
        postXfrResult(null, false, false);
    }

    @Override
    protected boolean onAbortNotification() {
        super.onAbortNotification();
        aborting = true;
        return MeemCoreV2.getInstance().abortAllXfr(true);
    }

    @Override
    protected boolean onXfrRequest() {
        super.onXfrRequest();
        return !aborting;
    }

    @Override
    public String toString() {
        return mName + ": " + upid + ": " + fType + ": " + fMode + ": " + catCode + ": " + path + ": " + meemPath + ": " + cSum + ": " + aborting;
    }
}
