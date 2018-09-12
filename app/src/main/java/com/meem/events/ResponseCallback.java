package com.meem.events;

public interface ResponseCallback {
    boolean execute(boolean result, Object info, Object extraInfo);
}
