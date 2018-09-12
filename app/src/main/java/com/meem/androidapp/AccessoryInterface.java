package com.meem.androidapp;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by arun on 18/7/17.
 */

public interface AccessoryInterface {
    boolean isRemote();
    InputStream getInputStream();
    OutputStream getOutputStream();
    int getHwVersion();
    void setHwVersion(int version);
    boolean isSlowSpeedMode();
    void setSlowSpeedMode(boolean slow);
    void closeAccessory();
}
