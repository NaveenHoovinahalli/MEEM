package com.meem.v2.net;

import android.content.Context;

import com.meem.androidapp.AccessoryInterface;
import com.meem.androidapp.ProductSpecs;
import com.meem.v2.mmp.MMPV2Constants;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by arun on 18/7/17.
 */

public class MNetAccessory implements AccessoryInterface {
    MNetTCPClient mTCPClient;
    boolean mSlowSpeedMode;

    public MNetAccessory(MNetTCPClient tcpClient) {
        mTCPClient = tcpClient;
        mSlowSpeedMode = false;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public InputStream getInputStream() {
        return (null != mTCPClient) ? mTCPClient.getInputStream() : null;
    }

    @Override
    public OutputStream getOutputStream() {
        return (null != mTCPClient) ? mTCPClient.getOutputStream() : null;
    }

    @Override
    public int getHwVersion() {
        return ProductSpecs.HW_VERSION_2_INEDA;
    }

    @Override
    public void setHwVersion(int version) {
        // only V2 support network feature
    }

    @Override
    public boolean isSlowSpeedMode() {
        return mSlowSpeedMode;
    }

    @Override
    public void setSlowSpeedMode(boolean slow) {
        mSlowSpeedMode = slow;
    }

    @Override
    public void closeAccessory() {
        if(null != mTCPClient) mTCPClient.close();
    }
}
