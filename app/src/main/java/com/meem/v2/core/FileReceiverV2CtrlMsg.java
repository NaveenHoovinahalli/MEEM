package com.meem.v2.core;

/**
 * @author Arun T A
 */

class FileReceiverV2CtrlMsg {
    public String fName;
    public byte xfrId;
    public long fSize;
    public int chunkSize;
    public String chkSum;
    public byte xOfy;

    FileReceiverV2CtrlMsg(String fName, byte xfrId, long fSize, int chunkSize, byte xOfy, String chkSum) {
        this.fName = fName;
        this.xfrId = xfrId;
        this.fSize = fSize;
        this.chunkSize = chunkSize;
        this.xOfy = xOfy;
        this.chkSum = chkSum;
    }

    FileReceiverV2CtrlMsg(String fName, byte xOfy) {
        this.fName = fName;
        this.xfrId = 0;
        this.fSize = 0;
        this.chunkSize = 0;
        this.xOfy = xOfy;
        this.chkSum = null;
    }
}
