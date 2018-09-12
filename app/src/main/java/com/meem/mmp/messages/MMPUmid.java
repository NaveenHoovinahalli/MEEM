package com.meem.mmp.messages;

import java.nio.ByteBuffer;

/**
 * @author Arun T A
 */

public class MMPUmid {

    private String mId = null;

    public MMPUmid(String umid) {
        mId = umid;
    }

    public byte[] asArray() {
        int len = 0;

        if (null != mId) {
            len = mId.length();
        }

        if (len >= MMPConstants.MMP_MAX_PARAM_LENGTH) {
            throw new IllegalArgumentException("Umid length exceeds maximum mmp parameter limit");
        }

        ByteBuffer bbUmid = ByteBuffer.allocate(len + 1);
        bbUmid.put((byte) len);

        if (null != mId) {
            bbUmid.put(mId.getBytes());
        }

        return bbUmid.array();
    }
}
