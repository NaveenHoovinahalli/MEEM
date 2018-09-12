package com.meem.mmp.messages;

public class MMPGetSingleFile extends MMPCtrlMsg {
    public MMPGetSingleFile(MMPSingleFileSpec spec) {
        super(MMPConstants.MMP_CODE_GET_SINGLE_FILE);
        addParam(spec.mUpid.asArray());
        addParam(spec.mFPath.asArray());
        addParam(spec.mDestFPath.asArray());
        addParam(spec.mCatCode);

        if (spec.mIsMirror) {
            addParam((byte) 'M');
        } else {
            addParam((byte) 'P');
        }
    }
}
