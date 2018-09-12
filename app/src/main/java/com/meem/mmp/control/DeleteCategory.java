package com.meem.mmp.control;

import com.meem.androidapp.UiContext;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPCtrlMsg;
import com.meem.mmp.messages.MMPDeleteCategory;
import com.meem.mmp.messages.MMPDeleteCategorySpec;

import java.util.ArrayList;

public class DeleteCategory extends MMPHandler {

    MMPDeleteCategorySpec mSpec;

    public DeleteCategory(MMPDeleteCategorySpec delSpec, ResponseCallback responseCallback) {
        super("DeleteCategory", MMPConstants.MMP_CODE_DELETE_CATEGORY, responseCallback);
        mSpec = delSpec;
    }

    @Override
    protected boolean kickStart() {
        MMPDeleteCategory delCatCmd = new MMPDeleteCategory(mSpec.getUpid(), mSpec.getCatCode(), mSpec.isMirror());
        return delCatCmd.send();
    }

    @Override
    protected boolean onMMPMessage(MMPCtrlMsg msg) {
        int msgCode = msg.getMessageCode();
        if (msgCode != MMPConstants.MMP_CODE_DELETE_CATEGORY) {
            // ignore and continue.
            mUiCtxt.log(UiContext.ERROR, "DeleteCategory object got unknown message");
            msg.dbgDumpBuffer();
            return true;
        }

        if (msg.isAck()) {
            // wait further
            return true;
        } else if (msg.isError()) {
            postResult(false, msg.getErrorCode(), mSpec, null);
            return false;
        } else if (msg.isSuccess()) {
            // 10Dec2015: Arun: To support SDCARD item deletion which are mapped to cats that appears in GUI.
            ArrayList<Byte> mappedCats = mSpec.getMappedCatCodes();
            if (mappedCats.isEmpty()) {
                // All finished
                postResult(true, 0, mSpec, null);
                return false;
            } else {
                mUiCtxt.log(UiContext.DEBUG, "DeleteCategory: deleting primary category succeeded: " + mSpec.getCatCode());

                // Delete mapped cats, one by one. Note that only one mapped category is there at present (20Dec2015).
                // Also, note that we are modifying the real array list. Must take care of this in GUI code when we
                // post the mSpec back in event.
                byte mappedCat = mappedCats.remove(0);
                mUiCtxt.log(UiContext.DEBUG, "DeleteCategory: Now deleting mapped category: " + mappedCat);

                if (!deleteMappedCat(mappedCat)) {
                    postResult(false, msg.getErrorCode(), mSpec, null);
                    return false;
                }

                // wait for its response.
                return true;
            }
        }

        // MMP bug!
        postResult(false, MMPConstants.MMP_ERROR_BUGCHECK, mSpec, null);
        return false;
    }

    private boolean deleteMappedCat(byte cat) {
        MMPDeleteCategory delCatCmd = new MMPDeleteCategory(mSpec.getUpid(), cat, mSpec.isMirror());
        return delCatCmd.send();
    }

    @Override
    protected boolean onMMPTimeout() {
        mUiCtxt.log(UiContext.DEBUG, "DeleteCategory TIMEOUT");
        postResult(false, MMPConstants.MMP_ERROR_TIMEOUT, mSpec, null);
        return false;
    }
}
