package com.meem.ui;

import com.meem.events.ResponseCallback;

import java.util.ArrayList;

/**
 * Created by SCS on 9/6/2016.
 */
public interface RestoreOrShareSmartDataInterface {

    void onRestoreSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfo, boolean isMirror, ResponseCallback responseCallback);
    void onShareSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfo, boolean isMirror, ResponseCallback responseCallback);
    void onDeleteSmartData(String vaultId, Byte catCode, ArrayList<SmartDataInfo> smartDataInfo, boolean isMirror, ResponseCallback responseCallback);
}
