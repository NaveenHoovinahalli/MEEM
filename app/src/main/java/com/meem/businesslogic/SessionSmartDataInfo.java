package com.meem.businesslogic;

import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.mml.MMLSmartDataDesc;

import java.util.ArrayList;
import java.util.HashMap;

public class SessionSmartDataInfo {
    public ArrayList<Byte> mCatCodes;
    public HashMap<Byte, ArrayList<MMPFPath>> mCatsFilesMap;
    // === For V2 ===
    public HashMap<Byte, MMLSmartDataDesc> mDbFilesMap;

    public int size() {
        if (null != mCatCodes) {
            return mCatCodes.size();
        }
        return 0;
    }

    public ArrayList<Byte> getCatCodes() {
        return mCatCodes;
    }

    public void setCatCodes(ArrayList<Byte> mCatCodes) {
        this.mCatCodes = mCatCodes;
    }

    public HashMap<Byte, ArrayList<MMPFPath>> getCatsFilesMap() {
        return mCatsFilesMap;
    }

    public void setCatsFilesMap(HashMap<Byte, ArrayList<MMPFPath>> mCatsFilesMap) {
        this.mCatsFilesMap = mCatsFilesMap;
    }
}
