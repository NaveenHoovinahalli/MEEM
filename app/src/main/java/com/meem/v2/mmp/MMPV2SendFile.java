package com.meem.v2.mmp;

import com.meem.events.ResponseCallback;
import com.meem.v2.core.MeemCoreV2;
import com.meem.v2.core.MeemCoreV2Request;

/**
 * Created by arun on 20/4/17.
 */

public class MMPV2SendFile extends MeemCoreV2Request {
    String upid;
    byte fType;
    byte fMode;
    byte catCode;
    String path;
    String meemPath;
    String cSum;

    long rowId;

    boolean aborting;

    public MMPV2SendFile(String upid, byte fType, byte fMode, byte catCode, String path, String meemPath, String cSum, long rowId, ResponseCallback responseCallback) {
        super("MMPV2SendFile", 0, responseCallback);

        this.upid = upid;
        this.fType = fType;
        this.fMode = fMode;
        this.catCode = catCode;
        this.path = path;
        this.meemPath = meemPath;
        this.cSum = cSum;
        this.rowId = rowId;

        aborting = false;
    }

    @Override
    protected boolean kickStart() {
        if(!super.kickStart()) {
            return false;
        }

        if (path == null || meemPath == null) {
            postXfrResult(null, false, true);
            return false;
        }

        return MeemCoreV2.getInstance().sendFile(upid, fType, fMode, catCode, path, meemPath, cSum, rowId, null);
    }

    @Override
    protected void onXfrCompletion(String path) {
        super.onXfrCompletion(path);
        postXfrResult(path, true, true);
    }

    @Override
    protected void onXfrError(String path, int status) {
        super.onXfrError(path, status);
        postXfrResult(path, true, false);
    }

    @Override
    protected void onException(Throwable ex) {
        super.onException(ex);
        postXfrResult(null, true, false);
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
        return mName + ": " + upid + ": " + fType + ": " + fMode + ": " + catCode + ": " + path + ": " + meemPath + ": " + cSum + ": " + rowId + ": " + ": " + aborting;
    }
}
