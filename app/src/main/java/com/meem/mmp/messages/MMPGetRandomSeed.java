package com.meem.mmp.messages;

import java.util.Random;

/**
 * @author Arun T A
 */

public class MMPGetRandomSeed extends MMPCtrlMsg {
    public MMPGetRandomSeed(MMPCtrlMsg copy) {
        super(copy);
    }

    public boolean prepareResponse() {
        setType(MMPConstants.MMP_TYPE_RES);
        Random rn = new Random();
        addParam(rn.nextInt());
        return true;
    }
}
