package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.messages.MMPUpid;
import com.meem.mmp.messages.MPPGetVSTAT;

import java.util.ArrayList;

/**
 * @author Arun T A
 */

public class GetAllVaultStatus extends MMPHandler {
    ArrayList<MMPFPath> mFileList;
    ArrayList<MMPUpid> mUpids;
    int mUpidIndex;
    int mUpidCount;

    public GetAllVaultStatus(ArrayList<MMPUpid> upids, ArrayList<MMPFPath> fileList, ResponseCallback onCompletion) {
        super("GetAllVaultStatus", MMPConstants.MMP_CODE_APP_GET_ALL_VAULT_STATUS, onCompletion);

        if (0 == upids.size()) {
            throw new IllegalArgumentException("Upid list is empty which does not make sense");
        }

        if (upids.size() != fileList.size()) {
            throw new IllegalArgumentException("There should be as many files as there are Upids");
        }

        mFileList = fileList;
        mUpids = upids;
        mUpidIndex = 0;
        mUpidCount = upids.size();
    }

    private boolean sendCommand() {
        MMPUpid upid = mUpids.get(mUpidIndex);
        MMPFPath fpath = mFileList.get(mUpidIndex);

        MPPGetVSTAT vstat = new MPPGetVSTAT(upid, fpath);
        return vstat.send();
    }

    @Override
    protected boolean kickStart() {
        return sendCommand();
    }

    @Override
    public boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_GET_VSTAT) {
            mUiCtxt.log(UiContext.ERROR, "GetAllVaultStatus object got an unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            return true;
        }

        if (msg.isError()) {
            postResult(false, msg.getErrorCode(), null, null);
            return false;
        }

        mUpidIndex++;

        if (mUpidIndex == mUpidCount) {
            // All done. Post success event.
            postResult(true, 0, null, null);
            return false;
        } else {
            return sendCommand();
        }
    }

    @Override
    public boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "GetAllVaultStatus TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, null, null);
        return false;
    }
}
