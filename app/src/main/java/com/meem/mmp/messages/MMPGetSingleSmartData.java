package com.meem.mmp.messages;

/**
 * Created by arun on 26/8/16.
 */
public class MMPGetSingleSmartData extends MMPCtrlMsg {
    public MMPGetSingleSmartData(MMPSingleSmartDataSpec spec) {
        super(MMPConstants.MMP_CODE_GET_SINGLE_SMART_DATA);
        addParam(spec.mUpid.asArray());
        addParam(spec.mCatCode);
        addParam(spec.mMirrorFPath.asArray());
        addParam(spec.mPlusFPath.asArray());
    }
}
