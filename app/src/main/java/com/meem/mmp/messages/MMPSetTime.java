package com.meem.mmp.messages;

import java.util.Calendar;

/**
 * @author Arun T A
 */

public class MMPSetTime extends MMPCtrlMsg {

    public MMPSetTime() {
        super(MMPConstants.MMP_CODE_SET_TIME);

        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH); // Note: zero based!
        int day = now.get(Calendar.DAY_OF_MONTH);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);

        addParam((short) year);
        addParam((byte) (month + 1));
        addParam((byte) day);
        addParam((byte) hour);
        addParam((byte) minute);
        addParam((byte) second);
    }
}
