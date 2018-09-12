package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

import java.io.File;

/**
 * @author Arun T A
 */

public class MMPFwUpdate extends MMPCtrlMsg {

    String mFwFilePath;
    File mFile;

    public MMPFwUpdate(String fwFilePath) {
        super(MMPConstants.MMP_CODE_UPDATE_FIRMWARE);

        mFwFilePath = fwFilePath;
        mFile = new File(mFwFilePath);

        int filePathLen = mFwFilePath.getBytes().length;
        String cSum = GenUtils.getFileMD5(mFwFilePath, null);

        addParam((int) mFile.length());
        addParam((short) filePathLen);
        addParam(mFwFilePath.getBytes());
        addParam(cSum.getBytes());
    }
}
