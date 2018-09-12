package com.meem.mmp.messages;

public class MMPGetThumbnailDb extends MMPCtrlMsg {
    public MMPGetThumbnailDb(MMPFPath fpath, int dbVersion) {
        super(MMPConstants.MMP_CODE_GET_THUMBNAIL_DB);
        addParam(fpath.asArray());
        addParam(dbVersion);
    }
}
