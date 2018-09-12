package com.meem.androidapp;

import com.meem.events.EventCode;
import com.meem.events.MeemEvent;

/**
 * Created by arun on 19/6/17.
 * Used to give a more user friendly update of session in ui
 */

public class SessionCommentary {
    private MeemEvent mEvent;

    public static final byte OPMODE_DONTCARE = 0;
    public static final byte OPMODE_PROCESSING_PHONE_ITEMS = 1;
    public static final byte OPMODE_PROCESSING_MEEM_ITEMS = 2;

    /**
     * NEw commentary
     *
     * @param current   current item number
     * @param total     total items number
     * @param catCode the category
     * @param opMode to be used during "confirming data phase to diff phone db processing and meem db processing
     */
    public SessionCommentary(EventCode evtCode, int current, int total, byte catCode, byte opMode) {
        mEvent = new MeemEvent(evtCode);

        mEvent.setArg0(current);
        mEvent.setArg1(total);
        mEvent.setInfo(catCode);
        mEvent.setExtraInfo(opMode);
    }

    public void post() {
        UiContext.getInstance().postEvent(mEvent);
    }
}
