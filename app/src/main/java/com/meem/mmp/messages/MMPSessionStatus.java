package com.meem.mmp.messages;

import android.util.Log;

/**
 * WARNING: This is not yet finalized.
 * <p/>
 * MSG code - 0x3ECCE5 Params 1 - session handle (1byte) 2 - catcode (if 0xAA is there, its irrespective of the catcode) (1 byte) 3 - Status
 * code (1 byte). Below are the details:
 * <p/>
 * B - When particular category operations has started P - Progress. Need to decide later. S- when particular category operation has ended
 * <p/>
 * 4 - If the above code is P, then the next 2 bytes will represent the percentage for the progress bar.
 *
 * @author Arun T A
 */

public class MMPSessionStatus extends MMPCtrlMsg {
    private static final String tag = "MMPSessionStatus";

    public MMPSessionStatus(byte handle, byte interval, MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_SESSION_STATUS);
        addParam(handle);
        addParam(interval);
        addParam(fpath.asArray());
    }

    // This is what we are interested in - in the present implementation, MEEM
    // will send
    // this to us during a session. This will parse response message header.
    public MMPSessionStatus(MMPCtrlMsg copy) {
        super(copy);
    }

    public MMPSessionStatusInfo getInfo() {
        MMPSessionStatusInfo info = new MMPSessionStatusInfo();

        info.mHandle = getByteParam();
        info.mCatCode = getByteParam();

        byte typeCode = getByteParam();
        // no problem to use ascii chars
        if (typeCode == 'P') {
            info.mType = MMPSessionStatusInfo.Type.PROGRESS;
            info.mProgress = getShortParam();
        } else if (typeCode == 'B') {
            info.mType = MMPSessionStatusInfo.Type.STARTED;
        } else if (typeCode == 'S') {
            info.mType = MMPSessionStatusInfo.Type.ENDED;
        } else {
            Log.e(tag, "Unknown progress code in session status message: " + String.valueOf((int) typeCode));
        }

        return info;
    }
}
