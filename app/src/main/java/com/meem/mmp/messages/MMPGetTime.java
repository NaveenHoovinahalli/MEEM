package com.meem.mmp.messages;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Arun T A
 */

public class MMPGetTime extends MMPCtrlMsg {

    public MMPGetTime(MMPCtrlMsg copy) {
        super(copy);
    }

    public boolean prapareResponse() {
        setType(MMPConstants.MMP_TYPE_RES);

        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH); // Note: zero based!
        int day = now.get(Calendar.DAY_OF_MONTH);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);

        addParam((short) year);
        addParam((byte) (month));
        addParam((byte) day);
        addParam((byte) hour);
        addParam((byte) minute);
        addParam((byte) second);

        return true;
    }
}
