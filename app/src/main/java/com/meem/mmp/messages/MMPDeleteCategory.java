package com.meem.mmp.messages;

public class MMPDeleteCategory extends MMPCtrlMsg {

    MMPUpid mUpid;
    byte mCatCode;

    public MMPDeleteCategory(MMPUpid upid, byte catCode, boolean isMirror) {
        super(MMPConstants.MMP_CODE_DELETE_CATEGORY);
        mUpid = upid;
        mCatCode = catCode;

        addParam(mUpid.asArray());
        addParam(mCatCode);
        if (isMirror) {
            addParam((byte) 'M');
        } else {
            addParam((byte) 'P');
        }
    }

    // For parsing response message. Not needed really as
    // We are keeping the details in com.meem.mmp.control.DeleteCategory object
    // anyway.
    public MMPDeleteCategory(MMPCtrlMsg copy) {
        super(copy);
    }
}
