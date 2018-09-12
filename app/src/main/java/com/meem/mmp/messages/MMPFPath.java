package com.meem.mmp.messages;

import com.meem.utils.GenUtils;

import java.nio.ByteBuffer;

/**
 * @author Arun T A
 */

public class MMPFPath {

    private String mFilePath;
    private long mFileSize;

    public MMPFPath(String path, long fsize) {
        mFilePath = path;
        mFileSize = fsize;
    }

    public String toString() {
        return mFilePath;
    }

    public byte[] asArray() {
        short pathLen = 0;
        if (null != mFilePath) {
            pathLen = (short) mFilePath.length();
        }

        int bufLen = 2 + pathLen;
        if (mFileSize != 0) {
            bufLen += 4;
        }

        ByteBuffer bbFPath = ByteBuffer.allocate(bufLen);

        if (mFileSize != 0) {
            // TODO: possible size issue
            bbFPath.putInt((int) mFileSize);
        }

        bbFPath.putShort(pathLen);
        if (null != mFilePath) {
            bbFPath.put(mFilePath.getBytes());
        }

        return bbFPath.array();
    }

    // for supporting MMP change on 21Aug2014
    public String getChecksum() {
        String csum = null;
        if (null != mFilePath) {
            csum = GenUtils.getFileMD5(mFilePath, null);
        }

        return csum;
    }

    public String getPath() {
        return mFilePath;
    }
}
