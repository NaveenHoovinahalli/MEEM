package com.meem.v2.phone;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateUtils;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.ui.SmartDataInfo;
import com.meem.utils.CRC32;
import com.meem.utils.GenUtils;
import com.meem.v2.mmp.MMPV2Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Implementation of calendar backup and restore Methods.
 *
 * @author karthik B S
 */

public class CalendersV2 extends SmartDataCategoryV2 {

    private static final String tag = "CalendarsV2";
    Event mCurrentMirrorItem, mCurrentPlusItem, mCurrentPhoneItem;
    Cursor calendarCursor;
    ArrayList<String> calendarColumnNames = new ArrayList<String>();

    // added due to IOS compatibility
    boolean apiLevelIsAbove17 = false;
    // Sql DB related
    private CalendarsDbV2 mCalendarsMirrDb;
    private CalendarsDbV2 mCalendarsArchiveDb;
    private AppLocalData mAppData = AppLocalData.getInstance();

    // For Db Related
    public CalendersV2(Context context, String upid) {
        super(MMPV2Constants.MMP_CATCODE_CALENDER, context, Events.CONTENT_URI, null);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            apiLevelIsAbove17 = true;
            // dbgTrace();
            // dbgTrace("API level is equal/above 17");
        }
        try {
            calendarCursor = mContentResolver.query(Calendars.CONTENT_URI, null, null, null, null);
            calendarCursor.moveToFirst();

            for (String colName : calendarCursor.getColumnNames()) {
                calendarColumnNames.add(colName);
                // dbgTrace("columname: "+colName);
            }
            calendarCursor.close();
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        mContext = context;
        ArrayList<String> mtableNamesList = new ArrayList<String>();

        mtableNamesList.add(CalendarTableNamesV2.CREATE_VAULT_LINKS_COUNT_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_VAULT_CALENDAR_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_CALENDAR_EVENT_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_CAL_COLOR_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_REMINDERS_LIST_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_RRULE_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_DAYSOFWEEK_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_DAYSOFMONTH_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_DAYSOFYEAR_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_WEEKOFYEAR_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_MONTHSOFYEAR_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_SETPOSITIONS_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_LIST_OF_ATTENDEES_TABLE);
        mtableNamesList.add(CalendarTableNamesV2.CREATE_LIST_OF_INSTANCES_TABLE);

        mCalendarsMirrDb = new CalendarsDbV2(context, mAppData.getCalendarV2MirrorDbFullPath(upid), mtableNamesList);
        mCalendarsMirrDb.getRowCount();

        mCalendarsArchiveDb = new CalendarsDbV2(context, mAppData.getCalendarV2PlusDbFullPath(upid), mtableNamesList);
        mCalendarsArchiveDb.getRowCount();

        dbgTrace();
    }

    public boolean iterateNaddToPhoneDb(ArrayList<SmartDataInfo> smartDataInfoList, Boolean isMirr) {
        dbgTrace();
        boolean res = true;
        for (SmartDataInfo sdInfo : smartDataInfoList) {
            if (mAbortFlag) {
                return false;
            }
            if (isMirr) {
                mCurrentMirrorItem = mCalendarsMirrDb.getEventForChecksum(sdInfo.getChecksum());
            } else {
                mCurrentMirrorItem = mCalendarsArchiveDb.getEventForChecksum(sdInfo.getChecksum());
            }
            if (!addMirrorItemToPhone()) {
                res = false;
                break;
            }

        }

        dbgTrace("Finished Restoring Individual Calender Events");
        return res;
    }

    @SuppressLint("InlinedApi")
    @Override
    long getNextPhoneDbItem() {
        dbgTrace();

        mCurrentPhoneItem = new Event();

        if (apiLevelIsAbove17) {
            mCurrentPhoneItem.isApiLevel17 = true;
        }
        // dbgTrace("API LEVEL is > 17");
        try {
            if (mColumnNames.contains(Events.DELETED)) {
                mCurrentPhoneItem.deleted = mCursor.getString(mCursor.getColumnIndex(Events.DELETED));
                if (mCurrentPhoneItem.deleted == null) {
                    mCurrentPhoneItem.deleted = "";
                }
                if (mCurrentPhoneItem.deleted.equalsIgnoreCase("1")) {
                    dbgTrace("content is deleted, returning checksum as zero");
                    mCursor.moveToNext();
                    return 0;
                } else {
                    // dbgTrace("deleted: "+mCurrentPhoneItem.deleted);
                    mCurrentPhoneItem.deleted = "0";
                }
            }

            if (mColumnNames.contains(Events.EVENT_END_TIMEZONE)) {
                mCurrentPhoneItem.eventEndTimezone = mCursor.getString(mCursor.getColumnIndex(Events.EVENT_END_TIMEZONE));
                if (mCurrentPhoneItem.eventEndTimezone == null) {
                    mCurrentPhoneItem.eventEndTimezone = "";
                }
            }

            if (isApiEqualOrAbove17()) {
                if (mColumnNames.contains(Events.IS_ORGANIZER)) {
                    mCurrentPhoneItem.isOrganizer = mCursor.getString(mCursor.getColumnIndex(Events.IS_ORGANIZER));
                    if (mCurrentPhoneItem.isOrganizer == null) {
                        mCurrentPhoneItem.isOrganizer = "";
                    }
                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC8)) {
                mCurrentPhoneItem.calSync8 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC8));
                if (mCurrentPhoneItem.calSync8 == null) {
                    mCurrentPhoneItem.calSync8 = "";
                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC9)) {
                mCurrentPhoneItem.calSync9 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC9));
                if (mCurrentPhoneItem.calSync9 == null) {
                    mCurrentPhoneItem.calSync9 = "";
                }
            }
            if (mColumnNames.contains(Events.CAL_SYNC7)) {
                mCurrentPhoneItem.calSync7 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC7));
                if (mCurrentPhoneItem.calSync7 == null) {
                    mCurrentPhoneItem.calSync7 = "";
                }
            }
            if (mColumnNames.contains(Events.VISIBLE)) {
                mCurrentPhoneItem.visible = mCursor.getString(mCursor.getColumnIndex(Events.VISIBLE));
                if (mCurrentPhoneItem.visible == null) {
                    mCurrentPhoneItem.visible = "";
                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC6)) {
                mCurrentPhoneItem.calSync6 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC6));
                if (mCurrentPhoneItem.calSync6 == null) {
                    mCurrentPhoneItem.calSync6 = "";
                }
            }
            if (mColumnNames.contains(Events.CAL_SYNC5)) {
                mCurrentPhoneItem.calSync5 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC5));
                if (mCurrentPhoneItem.calSync5 == null) {
                    mCurrentPhoneItem.calSync5 = "";
                }
            }
            if (mColumnNames.contains(Events.RRULE)) {
                mCurrentPhoneItem.rrule = mCursor.getString(mCursor.getColumnIndex(Events.RRULE));
                if (mCurrentPhoneItem.rrule == null) {
                    mCurrentPhoneItem.rrule = "";
                    mCurrentPhoneItem.hasRrule = "NO"; // IOS Compatability
                } else {
                    mCurrentPhoneItem.hasRrule = "YES"; // IOS Compatability
                    // split the values in rrule and fill the IOS rrObj
                    Rrobj tempRobj = new Rrobj();
                    String mRrule = mCurrentPhoneItem.rrule;
                    String subFieldWithValue = "";
                    String[] rruleStrs = mRrule.split(";");
                    // int weekno=0;
                    for (int i = 0; i < rruleStrs.length; ++i) {
                        subFieldWithValue = rruleStrs[i];
                        String[] value = subFieldWithValue.split("=");
                        if (value[0].equalsIgnoreCase("freq")) {
                            if (value[1].equalsIgnoreCase("daily")) {
                                tempRobj.frequency = "0";
                            }
                            if (value[1].equalsIgnoreCase("weekly")) {
                                tempRobj.frequency = "1";
                            }
                            if (value[1].equalsIgnoreCase("monthly")) {
                                tempRobj.frequency = "2";
                            }
                            if (value[1].equalsIgnoreCase("yearly")) {
                                tempRobj.frequency = "3";
                            }
                        }
                        if (value[0].equalsIgnoreCase("interval")) {
                            tempRobj.interval = value[1];
                        }
                        if (value[0].equalsIgnoreCase("until")) {
                            tempRobj.rrend = value[1];
                        }
                        if (value[0].equalsIgnoreCase("byweekno")) {
                            String tempWeekNums = value[1];
                            for (String token : tempWeekNums.split(",")) {
                                tempRobj.weeksOfTheYear.add(Integer.parseInt(token));

                            }
                            System.err.println("weeksOfTheYear: " + tempRobj.weeksOfTheYear);
                        }
                        if (value[0].equalsIgnoreCase("bymonthday")) { // need to test
                            String tempBydays = value[1];
                            for (String token : tempBydays.split(",")) {
                                tempRobj.daysOfTheMonth.add(Integer.parseInt(token));
                            }

                            System.err.println("daysOfTheMonth " + tempRobj.daysOfTheMonth);
                        }
                        if (value[0].equalsIgnoreCase("bymonth")) { // need to test
                            String tempBymonths = value[1];
                            for (String token : tempBymonths.split(",")) {
                                tempRobj.monthsOfTheYear.add(Integer.parseInt(token));
                            }

                            System.err.println("MonthsOfTheYear " + tempRobj.monthsOfTheYear);
                        }
                        if (value[0].equalsIgnoreCase("byyearday")) { // need to test
                            String tempByYearDay = value[1];
                            for (String token : tempByYearDay.split(",")) {
                                tempRobj.daysOfTheYear.add(Integer.parseInt(token));
                            }

                            System.err.println("DaysOfTheYear " + tempRobj.daysOfTheYear);
                        }
                        if (value[0].equalsIgnoreCase("count")) { // need to test
                            tempRobj.occurenceCount = Integer.parseInt(value[1]);
                            System.err.println("occurence count: " + tempRobj.occurenceCount);
                        }
                        if (value[0].equalsIgnoreCase("byday")) {
                            String tempBydays = value[1];
                            System.err.println("\nbyday: " + tempBydays);
                            int tempWeekno = 0;
                            boolean weeknoExist = false;
                            if (tempRobj.weeksOfTheYear.size() == 1) {
                                weeknoExist = true;
                                tempWeekno = tempRobj.weeksOfTheYear.get(1);
                            }
                            // int index=0;
                            for (String token : tempBydays.split(",")) {
                                System.err.println(token);

                                if (token.charAt(0) == '-') {
                                    //Does starts with prefix eg: -1MO -1st monday ,-3SA -3rd saturday

                                    //first fill the setposition field with prefix number
                                    tempRobj.setPositions.add(Integer.parseInt(Character.toString(token.charAt(1))));

                                    token = Character.toString(token.charAt(2)) + token.charAt(3);
                                    System.err.println("after splitting number and weekdays,value of token: " + token);

                                } else if (Character.isDigit(token.charAt(0))) {
                                    //Does starts with prefix eg: 1SU -1st sunday ,3SA -3rd saturday

                                    //first fill the setposition field with prefix number
                                    tempRobj.setPositions.add(Integer.parseInt(Character.toString(token.charAt(0))));

                                    token = Character.toString(token.charAt(1)) + token.charAt(2);
                                    System.err.println("after splitting number and weekdays,value of token: " + token);
                                }

                                DaysOfTheWeek obj = new DaysOfTheWeek();
                                if (weeknoExist) {
                                    obj.weekNumber = tempWeekno;
                                }
                                if (token.equalsIgnoreCase("su")) {
                                    obj.dayOfTheWeek = 1;
                                }
                                if (token.equalsIgnoreCase("mo")) {
                                    obj.dayOfTheWeek = 2;
                                }
                                if (token.equalsIgnoreCase("tu")) {
                                    obj.dayOfTheWeek = 3;
                                }
                                if (token.equalsIgnoreCase("we")) {
                                    obj.dayOfTheWeek = 4;
                                }
                                if (token.equalsIgnoreCase("th")) {
                                    obj.dayOfTheWeek = 5;
                                }
                                if (token.equalsIgnoreCase("fr")) {
                                    obj.dayOfTheWeek = 6;
                                }
                                if (token.equalsIgnoreCase("sa")) {
                                    obj.dayOfTheWeek = 7;
                                }
                                tempRobj.daysOfTheWeek.add(obj);
                            }

                        }
                    }
                    mCurrentPhoneItem.rrObjs.add(tempRobj);
                }
                // Log.d(tag, "\n rrule :" + mCurrentPhoneItem.rrule);
            }

            if (mColumnNames.contains(Events.CAL_SYNC4)) {
                mCurrentPhoneItem.calSync4 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC4));
                if (mCurrentPhoneItem.calSync4 == null) {
                    mCurrentPhoneItem.calSync4 = "";
                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC3)) {
                mCurrentPhoneItem.calSync3 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC3));
                if (mCurrentPhoneItem.calSync3 == null) {
                    mCurrentPhoneItem.calSync3 = "";
                }
            }
            if (mColumnNames.contains(Events.CAL_SYNC1)) {
                mCurrentPhoneItem.calSync1 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC1));
                if (mCurrentPhoneItem.calSync1 == null) {
                    mCurrentPhoneItem.calSync1 = "";
                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC2)) {
                mCurrentPhoneItem.calSync2 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC2));
                if (mCurrentPhoneItem.calSync2 == null) {
                    mCurrentPhoneItem.calSync2 = "";
                }
            }

            if (mColumnNames.contains(Events.HAS_ALARM)) {
                mCurrentPhoneItem.hasAlarm = mCursor.getString(mCursor.getColumnIndex(Events.HAS_ALARM));
                if (mCurrentPhoneItem.hasAlarm == null) {
                    mCurrentPhoneItem.hasAlarm = "";
                }
            }

            if (mColumnNames.contains(Events.RDATE)) {
                mCurrentPhoneItem.rdate = mCursor.getString(mCursor.getColumnIndex(Events.RDATE));
                if (mCurrentPhoneItem.rdate == null) {
                    mCurrentPhoneItem.rdate = "";
                }
                Log.d(tag, "\nrdate:" + mCurrentPhoneItem.rdate);
            }

            if (mColumnNames.contains(Events.DTSTART)) {
                mCurrentPhoneItem.dtstart = mCursor.getString(mCursor.getColumnIndex(Events.DTSTART));
                if (mCurrentPhoneItem.dtstart == null) {
                    mCurrentPhoneItem.dtstart = "";
                }
                // Log.d(tag, "\ndtstart:" + mCurrentPhoneItem.dtstart);
            }
            if (mColumnNames.contains(Events.SYNC_DATA1)) {
                mCurrentPhoneItem.syncData1 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA1));
                if (mCurrentPhoneItem.syncData1 == null) {
                    mCurrentPhoneItem.syncData1 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA2)) {
                mCurrentPhoneItem.syncData2 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA2));
                if (mCurrentPhoneItem.syncData2 == null) {
                    mCurrentPhoneItem.syncData2 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA3)) {
                mCurrentPhoneItem.syncData3 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA3));
                if (mCurrentPhoneItem.syncData3 == null) {
                    mCurrentPhoneItem.syncData3 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA4)) {
                mCurrentPhoneItem.syncData4 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA4));
                if (mCurrentPhoneItem.syncData4 == null) {
                    mCurrentPhoneItem.syncData4 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA5)) {
                mCurrentPhoneItem.syncData5 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA5));
                if (mCurrentPhoneItem.syncData5 == null) {
                    mCurrentPhoneItem.syncData5 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA6)) {
                mCurrentPhoneItem.syncData6 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA6));
                if (mCurrentPhoneItem.syncData6 == null) {
                    mCurrentPhoneItem.syncData6 = "";
                }
            }

            if (mColumnNames.contains(Events.SYNC_DATA7)) {
                mCurrentPhoneItem.syncData7 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA7));
                if (mCurrentPhoneItem.syncData7 == null) {
                    mCurrentPhoneItem.syncData7 = "";
                }
            }

            if (mColumnNames.contains(Events.SYNC_DATA8)) {
                mCurrentPhoneItem.syncData8 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA8));
                if (mCurrentPhoneItem.syncData8 == null) {
                    mCurrentPhoneItem.syncData8 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA10)) {
                mCurrentPhoneItem.syncData10 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA10));
                if (mCurrentPhoneItem.syncData10 == null) {
                    mCurrentPhoneItem.syncData10 = "";
                }
            }
            if (mColumnNames.contains(Events.SYNC_DATA9)) {
                mCurrentPhoneItem.syncData9 = mCursor.getString(mCursor.getColumnIndex(Events.SYNC_DATA9));
                if (mCurrentPhoneItem.syncData9 == null) {
                    mCurrentPhoneItem.syncData9 = "";
                }
            }

            if (mColumnNames.contains(Events.HAS_ATTENDEE_DATA)) {
                mCurrentPhoneItem.hasAttendeeData = mCursor.getString(mCursor.getColumnIndex(Events.HAS_ATTENDEE_DATA));
                if (mCurrentPhoneItem.hasAttendeeData == null) {
                    mCurrentPhoneItem.hasAttendeeData = "";
                }
            }

            if (mColumnNames.contains(Events.CALENDAR_TIME_ZONE)) {
                mCurrentPhoneItem.calendarTimezone = mCursor.getString(mCursor.getColumnIndex(Events.CALENDAR_TIME_ZONE));
                if (mCurrentPhoneItem.calendarTimezone == null) {
                    mCurrentPhoneItem.calendarTimezone = "";
                }
            }
            if (mColumnNames.contains(Events.DESCRIPTION)) {
                mCurrentPhoneItem.description = mCursor.getString(mCursor.getColumnIndex(Events.DESCRIPTION));
                if (mCurrentPhoneItem.description == null) {
                    mCurrentPhoneItem.description = "";
                    mCurrentPhoneItem.hasNotes = "NO";
                } else {
                    mCurrentPhoneItem.hasNotes = "YES";
                }
                // Log.d(tag, "\ndescription:" + mCurrentPhoneItem.description);
            }
            if (mColumnNames.contains(Events.CALENDAR_ACCESS_LEVEL)) {
                mCurrentPhoneItem.calendarAccessLevel = mCursor.getString(mCursor.getColumnIndex(Events.CALENDAR_ACCESS_LEVEL));
                if (mCurrentPhoneItem.calendarAccessLevel == null) {
                    mCurrentPhoneItem.calendarAccessLevel = "";
                }
            }
            if (mColumnNames.contains(Events.CUSTOM_APP_PACKAGE)) {
                mCurrentPhoneItem.customAppPackage = mCursor.getString(mCursor.getColumnIndex(Events.CUSTOM_APP_PACKAGE));
                if (mCurrentPhoneItem.customAppPackage == null) {
                    mCurrentPhoneItem.customAppPackage = "";
                }
            }
            if (mColumnNames.contains(Events.ACCOUNT_TYPE)) {
                mCurrentPhoneItem.accountType = mCursor.getString(mCursor.getColumnIndex(Events.ACCOUNT_TYPE));
                if (mCurrentPhoneItem.accountType == null) {
                    mCurrentPhoneItem.accountType = "";
                }
                // dbgTrace("\n account type(event):" +
                // mCurrentPhoneItem.account_type);
            }
            if (mColumnNames.contains(Events.HAS_EXTENDED_PROPERTIES)) {
                mCurrentPhoneItem.hasExtendedProperties = mCursor.getString(mCursor.getColumnIndex(Events.HAS_EXTENDED_PROPERTIES));
                if (mCurrentPhoneItem.hasExtendedProperties == null) {
                    mCurrentPhoneItem.hasExtendedProperties = "";
                }
            }

            if (mColumnNames.contains(Events.EVENT_LOCATION)) {
                mCurrentPhoneItem.eventLocation = mCursor.getString(mCursor.getColumnIndex(Events.EVENT_LOCATION));
                if (mCurrentPhoneItem.eventLocation == null) {
                    mCurrentPhoneItem.eventLocation = "";
                }
            }

            if (mColumnNames.contains(Events.DTEND)) {
                mCurrentPhoneItem.dtend = mCursor.getString(mCursor.getColumnIndex(Events.DTEND));
                if (mCurrentPhoneItem.dtend == null) {
                    mCurrentPhoneItem.dtend = "";
                }
            }
            if (mColumnNames.contains(Events.ALL_DAY)) {
                mCurrentPhoneItem.allDay = mCursor.getString(mCursor.getColumnIndex(Events.ALL_DAY));
                if (mCurrentPhoneItem.allDay == null) {
                    mCurrentPhoneItem.allDay = "";
                }
            }
            if (mColumnNames.contains(Events.ORGANIZER)) {
                mCurrentPhoneItem.organizer = mCursor.getString(mCursor.getColumnIndex(Events.ORGANIZER));
                if (mCurrentPhoneItem.organizer == null) {
                    mCurrentPhoneItem.organizer = "";
                }
            }
            if (mColumnNames.contains(Events.LAST_SYNCED)) {
                mCurrentPhoneItem.lastSynced = mCursor.getString(mCursor.getColumnIndex(Events.LAST_SYNCED));
                if (mCurrentPhoneItem.lastSynced == null) {
                    mCurrentPhoneItem.lastSynced = "";
                }
            }

            if (mColumnNames.contains(Events.ACCOUNT_NAME)) {
                mCurrentPhoneItem.accountName = mCursor.getString(mCursor.getColumnIndex(Events.ACCOUNT_NAME));
                if (mCurrentPhoneItem.accountName == null) {
                    mCurrentPhoneItem.accountName = "";
                }
                // dbgTrace("\naccount name(event):" +
                // mCurrentPhoneItem.ACCOUNT_NAME);
            }
            if (mColumnNames.contains(Events.ORIGINAL_INSTANCE_TIME)) {
                mCurrentPhoneItem.originalInstanceTime = mCursor.getString(mCursor.getColumnIndex(Events.ORIGINAL_INSTANCE_TIME));
                if (mCurrentPhoneItem.originalInstanceTime == null) {
                    mCurrentPhoneItem.originalInstanceTime = "";
                }
            }

            if (mColumnNames.contains(Events.SELF_ATTENDEE_STATUS)) {
                mCurrentPhoneItem.selfAttendeeStatus = mCursor.getString(mCursor.getColumnIndex(Events.SELF_ATTENDEE_STATUS));
                if (mCurrentPhoneItem.selfAttendeeStatus == null) {
                    mCurrentPhoneItem.selfAttendeeStatus = "";
                }
            }
            if (mColumnNames.contains(Events.EVENT_COLOR_KEY)) {
                mCurrentPhoneItem.eventColorIndex = mCursor.getString(mCursor.getColumnIndex(Events.EVENT_COLOR_KEY));
                if (mCurrentPhoneItem.eventColorIndex == null) {
                    mCurrentPhoneItem.eventColorIndex = "";
                }
            }
            if (mColumnNames.contains(Events.EVENT_TIMEZONE)) {
                mCurrentPhoneItem.eventTimezone = mCursor.getString(mCursor.getColumnIndex(Events.EVENT_TIMEZONE));
                if (mCurrentPhoneItem.eventTimezone == null) {
                    mCurrentPhoneItem.eventTimezone = "";
                }
            }
            if (mColumnNames.contains(Events.ALLOWED_AVAILABILITY)) {
                mCurrentPhoneItem.allowedAvailability = mCursor.getString(mCursor.getColumnIndex(Events.ALLOWED_AVAILABILITY));
                if (mCurrentPhoneItem.allowedAvailability == null) {
                    mCurrentPhoneItem.allowedAvailability = "";
                }
            }

            if (mColumnNames.contains(Events.CAN_ORGANIZER_RESPOND)) {
                mCurrentPhoneItem.canOrganizerRespond = mCursor.getString(mCursor.getColumnIndex(Events.CAN_ORGANIZER_RESPOND));
                if (mCurrentPhoneItem.canOrganizerRespond == null) {
                    mCurrentPhoneItem.canOrganizerRespond = "";
                }
            }
            if (mColumnNames.contains(Events.CAN_MODIFY_TIME_ZONE)) {
                mCurrentPhoneItem.canModifyTimeZone = mCursor.getString(mCursor.getColumnIndex(Events.CAN_MODIFY_TIME_ZONE));
                if (mCurrentPhoneItem.canModifyTimeZone == null) {
                    mCurrentPhoneItem.canModifyTimeZone = "";
                }
            }
            if (mColumnNames.contains(Events.LAST_DATE)) {
                mCurrentPhoneItem.lastDate = mCursor.getString(mCursor.getColumnIndex(Events.LAST_DATE));
                if (mCurrentPhoneItem.lastDate == null) {
                    mCurrentPhoneItem.lastDate = "";
                }
            }
            if (mColumnNames.contains(Events.GUESTS_CAN_MODIFY)) {
                mCurrentPhoneItem.guestsCanModify = mCursor.getString(mCursor.getColumnIndex(Events.GUESTS_CAN_MODIFY));
                if (mCurrentPhoneItem.guestsCanModify == null) {
                    mCurrentPhoneItem.guestsCanModify = "";
                }
            }

            if (mColumnNames.contains(Events.GUESTS_CAN_SEE_GUESTS)) {
                mCurrentPhoneItem.guestsCanSeeGuests = mCursor.getString(mCursor.getColumnIndex(Events.GUESTS_CAN_SEE_GUESTS));
                if (mCurrentPhoneItem.guestsCanSeeGuests == null) {
                    mCurrentPhoneItem.guestsCanSeeGuests = "";
                }
            }
            if (mColumnNames.contains(Events.EXRULE)) {
                mCurrentPhoneItem.exrule = mCursor.getString(mCursor.getColumnIndex(Events.EXRULE));
                if (mCurrentPhoneItem.exrule == null) {
                    mCurrentPhoneItem.exrule = "";
                }
            }
            if (mColumnNames.contains(Events.TITLE)) {
                mCurrentPhoneItem.title = mCursor.getString(mCursor.getColumnIndex(Events.TITLE));
                if (mCurrentPhoneItem.title == null) {
                    mCurrentPhoneItem.title = "";
                }
                // dbgTrace("\ntitle:" + mCurrentPhoneItem.title);
            }

            if (mColumnNames.contains(BaseColumns._ID)) {
                mCurrentPhoneItem._id = mCursor.getString(mCursor.getColumnIndex(BaseColumns._ID));
                if (mCurrentPhoneItem._id == null) {
                    mCurrentPhoneItem._id = "";
                }
                // Log.d(tag, "\nbase id which is referred to:"
                // + mCurrentPhoneItem._id);
            }

            //			if (mColumnNames.contains(CalendarContract.Events.DISPLAY_COLOR)) {
            //				mCurrentPhoneItem.displayColor = mCursor.getString(mCursor
            //						.getColumnIndex(CalendarContract.Events.DISPLAY_COLOR));
            //				if (mCurrentPhoneItem.displayColor == null) {
            //					mCurrentPhoneItem.displayColor = "";
            //				}
            //
            //			}
            if (mColumnNames.contains(Events._SYNC_ID)) {
                mCurrentPhoneItem.syncId = mCursor.getString(mCursor.getColumnIndex(Events._SYNC_ID));
                if (mCurrentPhoneItem.syncId == null) {
                    mCurrentPhoneItem.syncId = "";
                }

            }
            if (mColumnNames.contains(Events.ALLOWED_REMINDERS)) {
                mCurrentPhoneItem.allowedReminders = mCursor.getString(mCursor.getColumnIndex(Events.ALLOWED_REMINDERS));
                if (mCurrentPhoneItem.allowedReminders == null) {
                    mCurrentPhoneItem.allowedReminders = "";
                }

            }

            if (isApiEqualOrAbove17()) {
                if (mColumnNames.contains(Events.UID_2445)) {
                    mCurrentPhoneItem.uid2445 = mCursor.getString(mCursor.getColumnIndex(Events.UID_2445));
                    if (mCurrentPhoneItem.uid2445 == null) {
                        mCurrentPhoneItem.uid2445 = "";
                    }

                }
            }

            if (mColumnNames.contains(Events.CAL_SYNC10)) {
                mCurrentPhoneItem.calSync10 = mCursor.getString(mCursor.getColumnIndex(Events.CAL_SYNC10));
                if (mCurrentPhoneItem.calSync10 == null) {
                    mCurrentPhoneItem.calSync10 = "";
                }

            }
            if (mColumnNames.contains(Events.DIRTY)) {
                mCurrentPhoneItem.dirty = mCursor.getString(mCursor.getColumnIndex(Events.DIRTY));
                if (mCurrentPhoneItem.dirty == null) {
                    mCurrentPhoneItem.dirty = "";
                }
                // Log.d(tag, "dirty :" + mCurrentPhoneItem.dirty);
            }
            if (mColumnNames.contains(Events.ALLOWED_ATTENDEE_TYPES)) {
                mCurrentPhoneItem.allowedAttendeeTypes = mCursor.getString(mCursor.getColumnIndex(Events.ALLOWED_ATTENDEE_TYPES));
                if (mCurrentPhoneItem.allowedAttendeeTypes == null) {
                    mCurrentPhoneItem.allowedAttendeeTypes = "";
                }

            }
            if (mColumnNames.contains(Events.AVAILABILITY)) {
                mCurrentPhoneItem.availability = mCursor.getString(mCursor.getColumnIndex(Events.AVAILABILITY));
                if (mCurrentPhoneItem.availability == null) {
                    mCurrentPhoneItem.availability = "";
                }

            }
            if (mColumnNames.contains(Events.CALENDAR_ID)) {
                mCurrentPhoneItem.calendarId = mCursor.getString(mCursor.getColumnIndex(Events.CALENDAR_ID));
                if (mCurrentPhoneItem.calendarId == null) {
                    mCurrentPhoneItem.calendarId = "";
                }
                // dbgTrace("calendar id: " + mCurrentPhoneItem.calendar_id);
            }
            if (mColumnNames.contains(Events.ORIGINAL_ID)) {
                mCurrentPhoneItem.originalId = mCursor.getString(mCursor.getColumnIndex(Events.ORIGINAL_ID));
                if (mCurrentPhoneItem.originalId == null) {
                    mCurrentPhoneItem.originalId = "";
                }
            }
            if (mColumnNames.contains(Events.CUSTOM_APP_URI)) {
                mCurrentPhoneItem.customAppUri = mCursor.getString(mCursor.getColumnIndex(Events.CUSTOM_APP_URI));
                if (mCurrentPhoneItem.customAppUri == null) {
                    mCurrentPhoneItem.customAppUri = "";
                }
            }
            if (mColumnNames.contains(Events.ORIGINAL_ALL_DAY)) {
                mCurrentPhoneItem.originalAllDay = mCursor.getString(mCursor.getColumnIndex(Events.ORIGINAL_ALL_DAY));
                if (mCurrentPhoneItem.originalAllDay == null) {
                    mCurrentPhoneItem.originalAllDay = "";
                }

            }
            if (mColumnNames.contains(Events.MAX_REMINDERS)) {
                mCurrentPhoneItem.maxReminders = mCursor.getString(mCursor.getColumnIndex(Events.MAX_REMINDERS));
                if (mCurrentPhoneItem.maxReminders == null) {
                    mCurrentPhoneItem.maxReminders = "";
                }
            }
            if (mColumnNames.contains(Events.ACCESS_LEVEL)) {
                mCurrentPhoneItem.accessLevel = mCursor.getString(mCursor.getColumnIndex(Events.ACCESS_LEVEL));
                if (mCurrentPhoneItem.accessLevel == null) {
                    mCurrentPhoneItem.accessLevel = "";
                }
            }
            if (mColumnNames.contains(Events.CALENDAR_COLOR)) {
                mCurrentPhoneItem.calendarColor = mCursor.getString(mCursor.getColumnIndex(Events.CALENDAR_COLOR));
                if (mCurrentPhoneItem.calendarColor == null) {
                    mCurrentPhoneItem.calendarColor = "";
                }
                dbgTrace("==>> calcolor: " + mCurrentPhoneItem.calendarColor);
            }
            if (mColumnNames.contains(Events.DURATION)) {
                mCurrentPhoneItem.duration = mCursor.getString(mCursor.getColumnIndex(Events.DURATION));
                if (mCurrentPhoneItem.duration == null) {
                    mCurrentPhoneItem.duration = "";
                } else {
                    // duration is present
                    // duration format can be like P1D=>one day, P12W4D810S=>twelve weeks and four days 810 seconds.
                    // For generic understanding, i am converting to seconds only and filling the IOS fields.
                    String duration = mCurrentPhoneItem.duration;
                    String days = "";
                    String weeks = "";
                    String seconds = "";
                    String tempStr = "";

                    String hours = ""; // currently NA to android, since time set is not allowed when selected to allday.
                    String minutes = ""; // currently NA to android, since time set is not allowed when selected to allday.
                    for (int i = 0; i < duration.length(); i++) {
                        if (i == 0) {
                            // P constant neglect the character.In android there is no prefix with + or - sign.
                        } else {
                            if (duration.charAt(i) == 'D') {
                                days = tempStr;
                                tempStr = "";
                            } else if (duration.charAt(i) == 'W') {
                                weeks = tempStr;
                                tempStr = "";
                            } else if (duration.charAt(i) == 'S') {
                                seconds = tempStr;
                                tempStr = "";
                            } else if (duration.charAt(i) == 'H') {
                                hours = tempStr;
                                tempStr = "";
                            } else if (duration.charAt(i) == 'M') {
                                minutes = tempStr;
                                tempStr = "";
                            } else {
                                tempStr = tempStr + duration.charAt(i);
                            }
                        }
                    }
                    long daysInSec = 0;
                    long weeksInSec = 0;
                    long totalInSec = 0;
                    long hoursInSec = 0;
                    long minutesInSec = 0;
                    if (days.length() > 0) {
                        daysInSec = Integer.parseInt(days) * 86400; // no of days * 1 day in seconds
                        totalInSec = totalInSec + daysInSec;
                    }
                    if (weeks.length() > 0) {
                        weeksInSec = Integer.parseInt(weeks) * 604800; // no of weeks * 1 week in seconds
                        totalInSec = totalInSec + weeksInSec;
                    }
                    if (hours.length() > 0) {
                        hoursInSec = Integer.parseInt(hours) * 3600;
                        totalInSec = totalInSec + hoursInSec;
                    }
                    if (minutes.length() > 0) {
                        minutesInSec = Integer.parseInt(minutes) * 3600;
                        totalInSec = totalInSec + minutesInSec;
                    }
                    if (seconds.length() > 0) {
                        totalInSec = totalInSec + Integer.parseInt(seconds);
                    }
                    totalInSec = totalInSec * 1000; // to convert into millis
                    totalInSec = Long.parseLong(mCurrentPhoneItem.dtstart) + totalInSec;
                    mCurrentPhoneItem.dtend = String.valueOf(totalInSec); // In android dtend for recurrence event is not used, this value is explicitly filled for IOS compatability.
                    dbgTrace("dtend: " + mCurrentPhoneItem.dtend + "\ntotal In sec: " + totalInSec);
                }
            }
            if (mColumnNames.contains(Events.CALENDAR_DISPLAY_NAME)) {
                mCurrentPhoneItem.calendarDisplayName = mCursor.getString(mCursor.getColumnIndex(Events.CALENDAR_DISPLAY_NAME));
                if (mCurrentPhoneItem.calendarDisplayName == null) {
                    mCurrentPhoneItem.calendarDisplayName = "";
                }
            }
            if (mColumnNames.contains(Events.GUESTS_CAN_INVITE_OTHERS)) {
                mCurrentPhoneItem.guestsCanInviteOthers = mCursor.getString(mCursor.getColumnIndex(Events.GUESTS_CAN_INVITE_OTHERS));
                if (mCurrentPhoneItem.guestsCanInviteOthers == null) {
                    mCurrentPhoneItem.guestsCanInviteOthers = "";
                }
            }
            if (mColumnNames.contains(Events.ORIGINAL_SYNC_ID)) {
                mCurrentPhoneItem.originalSyncId = mCursor.getString(mCursor.getColumnIndex(Events.ORIGINAL_SYNC_ID));
                if (mCurrentPhoneItem.originalSyncId == null) {
                    mCurrentPhoneItem.originalSyncId = "";
                }

            }
            if (mColumnNames.contains(Events.EVENT_COLOR)) {
                mCurrentPhoneItem.eventColor = mCursor.getString(mCursor.getColumnIndex(Events.EVENT_COLOR));
                if (mCurrentPhoneItem.eventColor == null) {
                    mCurrentPhoneItem.eventColor = "";
                }

            }
            if (mColumnNames.contains(Events.EXDATE)) {
                mCurrentPhoneItem.exdate = mCursor.getString(mCursor.getColumnIndex(Events.EXDATE));
                if (mCurrentPhoneItem.exdate == null) {
                    mCurrentPhoneItem.exdate = "";
                }

            }

            mCurrentPhoneItem.listofAttendess = getAttendeesViaEventId(mCurrentPhoneItem._id);
            if (mCurrentPhoneItem.listofAttendess == null) {
                return -1;
            }
            mCurrentPhoneItem.listofInstances = getInstancesViaEventId(mCurrentPhoneItem._id);
            if (mCurrentPhoneItem.listofInstances == null) {
                return -1;
            }
            mCurrentPhoneItem.listofReminders = getRemindersViaEventId(mCurrentPhoneItem._id);
            if (mCurrentPhoneItem.listofReminders == null) {
                return -1;
            }

            calendarCursor = mContentResolver.query(Calendars.CONTENT_URI, null, "_id=" + mCurrentPhoneItem.calendarId, null, null);
            if (calendarCursor != null) {
                calendarCursor.moveToFirst();
                if (calendarColumnNames.contains(Calendars.OWNER_ACCOUNT)) {
                    mCurrentPhoneItem.ownerAccount = calendarCursor.getString(calendarCursor.getColumnIndex(Calendars.OWNER_ACCOUNT));
                    // dbgTrace("\n owner account:" +
                    // mCurrentPhoneItem.OWNER_ACCOUNT);
                }

                calendarCursor.close();
            }
            dbgTrace("Event title: " + mCurrentPhoneItem.title + " , calendar name: " + mCurrentPhoneItem.calendarDisplayName);
            long checksum = mCurrentPhoneItem.crc32();
            mCurrentPhoneItem.checksum = (int) checksum;
            dbgTrace("Checksum: " + mCurrentPhoneItem.checksum);
        } catch (Exception e) {
            dbgTrace(GenUtils.getStackTrace(e));
            // Need to assign mCurrentPhoneItem.checksum with some Error number
        }

        mCursor.moveToNext();
        dbgTrace("Ended");
        return mCurrentPhoneItem.checksum;
    }


    @Override
    boolean addMirrorItemToPhone() {
        dbgTrace();
        String calid = "0";
        boolean idPresent = false;

        Uri uri = Calendars.CONTENT_URI;
        Uri returnUri = null;
        ContentResolver cr = mContext.getContentResolver();

        try {
            Cursor cursor = cr.query(uri, null, null, null, null, null);
            ArrayList<String> columnNames = new ArrayList<String>();
            if (cursor != null) {
                cursor.moveToFirst();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }
                cursor.close();
            }

			/*
             * Get the list of calendars present currently in phone,
			 * compare the calendar name of backup content with list,
			 * if the calendar is present in list, onto that
			 * calendar else create a new calendar with the name the
			 * event has to restore and then insert the event into that.
			 *
			 */

            String[] projection = new String[]{BaseColumns._ID, Calendars.NAME, Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE, Calendars.CALENDAR_DISPLAY_NAME};

            Cursor calCursor = mContext.getContentResolver().query(Calendars.CONTENT_URI, projection, Calendars.VISIBLE + " = 1 ", null, BaseColumns._ID + " ASC");

            ArrayList<String> accountNames = new ArrayList<String>();
            ArrayList<String> calendarNames = new ArrayList<String>();
            ArrayList<String> calendarIds = new ArrayList<String>();

            String ACCOUNT_NAME = "private";
            int accCount = 0;

            if (calCursor != null && calCursor.moveToFirst()) {
                accCount = calCursor.getCount();
                for (int i = 0; i < accCount; i++) {

                    accountNames.add(calCursor.getString(calCursor.getColumnIndex(Calendars.ACCOUNT_NAME)));
                    calendarIds.add(calCursor.getString(calCursor.getColumnIndex(Calendars._ID)));
                    calendarNames.add(calCursor.getString(calCursor.getColumnIndex(Calendars.CALENDAR_DISPLAY_NAME)));
                    calCursor.moveToNext();

                    dbgTrace("acc name: " + accountNames.get(i) + "\t id:" + calendarIds.get(i) + "\t calendar disp name:" + calendarNames.get(i));
                }
                // Log.d(tag, "all ids and account names array is obtained");
            }

            idPresent = false;
            if (mAbortFlag) {
                if (calCursor != null) {
                    calCursor.close();
                }
                return false;
            }


            if (calendarNames.contains(mCurrentMirrorItem.calendarDisplayName)) {
                // Calendar Exist
                dbgTrace("Calendar already exist in phone");
                // dbgTrace();
                for (int i = 0; i < accCount; i++) {
                    dbgTrace("Cal name: " + calendarNames.get(i));
                    if (mCurrentMirrorItem.calendarDisplayName.equalsIgnoreCase(calendarNames.get(i))) {
                        dbgTrace("matched");
                        calid = calendarIds.get(i);
                        idPresent = true;
                        // dbgTrace("calendar exists with account name "+mCurrentMirrorItem.ACCOUNT_NAME+"\ncalendar Id: "+calid);
                    }
                }

            } else {

                dbgTrace("Calendar does not exist ,creating new calendar");
                final String INT_NAME_PREFIX = "priv";
                Uri calUri = Calendars.CONTENT_URI.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true").appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME).appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build();

                String dispName = mCurrentMirrorItem.calendarDisplayName;
                String intName = INT_NAME_PREFIX + dispName;
                ContentValues contentValues = new ContentValues();

                if (columnNames.contains(Calendars.ACCOUNT_NAME)) {
                    contentValues.put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
                }
                if (columnNames.contains(Calendars.ACCOUNT_TYPE)) {
                    contentValues.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
                }
                if (columnNames.contains(Calendars.NAME)) {
                    contentValues.put(Calendars.NAME, intName);
                }
                if (columnNames.contains(Calendars.CALENDAR_DISPLAY_NAME)) {
                    contentValues.put(Calendars.CALENDAR_DISPLAY_NAME, mCurrentMirrorItem.calendarDisplayName);
                }
                if (columnNames.contains(Calendars.CALENDAR_COLOR)) {
                    if (!mCurrentMirrorItem.calendarColor.equals("")) {
                        contentValues.put(Calendars.CALENDAR_COLOR, mCurrentMirrorItem.calendarColor);
                    }
                }
                if (columnNames.contains(Calendars.CALENDAR_ACCESS_LEVEL)) {
                    contentValues.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
                }

                if (columnNames.contains(Calendars.OWNER_ACCOUNT)) {
                    contentValues.put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME);
                }
                if (columnNames.contains(Calendars.VISIBLE)) {
                    contentValues.put(Calendars.VISIBLE, 1);
                }
                if (columnNames.contains(Calendars.SYNC_EVENTS)) {
                    contentValues.put(Calendars.SYNC_EVENTS, 1);
                }

                returnUri = cr.insert(calUri, contentValues);
                long eventID = Long.parseLong(returnUri.getLastPathSegment());
                calid = String.valueOf(eventID);
                Log.d(tag, "cal id:" + calid);

                // dbgTrace("calendar name: "+mCurrentMirrorItem.CALENDAR_DISPLAY_NAME+"\tNew calendar id is: "+calid+"\nInserted URI: "+returnUri);
                if (Integer.parseInt(calid) > 0) {
                    idPresent = true;
                }

            }

            if (idPresent) {
                // Restore all events for the calendar
                // dbgTrace("restoring for event calendar display name:"
                // + mCurrentMirrorItem.CALENDAR_DISPLAY_NAME);
                boolean mRes = insertEventToPhoneDb(mContext, calid, mCurrentMirrorItem);
                dbgTrace("Ended");
                if (!mRes) {
                    return false;
                }
            }

            if (calCursor != null) {
                calCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        return true;
    }

    @Override
    boolean addPhoneItemToDatabaseAsMirr() {
        dbgTrace();
        return mCalendarsMirrDb.addToSqlDataBase(mCurrentPhoneItem);
    }

    @Override
    void refreshPhoneDb() {
        // Nothing to do in Calendars

    }

    @Override
    long getNextMirrorItem(int row) {
        mCurrentMirrorItem = this.getMirrCalEventforRow(row);
        dbgTrace("Mirr item checksum: " + mCurrentMirrorItem.checksum);
        return mCurrentMirrorItem.checksum;
    }

    @Override
    long getNextMirrorPlusItem(int row) {
        mCurrentPlusItem = this.getArchiveCalEventforRow(row);
        dbgTrace("Archive item checksum: " + mCurrentPlusItem.checksum);
        return mCurrentPlusItem.checksum;
    }

    @Override
    int[] getDupArrayForCsum(int csum){
        return new int[0];
    }

    private Event getMirrCalEventforRow(int row) {
        dbgTrace("getMirrCalEventforRow");
        Event tempCalEvent = mCalendarsMirrDb.getEventForRow(row);
        return tempCalEvent;
    }

    private Event getArchiveCalEventforRow(int row) {
        dbgTrace("getArchiveCalEventforRow");
        Event tempCalEvent = mCalendarsArchiveDb.getEventForRow(row);
        return tempCalEvent;
    }

    @Override
    boolean deleteitemForCsum(int csum) {
        return mCalendarsMirrDb.deleteCalendarForChecksum(csum);
    }

    @Override
    public long getMirrTotalItemsCount() {
        dbgTrace();
        return mCalendarsMirrDb.getRowCount();
    }


    @Override
    long getMirrPlusTotalItemsCount() {
        dbgTrace();
        return mCalendarsArchiveDb.getRowCount();
    }

    @Override
    boolean addMirrToDataBase() {
        dbgTrace();
        return mCalendarsMirrDb.addToSqlDataBase(mCurrentMirrorItem);
    }

    @Override
    boolean addMirrPlusToDataBase() {
        dbgTrace();
        return mCalendarsArchiveDb.addToSqlDataBase(mCurrentMirrorItem);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("CalendersV2.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("CalendersV2.log", trace);
    }

    /**
     * To check the connected phone API version is equal or above jelly bean
     *
     * @return boolean
     */
    private boolean isApiEqualOrAbove17() {
        // dbgTrace();
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * To get the list of attendees of an event
     *
     * @param eventId
     *
     * @return ArrayList
     */
    private ArrayList<AttendessBean> getAttendeesViaEventId(String eventId) {
        // dbgTrace();
        ArrayList<AttendessBean> listOfAttendessBeans = new ArrayList<AttendessBean>();

        try {
            Cursor cursor = mContentResolver.query(Attendees.CONTENT_URI, null, "event_id=" + eventId, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();

                ArrayList<String> columnNames = new ArrayList<String>();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }

                cursor.moveToFirst();

                int attendeesCount = cursor.getCount();
                for (int i = 0; i < attendeesCount; i++) {
                    if (mAbortFlag) {
                        // Aborting----------
                        cursor.close();
                        return null;
                    }
                    // Attendess
                    AttendessBean attendessBean = new AttendessBean();

                    if (columnNames.contains(Attendees.ATTENDEE_EMAIL)) {
                        attendessBean.attendeeEmail = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_EMAIL));

                        if (attendessBean.attendeeEmail == null) {
                            attendessBean.attendeeEmail = "";
                        }
                    }
                    if (columnNames.contains(Attendees.ATTENDEE_IDENTITY)) {
                        attendessBean.attendeeIdentity = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_IDENTITY));
                        if (attendessBean.attendeeIdentity == null) {
                            attendessBean.attendeeIdentity = "";
                        }
                    }

                    if (columnNames.contains(Attendees.ATTENDEE_ID_NAMESPACE)) {
                        attendessBean.attendeeIdNamespace = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_ID_NAMESPACE));

                        if (attendessBean.attendeeIdNamespace == null) {
                            attendessBean.attendeeIdNamespace = "";
                        }
                    }

                    if (columnNames.contains(Attendees.ATTENDEE_NAME)) {
                        attendessBean.attendeeName = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_NAME));
                        if (attendessBean.attendeeName == null) {
                            attendessBean.attendeeName = "";
                        }
                    }

                    if (columnNames.contains(Attendees.ATTENDEE_RELATIONSHIP)) {
                        attendessBean.attendeeRelationship = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_RELATIONSHIP));
                        if (attendessBean.attendeeRelationship == null) {
                            attendessBean.attendeeRelationship = "";
                        }
                    }

                    if (columnNames.contains(Attendees.ATTENDEE_STATUS)) {
                        attendessBean.attendeeStatus = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_STATUS));
                        if (attendessBean.attendeeStatus == null) {
                            attendessBean.attendeeStatus = "";
                        }
                    }
                    if (columnNames.contains(String.valueOf(Attendees.ATTENDEE_STATUS_ACCEPTED))) {
                        attendessBean.attendeeStatusAccepted = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.ATTENDEE_STATUS_ACCEPTED)));

                    }
                    if (columnNames.contains(String.valueOf(Attendees.ATTENDEE_STATUS_DECLINED))) {
                        attendessBean.attendeeStatusDeclined = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.ATTENDEE_STATUS_DECLINED)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.ATTENDEE_STATUS_INVITED))) {
                        attendessBean.attendeeStatusInvited = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.ATTENDEE_STATUS_INVITED)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.ATTENDEE_STATUS_NONE))) {
                        attendessBean.attendeeStatusNone = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.ATTENDEE_STATUS_NONE)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.ATTENDEE_STATUS_TENTATIVE))) {
                        attendessBean.attendeeStatusTentiative = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.ATTENDEE_STATUS_TENTATIVE)));

                    }

                    if (columnNames.contains(Attendees.ATTENDEE_TYPE)) {
                        attendessBean.attendeeType = cursor.getString(cursor.getColumnIndex(Attendees.ATTENDEE_TYPE));
                        if (attendessBean.attendeeType == null) {
                            attendessBean.attendeeType = "";
                        }
                    }

                    if (columnNames.contains(Attendees.EVENT_ID)) {
                        attendessBean.attendeeEventId = cursor.getString(cursor.getColumnIndex(Attendees.EVENT_ID));
                        if (attendessBean.attendeeEventId == null) {
                            attendessBean.attendeeEventId = "";
                        }
                    }

                    if (columnNames.contains(String.valueOf(Attendees.RELATIONSHIP_ATTENDEE))) {
                        attendessBean.relationshipAttendee = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.RELATIONSHIP_ATTENDEE)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.RELATIONSHIP_NONE))) {
                        attendessBean.relationshipNone = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.RELATIONSHIP_NONE)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.RELATIONSHIP_ORGANIZER))) {
                        attendessBean.relationshipOrganizer = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.RELATIONSHIP_ORGANIZER)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.RELATIONSHIP_PERFORMER))) {
                        attendessBean.relationshipPerformer = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.RELATIONSHIP_PERFORMER)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.RELATIONSHIP_SPEAKER))) {
                        attendessBean.relationshipSpeaker = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.RELATIONSHIP_SPEAKER)));

                    }

                    if (columnNames.contains(String.valueOf(Attendees.TYPE_NONE))) {
                        attendessBean.typeNone = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.TYPE_NONE)));

                    }
                    if (columnNames.contains(String.valueOf(Attendees.TYPE_OPTIONAL))) {
                        attendessBean.typeOptional = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.TYPE_OPTIONAL)));

                    }
                    if (columnNames.contains(String.valueOf(Attendees.TYPE_REQUIRED))) {
                        attendessBean.typeRequired = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.TYPE_REQUIRED)));

                    }
                    if (columnNames.contains(String.valueOf(Attendees.TYPE_RESOURCE))) {
                        attendessBean.typeResource = cursor.getInt(cursor.getColumnIndex(String.valueOf(Attendees.TYPE_RESOURCE)));

                    }
                    // System.out
                    // .println("Attendee content-----------------------\nAttendee_email: "
                    // + attendessBean.ATTENDEE_EMAIL
                    // + "\nATTENDEE_IDENTITY :"
                    // + attendessBean.ATTENDEE_IDENTITY
                    // + "\nATTENDEE_ID_NAMESPACE"
                    // + attendessBean.ATTENDEE_ID_NAMESPACE
                    // + "\nATTENDEE_NAME :"
                    // + attendessBean.ATTENDEE_NAME
                    // + "\nATTENDEE_RELATIONSHIP :"
                    // + attendessBean.ATTENDEE_RELATIONSHIP
                    // + "\nATTENDEE_STATUS :"
                    // + attendessBean.ATTENDEE_STATUS
                    // + "\nATTENDEE_STATUS_ACCEPTED :"
                    // + attendessBean.ATTENDEE_STATUS_ACCEPTED
                    // + "\nATTENDEE_STATUS_DECLINED :"
                    // + attendessBean.ATTENDEE_STATUS_DECLINED
                    // + "\nATTENDEE_STATUS_INVITED :"
                    // + attendessBean.ATTENDEE_STATUS_INVITED
                    // + "\nATTENDEE_STATUS_NONE :"
                    // + attendessBean.ATTENDEE_STATUS_NONE
                    // + "\nATTENDEE_STATUS_TENTATIVE :"
                    // + attendessBean.ATTENDEE_STATUS_TENTATIVE
                    // + "\nEVENT_ID :"
                    // + attendessBean.EVENT_ID
                    // + "\nRELATIONSHIP_ATTENDEE :"
                    // + attendessBean.RELATIONSHIP_ATTENDEE
                    // + "\nRELATIONSHIP_NONE :"
                    // + attendessBean.RELATIONSHIP_NONE
                    // + "\nRELATIONSHIP_ORGANIZER :"
                    // + attendessBean.RELATIONSHIP_ORGANIZER
                    // + "\nRELATIONSHIP_PERFORMER :"
                    // + attendessBean.RELATIONSHIP_PERFORMER
                    // + "\nRELATIONSHIP_SPEAKER :"
                    // + attendessBean.RELATIONSHIP_SPEAKER
                    // + "\nTYPE_NONE"
                    // + attendessBean.TYPE_NONE
                    // + "\nTYPE_OPTIONAL"
                    // + attendessBean.TYPE_OPTIONAL
                    // + "\nTYPE_REQUIRED"
                    // + attendessBean.TYPE_REQUIRED
                    // + "\nTYPE_RESOURCE"
                    // + attendessBean.TYPE_RESOURCE);

                    listOfAttendessBeans.add(attendessBean);
                    cursor.moveToNext();
                }
                // Log.d(tag,
                // "Method:getAttendeesViaEventId :cursor position Ended :"
                // + cursor.getPosition());

                cursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return listOfAttendessBeans;

    }

    /**
     * To get the list of reminders of an event
     *
     * @param eventId
     *
     * @return ArrayList
     */
    private ArrayList<RemindersBean> getRemindersViaEventId(String eventId) {
        // dbgTrace();
        ArrayList<RemindersBean> listOfReminersBean = new ArrayList<RemindersBean>();

        try {
            Cursor cursor = mContentResolver.query(Reminders.CONTENT_URI, null, "event_id=" + eventId, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();

                ArrayList<String> columnNames = new ArrayList<String>();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }

                cursor.moveToFirst();

                int reminderCount = cursor.getCount();
                for (int i = 0; i < reminderCount; i++) {
                    if (mAbortFlag) {
                        // Aborting----------
                        cursor.close();
                        return null;
                    }
                    // Reminders

                    RemindersBean remindersBean = new RemindersBean();
                    if (columnNames.contains(Reminders.EVENT_ID)) {
                        remindersBean.reminderEventId = cursor.getString(cursor.getColumnIndex(Reminders.EVENT_ID));
                        if (remindersBean.reminderEventId == null) {
                            remindersBean.reminderEventId = "";
                        }
                    }
                    if (columnNames.contains(Reminders.METHOD)) {
                        remindersBean.reminderMethod = cursor.getString(cursor.getColumnIndex(Reminders.METHOD));

                        if (remindersBean.reminderMethod == null) {
                            remindersBean.reminderMethod = "";
                        }
                    }
                    if (columnNames.contains(String.valueOf(Reminders.METHOD_ALARM))) {
                        remindersBean.methodAlarm = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.METHOD_ALARM)));

                    }
                    if (columnNames.contains(String.valueOf(Reminders.METHOD_ALERT))) {
                        remindersBean.methodAlert = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.METHOD_ALERT)));

                    }

                    if (columnNames.contains(String.valueOf(Reminders.METHOD_DEFAULT))) {
                        remindersBean.methodDefault = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.METHOD_DEFAULT)));

                    }

                    if (columnNames.contains(String.valueOf(Reminders.METHOD_EMAIL))) {
                        remindersBean.methodEmail = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.METHOD_EMAIL)));

                    }

                    if (columnNames.contains(String.valueOf(Reminders.METHOD_SMS))) {
                        remindersBean.methodSms = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.METHOD_SMS)));

                    }

                    if (columnNames.contains(Reminders.MINUTES)) {
                        remindersBean.reminderMinutes = cursor.getString(cursor.getColumnIndex(Reminders.MINUTES));
                        if (remindersBean.reminderMinutes == null) {
                            remindersBean.reminderMinutes = "";
                        }
                    }

                    if (columnNames.contains(String.valueOf(Reminders.MINUTES_DEFAULT))) {
                        remindersBean.reminderMinutesDefault = cursor.getInt(cursor.getColumnIndex(String.valueOf(Reminders.MINUTES_DEFAULT)));

                    }
                    // System.out
                    // .println("Reminder content:---------------\nEVENT_ID :"
                    // + remindersBean.EVENT_ID + "\nMETHOD :"
                    // + remindersBean.METHOD + "\nMETHOD_ALARM :"
                    // + remindersBean.METHOD_ALARM
                    // + "\nMETHOD_ALERT :"
                    // + remindersBean.METHOD_ALERT
                    // + "\nMETHOD_DEFAULT"
                    // + remindersBean.METHOD_DEFAULT
                    // + "\nMETHOD_EMAIL" + remindersBean.METHOD_EMAIL
                    // + "\nMETHOD_SMS :" + remindersBean.METHOD_SMS
                    // + "\nMINUTES :" + remindersBean.MINUTES
                    // + "\nMINUTES_DEFAULT :"
                    // + remindersBean.MINUTES_DEFAULT);

                    listOfReminersBean.add(remindersBean);
                    cursor.moveToNext();

                }
                // Log.d(tag,
                // "Method:getRemindersViaEventId :cursor position Ended:"
                // + cursor.getPosition());

                cursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return listOfReminersBean;

    }

    /**
     * To get the list of instances of an event
     *
     * @param eventId
     *
     * @return ArrayList
     */
    private ArrayList<InstancesBean> getInstancesViaEventId(String eventId) {
        // dbgTrace();
        ArrayList<InstancesBean> listOfInstancesBean = new ArrayList<InstancesBean>();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();

        long now = new Date().getTime();
        ContentUris.appendId(builder, (now - DateUtils.WEEK_IN_MILLIS));
        ContentUris.appendId(builder, now + DateUtils.WEEK_IN_MILLIS);

        try {
            Cursor cursor = mContentResolver.query(builder.build(), null, "event_id=" + eventId, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();

                ArrayList<String> columnNames = new ArrayList<String>();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }

                cursor.moveToFirst();
                int instanceCount = cursor.getCount();
                for (int i = 0; i < instanceCount; i++) {
                    if (mAbortFlag) {
                        // Aborting----------
                        cursor.close();
                        return null;
                    }
                    // Reminders

                    InstancesBean instancesBean = new InstancesBean();
                    if (columnNames.contains(Instances.BEGIN)) {
                        instancesBean.instanceBegin = cursor.getString(cursor.getColumnIndex(Instances.BEGIN));
                        if (instancesBean.instanceBegin == null) {
                            instancesBean.instanceBegin = "";
                        }
                    }

                    if (columnNames.contains(Instances.END)) {
                        instancesBean.instanceEnd = cursor.getString(cursor.getColumnIndex(Instances.END));

                        if (instancesBean.instanceEnd == null) {
                            instancesBean.instanceEnd = "";
                        }
                    }
                    if (columnNames.contains(Instances.END_DAY)) {
                        instancesBean.instanceEndDay = cursor.getString(cursor.getColumnIndex(Instances.END_DAY));
                        if (instancesBean.instanceEndDay == null) {
                            instancesBean.instanceEndDay = "";
                        }
                    }
                    if (columnNames.contains(Instances.END_MINUTE)) {
                        instancesBean.instanceEndMinute = cursor.getString(cursor.getColumnIndex(Instances.END_MINUTE));
                        if (instancesBean.instanceEndMinute == null) {
                            instancesBean.instanceEndMinute = "";
                        }
                    }
                    if (columnNames.contains(Instances.EVENT_ID)) {
                        instancesBean.instanceEventId = cursor.getString(cursor.getColumnIndex(Instances.EVENT_ID));
                        if (instancesBean.instanceEventId == null) {
                            instancesBean.instanceEventId = "";
                        }
                    }
                    if (columnNames.contains(Instances.START_DAY)) {
                        instancesBean.instanceStartDay = cursor.getString(cursor.getColumnIndex(Instances.START_DAY));
                        if (instancesBean.instanceStartDay == null) {
                            instancesBean.instanceStartDay = "";
                        }
                    }

                    if (columnNames.contains(Instances.START_MINUTE)) {
                        instancesBean.instanceStartMinute = cursor.getString(cursor.getColumnIndex(Instances.START_MINUTE));
                        if (instancesBean.instanceStartMinute == null) {
                            instancesBean.instanceStartMinute = "";
                        }
                    }
                    // dbgTrace("Instance content:---------------\n BEGIN:"
                    // + instancesBean.BEGIN + "\nEND :" + instancesBean.END
                    // + "\nEND_DAY :" + instancesBean.END_DAY
                    // + "\nEND_MINUTE :" + instancesBean.END_MINUTE
                    // + "\nEVENT_ID :" + instancesBean.EVENT_ID
                    // + "\nSTART_DAY :" + instancesBean.START_DAY
                    // + "\nSTART_MINUTE :" + instancesBean.START_MINUTE);

                    listOfInstancesBean.add(instancesBean);
                    cursor.moveToNext();

                }
                // Log.d(tag,
                // "Method:getInstancesViaEventId :cursor position Ended:"
                // + cursor.getPosition());

                cursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return listOfInstancesBean;

    }

    /**
     * To insert list of reminders of an event into phone DB
     *
     * @param eventObj
     * @param eventUri
     *
     * @return boolean
     */
    private boolean restoreReminders(Event eventObj, Uri eventUri) {
        // dbgTrace();
        Uri REMINDERS_URI = Reminders.CONTENT_URI;
        try {
            Cursor cursor = mContentResolver.query(REMINDERS_URI, null, "event_id=" + Long.parseLong(eventUri.getLastPathSegment()), null, null, null);
            ArrayList<String> columnNames = new ArrayList<String>();
            if (cursor != null) {
                cursor.moveToFirst();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }
                cursor.close();
            }

            int remindersCount = eventObj.listofReminders.size();
            for (int i = 0; i < remindersCount; i++) {
                if (mAbortFlag) {
                    return false;
                }
                ContentValues values = new ContentValues();
                RemindersBean reminderObj = eventObj.listofReminders.get(i);
                if (columnNames.contains(Reminders.EVENT_ID)) {
                    if (!(reminderObj.reminderEventId.equalsIgnoreCase(""))) {
                        values.put(Reminders.EVENT_ID, Long.parseLong(eventUri.getLastPathSegment()));
                    }
                }
                if (columnNames.contains(Reminders.METHOD)) {
                    if (!(reminderObj.reminderMethod.equalsIgnoreCase(""))) {
                        values.put(Reminders.METHOD, reminderObj.reminderMethod);
                    }
                }
                if (columnNames.contains(Reminders.MINUTES)) {
                    if (!(reminderObj.reminderMinutes.equalsIgnoreCase(""))) {
                        values.put(Reminders.MINUTES, reminderObj.reminderMinutes);
                    }
                }
                // @SuppressWarnings("unused")
                Uri reminderUri = mContext.getContentResolver().insert(REMINDERS_URI, values);
                Log.d(tag, "Inserted reminder of uri: " + reminderUri);
                values.clear();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return true;

    }

    /**
     * To insert list of attendees of an event into phone DB
     *
     * @param eventObj
     * @param eventUri
     *
     * @return boolean
     */
    private boolean restoreAttendees(Event eventObj, Uri eventUri) {
        // dbgTrace();
        Uri ATTENDEES_URI = Attendees.CONTENT_URI;
        try {
            Cursor cursor = mContentResolver.query(ATTENDEES_URI, null, "event_id=" + Long.parseLong(eventUri.getLastPathSegment()), null, null, null);
            ArrayList<String> columnNames = new ArrayList<String>();
            if (cursor != null) {
                cursor.moveToFirst();
                for (String colName : cursor.getColumnNames()) {
                    columnNames.add(colName);
                }
                cursor.close();
            }

            int attendeesCount = eventObj.listofAttendess.size();
            for (int i = 0; i < attendeesCount; i++) {
                if (mAbortFlag) {
                    return false;
                }
                ContentValues values = new ContentValues();
                AttendessBean attendeeObj = eventObj.listofAttendess.get(i);

                if (columnNames.contains(Attendees.ATTENDEE_EMAIL)) {
                    if (!(attendeeObj.attendeeEmail.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_EMAIL, attendeeObj.attendeeEmail);
                    }
                }

                if (columnNames.contains(Attendees.ATTENDEE_IDENTITY)) {
                    if (!(attendeeObj.attendeeIdentity.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_IDENTITY, attendeeObj.attendeeIdentity);
                    }
                }

                if (columnNames.contains(Attendees.ATTENDEE_ID_NAMESPACE)) {
                    if (!(attendeeObj.attendeeIdNamespace.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_ID_NAMESPACE, attendeeObj.attendeeIdNamespace);
                    }
                }

                if (columnNames.contains(Attendees.ATTENDEE_RELATIONSHIP)) {
                    if (!(attendeeObj.attendeeRelationship.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_RELATIONSHIP, attendeeObj.attendeeRelationship);
                    }
                }

                if (columnNames.contains(Attendees.ATTENDEE_STATUS)) {
                    if (!(attendeeObj.attendeeStatus.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_STATUS, attendeeObj.attendeeStatus);
                    }
                }

                if (columnNames.contains(Attendees.ATTENDEE_TYPE)) {
                    if (!(attendeeObj.attendeeType.equalsIgnoreCase(""))) {
                        values.put(Attendees.ATTENDEE_TYPE, attendeeObj.attendeeType);
                    }
                }

                if (columnNames.contains(Attendees.EVENT_ID)) {
                    if (!(attendeeObj.attendeeEventId.equalsIgnoreCase(""))) {
                        values.put(Attendees.EVENT_ID, Long.parseLong(eventUri.getLastPathSegment()));
                    }
                }

                // @SuppressWarnings("unused")
                Uri attendeesUri = mContext.getContentResolver().insert(ATTENDEES_URI, values);
                Log.d(tag, "Inserted attendee of uri: " + attendeesUri);
                values.clear();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return true;
    }

    /**
     * To insert an Event to Phone database
     *
     * @param context
     * @param calendarid
     * @param eventObj
     *
     * @return boolean
     */
    @SuppressLint("InlinedApi")
    private boolean insertEventToPhoneDb(Context context, String calendarid, Event eventObj) {
        dbgTrace();
        if (mAbortFlag) {
            return false;
        }
        Event evbean = eventObj;

        ContentValues values = new ContentValues();

        if (mColumnNames.contains(Events.CALENDAR_ID)) {
            if (!(calendarid.equalsIgnoreCase(""))) {
                values.put(Events.CALENDAR_ID, calendarid);
            }
        }
        if (mColumnNames.contains(Events.TITLE)) {
            if (!(evbean.title.equalsIgnoreCase(""))) {
                values.put(Events.TITLE, evbean.title);
            }
        }
        if (mColumnNames.contains(Events.DTSTART)) {
            if (!(evbean.dtstart.equalsIgnoreCase(""))) {
                values.put(Events.DTSTART, evbean.dtstart);
            }
        }

        // TimeZone is taken as phones default timezone
        values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        dbgTrace("\nevent timezone " + evbean.eventTimezone);
        dbgTrace("\nrrule  " + evbean.rrule);

        // For non-recurring events
        if ((evbean.rrule.equalsIgnoreCase(""))) {
            if (mColumnNames.contains(Events.DTEND)) {
                if (!(evbean.dtend.equalsIgnoreCase(""))) {
                    values.put(Events.DTEND, evbean.dtend);
                }
            }
        } else {
            // For recurring events
            if (mColumnNames.contains(Events.DURATION)) {
                if (!(evbean.duration.equalsIgnoreCase(""))) {
                    values.put(Events.DURATION, evbean.duration);
                    dbgTrace("\nevent duration :" + evbean.duration);
                } else {
                    dbgTrace("\nWarning: event duration for recurrence event is empty, restore fails");
                }
            }
            if (mColumnNames.contains(Events.RRULE)) {
                if (!(evbean.rrule.equalsIgnoreCase(""))) {
                    values.put(Events.RRULE, evbean.rrule);
                    dbgTrace("\nevent rrule " + evbean.rrule);
                }
            }
            if (mColumnNames.contains(Events.RDATE)) {
                if (!(evbean.rdate.equalsIgnoreCase(""))) {
                    values.put(Events.RDATE, evbean.rdate);
                }
            }

        }
        dbgTrace("Events.EVENT_LOCATION");

        if (mColumnNames.contains(Events.EVENT_LOCATION)) {
            if (!(evbean.eventLocation.equalsIgnoreCase(""))) {
                values.put(Events.EVENT_LOCATION, evbean.eventLocation);
            }
        }

        dbgTrace("Events.EVENT_COLOR");

        if (mColumnNames.contains(Events.EVENT_COLOR)) {
            if (!(evbean.eventColor.equalsIgnoreCase(""))) {
                values.put(Events.EVENT_COLOR, evbean.eventColor);
            }
        }

        dbgTrace("Events.EVENT_END_TIMEZONE");

        if (mColumnNames.contains(Events.EVENT_END_TIMEZONE)) {
            if (!(evbean.eventEndTimezone.equalsIgnoreCase(""))) {
                values.put(Events.EVENT_END_TIMEZONE, evbean.eventEndTimezone);
            }
        }

        dbgTrace("Events.DESCRIPTION");


        if (mColumnNames.contains(Events.DESCRIPTION)) {
            if (!(evbean.description.equalsIgnoreCase(""))) {
                values.put(Events.DESCRIPTION, evbean.description);
            }
        }
        dbgTrace("Event description: " + evbean.description);
        if (mColumnNames.contains(Events.ALL_DAY)) {
            if (!(evbean.allDay.equalsIgnoreCase(""))) {
                values.put(Events.ALL_DAY, Integer.parseInt(evbean.allDay));
                //values.put(Events.ALL_DAY, 0);
            }
        }
        dbgTrace("allday : " + evbean.allDay);

        // dont assign this organizer, is you insert so, the event cannot be editable
        //		if (mColumnNames.contains(CalendarContract.Events.ORGANIZER)) {
        //			if (!(evbean.organizer.equalsIgnoreCase(""))) {
        //				values.put(Events.ORGANIZER, evbean.organizer);
        //			}
        //		}
        //		dbgTrace("\norganizer "+ evbean.organizer);

        if (mColumnNames.contains(Events.AVAILABILITY)) {
            if (!(evbean.availability.equalsIgnoreCase(""))) {
                values.put(Events.AVAILABILITY, evbean.availability);
            }
        }
        dbgTrace("Event availablity: " + evbean.availability);

        if (mColumnNames.contains(Events.EXRULE)) {
            if (!(evbean.exrule.equalsIgnoreCase(""))) {
                values.put(Events.EXRULE, evbean.exrule);
            }
        }
        dbgTrace("Exrule: " + evbean.exrule);

        if (mColumnNames.contains(Events.EXDATE)) {
            if (!(evbean.exdate.equalsIgnoreCase(""))) {
                values.put(Events.EXDATE, evbean.exdate);
            }
        }
        dbgTrace("Exdate: " + evbean.exdate);

        if (mColumnNames.contains(Events.ORIGINAL_ID)) {
            if (!(evbean.originalId.equalsIgnoreCase(""))) {
                values.put(Events.ORIGINAL_ID, evbean.originalId);
            }
        }
        dbgTrace("\nEvent original id: " + evbean.originalId);

        if (mColumnNames.contains(Events.ORIGINAL_SYNC_ID)) {
            if (!(evbean.originalSyncId.equalsIgnoreCase(""))) {
                values.put(Events.ORIGINAL_SYNC_ID, evbean.originalSyncId);
            }
        }
        dbgTrace("\noriginal sync id: " + evbean.originalSyncId);

        if (mColumnNames.contains(Events.ORIGINAL_INSTANCE_TIME)) {
            if (!(evbean.originalInstanceTime.equalsIgnoreCase(""))) {
                values.put(Events.ORIGINAL_INSTANCE_TIME, evbean.originalInstanceTime);
            }
        }
        dbgTrace("\noriginal instance time: " + evbean.originalInstanceTime);

        if (mColumnNames.contains(Events.ORIGINAL_ALL_DAY)) {
            if (!(evbean.originalAllDay.equalsIgnoreCase(""))) {
                values.put(Events.ORIGINAL_ALL_DAY, evbean.originalAllDay);
            }
        }
        dbgTrace("original all day: " + evbean.originalAllDay);

        if (mColumnNames.contains(Events.ACCESS_LEVEL)) {
            if (!(evbean.accessLevel.equalsIgnoreCase(""))) {
                values.put(Events.ACCESS_LEVEL, "3");
                Log.d(tag, "access level: " + evbean.accessLevel);
            }
        }
        dbgTrace("access level: " + evbean.accessLevel);

        if (mColumnNames.contains(Events.CUSTOM_APP_PACKAGE)) {
            if (!(evbean.customAppPackage.equalsIgnoreCase(""))) {
                values.put(Events.CUSTOM_APP_PACKAGE, evbean.customAppPackage);
            }
        }

        if (mColumnNames.contains(Events.CUSTOM_APP_URI)) {
            if (!(evbean.customAppUri.equalsIgnoreCase(""))) {
                values.put(Events.CUSTOM_APP_URI, evbean.customAppUri);
            }
        }

        if (isApiEqualOrAbove17()) {
            if (mColumnNames.contains(Events.UID_2445)) {
                if (!(evbean.uid2445.equalsIgnoreCase(""))) {
                    values.put(Events.UID_2445, evbean.uid2445);
                }
            }
        }

        if (mColumnNames.contains(Events.GUESTS_CAN_MODIFY)) {
            if (!(evbean.guestsCanModify.equalsIgnoreCase(""))) {
                values.put(Events.GUESTS_CAN_MODIFY, Integer.parseInt(evbean.guestsCanModify));
            }
        }
        dbgTrace("\nguests can modify " + evbean.guestsCanModify);

        if (mColumnNames.contains(Events.GUESTS_CAN_INVITE_OTHERS)) {
            if (!(evbean.guestsCanInviteOthers.equalsIgnoreCase(""))) {
                values.put(Events.GUESTS_CAN_INVITE_OTHERS, Integer.parseInt(evbean.guestsCanInviteOthers));
            }
        }
        dbgTrace("\nguests can invite others " + evbean.guestsCanInviteOthers);

        if (mColumnNames.contains(Events.GUESTS_CAN_SEE_GUESTS)) {
            if (!(evbean.guestsCanSeeGuests.equalsIgnoreCase(""))) {
                values.put(Events.GUESTS_CAN_SEE_GUESTS, Integer.parseInt(evbean.guestsCanSeeGuests));
            }
        }
        dbgTrace("\nguests can see guests " + evbean.guestsCanSeeGuests);

        try {
            // dbgTrace("Method:restoreEvents Inserted " + evbean.title);
            Uri eventsUri = context.getContentResolver().insert(Events.CONTENT_URI, values);
            // dbgTrace("Method:restoreEvents Inserted " + evbean.title
            // + " return Uri value: " +
            // eventsUri+"\ndisplay name of cal:"+evbean.CALENDAR_DISPLAY_NAME
            // );
            dbgTrace("Insert event: " + eventsUri);
            boolean mRemiderRes = restoreReminders(evbean, eventsUri);
            if (!mRemiderRes) {
                return false;
            }
            boolean mAttendeeRes = restoreAttendees(evbean, eventsUri);
            if (!mAttendeeRes) {
                return false;
            }
        } catch (Exception e) {
            dbgTrace("Method:restoreEvents: Exception: " + e.getMessage());
        }
        // dbgTrace("Ended");
        return true;

    }

    // Internal methods
    public static class InstancesBean {
        public String instanceBegin = "";
        public String instanceEnd = "";
        public String instanceEndDay = "";
        public String instanceEndMinute = "";
        public String instanceEventId = "";
        public String instanceStartDay = "";
        public String instanceStartMinute = "";
    }

    public static class RemindersBean {
        public String reminderEventId = "";
        public String reminderMethod = "";
        public int methodAlarm;
        public int methodAlert;
        public int methodDefault;
        public int methodEmail;
        public int methodSms;
        public String reminderMinutes = "";
        public int reminderMinutesDefault;
        public String absoluteDate = ""; // added due to iphone compatability
    }

    public static class AttendessBean {
        public String attendeeEmail = "";
        public String attendeeIdentity = "";
        public String attendeeIdNamespace = "";
        public String attendeeName = "";
        public String attendeeRelationship = "";
        public String attendeeStatus = "";
        public int attendeeStatusAccepted;
        public int attendeeStatusDeclined;
        public int attendeeStatusInvited;
        public int attendeeStatusNone;
        public int attendeeStatusTentiative;
        public String attendeeType = "";
        public String attendeeEventId = "";
        public int relationshipAttendee;
        public int relationshipNone;
        public int relationshipOrganizer;
        public int relationshipPerformer;
        public int relationshipSpeaker;
        public int typeNone;
        public int typeOptional;
        public int typeRequired;
        public int typeResource;
    }

    public static class DaysOfTheWeek {
        int dayOfTheWeek = 0;
        int weekNumber = 0;
    }

    public static class Rrobj {
        public String interval = "";
        public ArrayList<DaysOfTheWeek> daysOfTheWeek = new ArrayList<DaysOfTheWeek>();
        public ArrayList<Integer> daysOfTheMonth = new ArrayList<Integer>(); // Integer or string ????????????????
        public ArrayList<Integer> daysOfTheYear = new ArrayList<Integer>();
        public ArrayList<Integer> weeksOfTheYear = new ArrayList<Integer>();
        public ArrayList<Integer> monthsOfTheYear = new ArrayList<Integer>();
        public ArrayList<Integer> setPositions = new ArrayList<Integer>();
        public String frequency = "";
        public String rrend = "";
        public int occurenceCount = 0;
    }

    public static class CalColor {
        public String alpha = "";
        public String blue = "";
        public String green = "";
        public String red = "";

    }

    // Donot make this class with @JsonInclude(Include.NON_DEFAULT),This class
    // has a minAPIlevel info
    public static class Event {
        public String rrule = "";
        public String hasAlarm = "";
        public String rdate = "";
        public String dtstart = "";
        public String calendarTimezone = ""; // ?? fields not filled
        public String description = "";
        public String dtend = "";
        public String allDay = "";
        public String eventLocation = "";
        public String eventTimezone = "";
        public String lastDate = "";
        public String exrule = "";
        public String title = "";
        //public String displayColor = "";
        public String calendarColor = "";
        public String duration = "";
        public String calendarDisplayName = "";
        public String eventColor = "";
        public String exdate = "";
        public String eventStatus = "";
        public String eventEndTimezone = "";
        public String guestsCanInviteOthers = "";
        public String canModifyTimeZone = "";
        public String guestsCanModify = "";
        public String guestsCanSeeGuests = "";

        public String ownerAccount = "";
        public int minApiLevel = 16;
        public boolean isApiLevel17 = false;
        public String isOrganizer = "";
        public String calSync9 = "";
        public String calSync8 = "";
        public String calSync7 = "";
        public String visible = "";
        public String calSync6 = "";
        public String calSync5 = "";
        public String calSync4 = "";
        public String calSync3 = "";
        public String calSync2 = "";
        public String calSync1 = "";
        public String calendarColorIndex = "";
        public String syncData1 = "";
        public String syncData2 = "";
        public String hasAttendeeData = "";
        public String syncData3 = "";
        public String syncData4 = "";
        public String syncData5 = "";
        public String syncData6 = "";
        public String calendarAccessLevel = "";
        public String syncData7 = "";
        public String syncData8 = "";
        public String syncData9 = "";
        public String customAppPackage = "";
        public String accountType = "";
        public String hasExtendedProperties = "";
        public String syncData10 = "";
        public String organizer = "";
        public String lastSynced = "";

        public String deleted = "";

        public String accountName = "";
        public String originalInstanceTime = "";
        public String selfAttendeeStatus = "";
        public String eventColorIndex = "";
        public String allowedAvailability = "";
        public String canOrganizerRespond = "";
        public String _id = "";
        public String syncId = "";
        public String allowedReminders = "";
        public String uid2445 = "";
        public String calSync10 = "";
        public String dirty = "";
        public String allowedAttendeeTypes = "";
        public String availability = "";
        public String calendarId = "";
        public String originalId = "";
        public String customAppUri = "";
        public String originalAllDay = "";
        public String maxReminders = "";
        public String accessLevel = "";
        public String originalSyncId = "";

        public ArrayList<AttendessBean> listofAttendess = new ArrayList<AttendessBean>();
        public ArrayList<RemindersBean> listofReminders = new ArrayList<RemindersBean>();
        public ArrayList<InstancesBean> listofInstances = new ArrayList<InstancesBean>();

        // following fields are added due to IOS compatibility
        public ArrayList<Rrobj> rrObjs = new ArrayList<Rrobj>();
        public String supEventAvailabilities = "";
        public String calId = "";
        public ArrayList<CalColor> calColor = new ArrayList<CalColor>();
        public String calItemId = "";
        public String calItemExternalId = "";
        public String source = "";
        public String subscribed = "";
        public String eventId = "";
        public String iAvailability = "";
        public String isDetached = "";
        public String status = "";
        public String birthdayPersonId = "";
        public String creationDate = "";
        public String lastModifiedDate = "";
        public String url = "";
        public String hasNotes = "";
        public String hasRrule = "";
        public String isContentModifiable = "";
        public String isImmutable = "";
        public String type = "";
        public String allowedEntityTypes = "";

        // this is crucial for logic. see the documentation.
        public int checksum;

        public long crc32() {
            if (this._id == null) {
                return 0;
            }
            CRC32 checksum = new CRC32();

            checksum.update(this.calendarDisplayName.getBytes());
            checksum.update(this.dtstart.getBytes());
            checksum.update(this.dtend.getBytes());
            checksum.update(this.title.getBytes());
            checksum.update(this.eventLocation.getBytes());
            checksum.update(this.description.getBytes());
            if (this.hasRrule.equals("YES")) {
                checksum.update(this.rrule.getBytes());
            }
            /*System.out.println("calendar name: " + calendarDisplayName + "\ndtstart: " + dtstart + "\ndtend: " + dtend
                    + "\ntitle: " + title + "\nlocation: " + eventLocation + "\ndescription: " + description + "\nrrule: "
					+ rrule);*/

            return checksum.getValue();
        }

    }


}
