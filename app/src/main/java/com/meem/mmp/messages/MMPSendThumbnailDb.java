package com.meem.mmp.messages;

public class MMPSendThumbnailDb extends MMPCtrlMsg {
    public MMPSendThumbnailDb(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SEND_THUMBNAIL_DB);
        addParam(fpath.asArray());

        // MMP change on 21Aug2014
        String csum = fpath.getChecksum();
        if (null != csum) {
            addParam(csum.getBytes());
        }
    }
}
