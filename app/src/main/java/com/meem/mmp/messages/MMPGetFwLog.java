package com.meem.mmp.messages;

/**
 * Command to download firmware log file.
 *
 * @author Arun T A
 */

public class MMPGetFwLog extends MMPCtrlMsg {
    public MMPGetFwLog(MMPFPath fpath) {
        super(MMPConstants.MMP_CODE_GET_FW_LOG);
        addParam(fpath.asArray());
    }
}
