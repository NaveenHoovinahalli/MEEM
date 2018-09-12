package com.meem.mmp.mml;

/**
 * This class represents smart data as described in DATD and SESD. This class is used during DATD parsing and and SESD creation.
 *
 * @author Arun T A
 */

public class MMLSmartDataDesc {
    public byte mCatCode = 0;
    // as of now, creation time is ignored.
    public String mCreationTime = "";
    // as of now, DATD do not provide smart data file path. .. :)
    public String[] mPaths = new String[2];
    public long[] mSizes = new long[2];
    public String[] mCSums = new String[2];
    // === For V2 ===
    public String[] mMeemPaths = new String[2];

    // for debugging.
    public String toString() {
        String str = "";
        for (int i = 0; i < 2; i++) {
            str += "[" + i + "]: " + mCatCode + ": " + mPaths[i] + ", size: " + mSizes[i] + ", csum: " + mCSums[i];
            if (i == 0) str += "\n";
        }
        return str;
    }
}
