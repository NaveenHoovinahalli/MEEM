package com.meem.businesslogic;

import com.meem.mmp.mml.MMLGenericDataDesc;

public interface DatdProcessingListener {
    public boolean onNewGenericDataDesc(MMLGenericDataDesc genericDataDesc);

    public void onDatdProcessingCompletion(boolean result);
}
