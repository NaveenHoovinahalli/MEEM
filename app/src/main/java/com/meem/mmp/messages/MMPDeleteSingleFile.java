package com.meem.mmp.messages;

/**
 * Created by arun on 21/11/16.
 */

public class MMPDeleteSingleFile extends MMPCtrlMsg {
    public MMPDeleteSingleFile(MMPSingleFileSpec spec) {
        super(MMPConstants.MMP_CODE_DELETE_SINGLE_FILE);
        addParam(spec.mUpid.asArray());
        addParam(spec.mFPath.asArray());
        addParam(spec.mCatCode);

        if (spec.mIsMirror) {
            addParam((byte) 'M');
        } else {
            addParam((byte) 'P');
        }
    }
}