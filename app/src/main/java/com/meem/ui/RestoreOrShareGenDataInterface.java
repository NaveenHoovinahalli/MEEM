package com.meem.ui;

import com.meem.events.ResponseCallback;

import java.util.ArrayList;

/**
 * Created by SCS on 9/6/2016.
 */
public interface RestoreOrShareGenDataInterface {

    void onRestoreGenData(String vaultId, Byte catCode, ArrayList<GenDataInfo> genDataInfo, boolean isMirror, ResponseCallback responseCallback);
    void onShareGenData(String vaultId, Byte catCode, ArrayList<GenDataInfo> genDataInfo, boolean isMirror, ResponseCallback responseCallback);
    void onDeleteGenData(String vaultId, Byte catCode, ArrayList<GenDataInfo> genDataInfo, boolean isMirror, ResponseCallback responseCallback);
}
