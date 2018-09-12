package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPSessionComplete extends MMPCtrlMsg {
    public MMPSessionComplete(MMPCtrlMsg copy) {
        super(copy);
    }

    public byte getHandle() {
        return getByteParam();
    }
}
