package com.meem.mmp.messages;

import com.meem.androidapp.ProductSpecs;

import java.nio.ByteBuffer;

/**
 * @author Arun T A
 */

public class MMPUpid {

    private String mId = null;

    public MMPUpid(String upid) {
        mId = upid;
    }

    public byte[] asArray() {
        int len = mId.length();

        if (len >= MMPConstants.MMP_MAX_PARAM_LENGTH) {
            throw new IllegalArgumentException("Upid length exceeds maximum mmp parameter limit");
        }

        // TODO: Remove this HACK by fixing firmware
        len += ProductSpecs.HACK_UPID_LEN_FIX;

        ByteBuffer bbUpid = ByteBuffer.allocate(len + 1);
        bbUpid.put((byte) (len));
        bbUpid.put(mId.getBytes());

        return bbUpid.array();
    }

    @Override
    public final String toString() {
        return mId;
    }
}
