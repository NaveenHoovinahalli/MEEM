package com.meem.v2.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.meem.phone.MeemSqlDBManager;
import com.meem.utils.GenUtils;

import java.util.ArrayList;

/**
 * Created by karthik B S on 3/4/17.
 */
public class CalendarsDbV2 extends MeemSqlDBManager {
    private String tag = "CalendarDbV2";
    private CalendersV2.Event mCurrentItem;


    public CalendarsDbV2(Context mContext, String dbFilepath, ArrayList<String> tbNamesArrList) {
        super(mContext, dbFilepath, tbNamesArrList);
        dbgTrace();
    }


    // individual retore related contructor
    public CalendarsDbV2(Context mContext, String dbFilepath) {
        super(mContext, dbFilepath);
        dbgTrace();
    }


    private void dbgTrace() {
        GenUtils.logMethodToFile("CalendarSqlDbV2.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("CalendarSqlDbV2.log", trace);
    }

    public boolean addToSqlDataBase(CalendersV2.Event currItem) {
        dbgTrace();
        mCurrentItem = currItem;
        boolean ret = true;

        if (isCalendarForCSM(mCurrentItem.checksum))
            return true;

        ContentValues values = new ContentValues();
        values.put("checksum", (int) mCurrentItem.checksum);
        String item = mCurrentItem.dtstart;
        if (0 != item.length()) {
            values.put("dtstart", item);
        }

        if (!add(values, CalendarTableNamesV2.VAULT_CALENDAR_TABLE)) {
            dbgTrace("Inserting to vault_calendars_table Failed");
            return false;
        } else {
            int linkcount;
            linkcount = getLinkCountforCsum(mCurrentItem.checksum);
            dbgTrace("link count " + linkcount);


            if (linkcount == 0) {
                if (addnewLink(mCurrentItem.checksum)) {

                    if (!addCalendar_Event()) {
                        dbgTrace("Adding Calendar_event table Failed");
                        ret = false;
                    }

                    if (!addCal_Color()) {
                        dbgTrace("Adding Cal_color table Failed");
                        ret = false;
                    }

                    if (!addList_of_attendees()) {
                        dbgTrace("Adding List_of_attendees table Failed");
                        ret = false;
                    }

                    if (!addList_of_instances()) {
                        dbgTrace("Adding List_of_instances table Failed");
                        ret = false;
                    }

                    if (!addReminders_list()) {
                        dbgTrace("Adding Reminders_list table Failed");
                        ret = false;
                    }

                    if (!addRRule_list()) {
                        dbgTrace("Adding RRule_list table Failed");
                        ret = false;
                    }


                } else {
                    dbgTrace("adding new link to Links table failed ");
                    return true;
                }
            } else if (linkcount > 0) {
                        /* if links count is less more than  */
                if (!addLinkscount(mCurrentItem.checksum)) dbgTrace(" Adding addLinkscount failed  ");
            }


        }

        return ret;
    }

    private int getLinkCountforCsum(int checkSum) {
        dbgTrace();

        String querry = "select * from " + CalendarTableNamesV2.LINKS_COUNT_TABLE + " where checksum = " + checkSum;
        return rawQuerryGetInt(querry, 1);
    }

    private boolean decrementLinkscountForChecksum(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + CalendarTableNamesV2.LINKS_COUNT_TABLE +
                " set linkscount = linkscount - 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean addLinkscount(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + CalendarTableNamesV2.LINKS_COUNT_TABLE +
                " set linkscount = linkscount + 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean addnewLink(int checkSum) {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("checksum", (int) checkSum);
        values.put("linkscount", (int) 1);

        return add(values, CalendarTableNamesV2.LINKS_COUNT_TABLE);
    }

    public boolean deleteAll() {
        boolean result = true;


        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + CalendarTableNamesV2.LINKS_COUNT_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.VAULT_CALENDAR_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.REMINDERS_LIST_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.CAL_COLOR_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.RRULE_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFWEEK_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFMONTH_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFYEAR_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.WEEKOFYEAR_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.MONTHSOFYEAR_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.SETPOSITIONS_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.LIST_OF_ATTENDEES_TABLE);
            db.execSQL("delete from " + CalendarTableNamesV2.LIST_OF_INSTANCES_TABLE);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }
        return result;

    }

    private boolean addCalendar_Event() {
        dbgTrace();
        ContentValues values = new ContentValues();

        values.put("checksum", (int) mCurrentItem.checksum);
        String item = mCurrentItem.hasAlarm;
        if (0 != item.length()) {
            values.put("has_alarm", item);
        }
        item = mCurrentItem.eventTimezone;
        if (0 != item.length()) {
            values.put("event_timezone", item);
        }
        item = mCurrentItem.description;
        if (0 != item.length()) {
            values.put("description", item);
        }
        item = mCurrentItem.eventLocation;
        if (0 != item.length()) {
            values.put("event_location", item);
        }
        item = mCurrentItem.dtstart;
        if (0 != item.length()) {
            values.put("dtstart", item);
        }
        item = mCurrentItem.dtend;
        if (0 != item.length()) {
            values.put("dtend", item);
        }
        item = mCurrentItem.allDay;
        if (0 != item.length()) {
            values.put("allday", item);
        }
        item = mCurrentItem.title;
        if (0 != item.length()) {
            values.put("title", item);
        }
        item = mCurrentItem.calendarDisplayName;
        if (0 != item.length()) {
            values.put("calendar_displayname", item);
        }
        item = mCurrentItem.duration;
        if (0 != item.length()) {
            values.put("duration", item);
        }
        item = mCurrentItem.supEventAvailabilities;
        if (0 != item.length()) {
            values.put("sup_event_availabilities", item);
        }
        item = mCurrentItem.calId;
        if (0 != item.length()) {
            values.put("cal_id", item);
        }
        item = mCurrentItem.calItemId;
        if (0 != item.length()) {
            values.put("cal_itemid", item);
        }
        item = mCurrentItem.calItemExternalId;
        if (0 != item.length()) {
            values.put("cal_item_externalid", item);
        }
        item = mCurrentItem.source;
        if (0 != item.length()) {
            values.put("source", item);
        }
        item = mCurrentItem.subscribed;
        if (0 != item.length()) {
            values.put("subscribed", item);
        }
        item = mCurrentItem.eventId;
        if (0 != item.length()) {
            values.put("eventid", item);
        }
        item = mCurrentItem.availability;
        if (0 != item.length()) {
            values.put("availability", item);
        }
        item = mCurrentItem.isDetached;
        if (0 != item.length()) {
            values.put("isdetached", item);
        }
        item = mCurrentItem.status;
        if (0 != item.length()) {
            values.put("status", item);
        }
        item = mCurrentItem.birthdayPersonId;
        if (0 != item.length()) {
            values.put("birthday_personid", item);
        }
        item = mCurrentItem.creationDate;
        if (0 != item.length()) {
            values.put("creationdate", item);
        }
        item = mCurrentItem.lastModifiedDate;
        if (0 != item.length()) {
            values.put("last_modifieddate", item);
        }
        item = mCurrentItem.url;
        if (0 != item.length()) {
            values.put("url", item);
        }
        item = mCurrentItem.hasNotes;
        if (0 != item.length()) {
            values.put("hasnotes", item);
        }
        item = mCurrentItem.hasRrule;
        if (0 != item.length()) {
            values.put("has_rrule", item);
        }
        item = mCurrentItem.isContentModifiable;
        if (0 != item.length()) {
            values.put("iscontent_modifiable", item);
        }
        item = mCurrentItem.isImmutable;
        if (0 != item.length()) {
            values.put("isimmutable", item);
        }
        item = mCurrentItem.type;
        if (0 != item.length()) {
            values.put("type", item);
        }
        item = mCurrentItem.allowedEntityTypes;
        if (0 != item.length()) {
            values.put("allowed_entitytypes", item);
        }
        item = mCurrentItem.rrule;
        if (0 != item.length()) {
            values.put("rrule", item);
        }
        item = mCurrentItem.calendarColor;
        if (0 != item.length()) {
            values.put("calender_color", item);
        }
        item = mCurrentItem.rdate;
        if (0 != item.length()) {
            values.put("rdate", item);
        }
        item = mCurrentItem.eventColor;
        if (0 != item.length()) {
            values.put("event_color", item);
        }
        item = mCurrentItem.eventEndTimezone;
        if (0 != item.length()) {
            values.put("event_end_timezone", item);
        }
        item = mCurrentItem.exrule;
        if (0 != item.length()) {
            values.put("exrule", item);
        }
        item = mCurrentItem.exdate;
        if (0 != item.length()) {
            values.put("exdate", item);
        }
        item = mCurrentItem.originalId;
        if (0 != item.length()) {
            values.put("original_id", item);
        }
        item = mCurrentItem.originalSyncId;
        if (0 != item.length()) {
            values.put("original_sync_id", item);
        }
        item = mCurrentItem.originalInstanceTime;
        if (0 != item.length()) {
            values.put("original_instance_time", item);
        }
        item = mCurrentItem.originalAllDay;
        if (0 != item.length()) {
            values.put("original_allday", item);
        }
        item = mCurrentItem.accessLevel;
        if (0 != item.length()) {
            values.put("access_level", item);
        }
        item = mCurrentItem.customAppPackage;
        if (0 != item.length()) {
            values.put("customapp_package", item);
        }
        item = mCurrentItem.customAppUri;
        if (0 != item.length()) {
            values.put("customapp_uri", item);
        }
        item = mCurrentItem.uid2445;
        if (0 != item.length()) {
            values.put("uid_2445", item);
        }
        item = mCurrentItem.guestsCanModify;
        if (0 != item.length()) {
            values.put("guestcan_modify", item);
        }
        item = mCurrentItem.guestsCanInviteOthers;
        if (0 != item.length()) {
            values.put("guestcan_inviteothers", item);
        }
        item = mCurrentItem.guestsCanSeeGuests;
        if (0 != item.length()) {
            values.put("guestcan_see_guests", item);
        }


        item = mCurrentItem._id;
        if (0 != item.length()) {
            values.put("id", item);
        }
        item = mCurrentItem.accountName;
        if (0 != item.length()) {
            values.put("account_name", item);
        }
        item = mCurrentItem.accountType;
        if (0 != item.length()) {
            values.put("account_type", item);
        }
        item = mCurrentItem.allowedAttendeeTypes;
        if (0 != item.length()) {
            values.put("allowed_attendee_types", item);
        }
        item = mCurrentItem.allowedAvailability;
        if (0 != item.length()) {
            values.put("allowed_availability", item);
        }
        item = mCurrentItem.allowedReminders;
        if (0 != item.length()) {
            values.put("allowed_reminders", item);
        }
        item = mCurrentItem.availability;
        if (0 != item.length()) {
            values.put("availability", item);
        }


        item = mCurrentItem.calSync1;
        if (0 != item.length()) {
            values.put("cal_sync1", item);
        }
        item = mCurrentItem.calSync2;
        if (0 != item.length()) {
            values.put("cal_sync2", item);
        }
        item = mCurrentItem.calSync3;
        if (0 != item.length()) {
            values.put("cal_sync3", item);
        }
        item = mCurrentItem.calSync4;
        if (0 != item.length()) {
            values.put("cal_sync4", item);
        }
        item = mCurrentItem.calSync5;
        if (0 != item.length()) {
            values.put("cal_sync5", item);
        }
        item = mCurrentItem.calSync6;
        if (0 != item.length()) {
            values.put("cal_sync6", item);
        }
        item = mCurrentItem.calSync7;
        if (0 != item.length()) {
            values.put("cal_sync7", item);
        }
        item = mCurrentItem.calSync8;
        if (0 != item.length()) {
            values.put("cal_sync8", item);
        }
        item = mCurrentItem.calSync9;
        if (0 != item.length()) {
            values.put("cal_sync9", item);
        }
        item = mCurrentItem.calSync10;
        if (0 != item.length()) {
            values.put("cal_sync10", item);
        }


        item = mCurrentItem.calendarAccessLevel;
        if (0 != item.length()) {
            values.put("calendar_access_level", item);
        }
        item = mCurrentItem.calendarColorIndex;
        if (0 != item.length()) {
            values.put("calendar_color_index", item);
        }
        item = mCurrentItem.calendarId;
        if (0 != item.length()) {
            values.put("calendar_id", item);
        }
        item = mCurrentItem.calendarTimezone;
        if (0 != item.length()) {
            values.put("calendar_timezone", item);
        }
        item = mCurrentItem.canOrganizerRespond;
        if (0 != item.length()) {
            values.put("can_organizer_respond", item);
        }
        item = mCurrentItem.dirty;
        if (0 != item.length()) {
            values.put("dirty", item);
        }
        item = mCurrentItem.eventColorIndex;
        if (0 != item.length()) {
            values.put("event_color_index", item);
        }
        item = mCurrentItem.eventStatus;
        if (0 != item.length()) {
            values.put("event_status", item);
        }
        item = mCurrentItem.hasAttendeeData;
        if (0 != item.length()) {
            values.put("has_attendee_data", item);
        }
        item = mCurrentItem.hasExtendedProperties;
        if (0 != item.length()) {
            values.put("has_extended_properties", item);
        }


        boolean bItem = mCurrentItem.isApiLevel17;
        if (bItem) {
            values.put("is_apiLevel_17", (int) 1);
        } else {
            values.put("is_apiLevel_17", (int) 0);
        }

        item = mCurrentItem.isOrganizer;
        if (0 != item.length()) {
            values.put("is_organizer", item);
        }
        item = mCurrentItem.lastDate;
        if (0 != item.length()) {
            values.put("last_date", item);
        }
        item = mCurrentItem.lastSynced;
        if (0 != item.length()) {
            values.put("last_synced", item);
        }
        item = mCurrentItem.maxReminders;
        if (0 != item.length()) {
            values.put("max_reminders", item);
        }
        int itemInt = mCurrentItem.minApiLevel;
        values.put("min_apiLevel", itemInt);

        item = mCurrentItem.organizer;
        if (0 != item.length()) {
            values.put("organizer", item);
        }
        item = mCurrentItem.originalId;
        if (0 != item.length()) {
            values.put("original_id", item);
        }
        item = mCurrentItem.ownerAccount;
        if (0 != item.length()) {
            values.put("owner_account", item);
        }
        item = mCurrentItem.selfAttendeeStatus;
        if (0 != item.length()) {
            values.put("self_attendeeStatus", item);
        }
        item = mCurrentItem.syncId;
        if (0 != item.length()) {
            values.put("sync_id", item);
        }
        item = mCurrentItem.visible;
        if (0 != item.length()) {
            values.put("visible", item);
        }


        item = mCurrentItem.syncData1;
        if (0 != item.length()) {
            values.put("sync_data1", item);
        }
        item = mCurrentItem.syncData2;
        if (0 != item.length()) {
            values.put("sync_data2", item);
        }
        item = mCurrentItem.syncData3;
        if (0 != item.length()) {
            values.put("sync_data3", item);
        }
        item = mCurrentItem.syncData4;
        if (0 != item.length()) {
            values.put("sync_data4", item);
        }
        item = mCurrentItem.syncData5;
        if (0 != item.length()) {
            values.put("sync_data5", item);
        }
        item = mCurrentItem.syncData6;
        if (0 != item.length()) {
            values.put("sync_data6", item);
        }
        item = mCurrentItem.syncData7;
        if (0 != item.length()) {
            values.put("sync_data7", item);
        }
        item = mCurrentItem.syncData8;
        if (0 != item.length()) {
            values.put("sync_data8", item);
        }
        item = mCurrentItem.syncData9;
        if (0 != item.length()) {
            values.put("sync_data9", item);
        }
        item = mCurrentItem.syncData10;
        if (0 != item.length()) {
            values.put("sync_data10", item);
        }


        return add(values, CalendarTableNamesV2.CALENDAR_EVENT_TABLE);

    }


    private boolean addCal_Color() {
        dbgTrace();

        ArrayList<CalendersV2.CalColor> calColorArrList = mCurrentItem.calColor;
        int count = calColorArrList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.CalColor mCalColor = calColorArrList.get(i);

            String item = mCalColor.alpha;
            if (0 != item.length()) {
                values.put("alpha", item);
            }
            item = mCalColor.red;
            if (0 != item.length()) {
                values.put("red", item);
            }
            item = mCalColor.blue;
            if (0 != item.length()) {
                values.put("blue", item);
            }
            item = mCalColor.green;
            if (0 != item.length()) {
                values.put("green", item);
            }
            if (!add(values, CalendarTableNamesV2.CAL_COLOR_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addList_of_attendees() {
        dbgTrace();

        ArrayList<CalendersV2.AttendessBean> attendessBeanList = mCurrentItem.listofAttendess;
        int count = attendessBeanList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.AttendessBean mAttendessBean = attendessBeanList.get(i);

            String item = mAttendessBean.attendeeEmail;
            if (0 != item.length()) {
                values.put("attendee_email", item);
            }
            item = mAttendessBean.attendeeIdentity;
            if (0 != item.length()) {
                values.put("attendee_identity", item);
            }
            item = mAttendessBean.attendeeIdNamespace;
            if (0 != item.length()) {
                values.put("attendee_idnamespace", item);
            }
            item = mAttendessBean.attendeeName;
            if (0 != item.length()) {
                values.put("attendee_name", item);
            }
            item = mAttendessBean.attendeeRelationship;
            if (0 != item.length()) {
                values.put("attendee_relationship", item);
            }
            item = mAttendessBean.attendeeStatus;
            if (0 != item.length()) {
                values.put("attendee_status", item);
            }
            item = mAttendessBean.attendeeType;
            if (0 != item.length()) {
                values.put("attendee_type", item);
            }
            item = mAttendessBean.attendeeEventId;
            if (0 != item.length()) {
                values.put("attendee_eventid", item);
            }

            if (!add(values, CalendarTableNamesV2.LIST_OF_ATTENDEES_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addList_of_instances() {
        dbgTrace();

        ArrayList<CalendersV2.InstancesBean> instancesBeanList = mCurrentItem.listofInstances;
        int count = instancesBeanList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.InstancesBean mInstancesBean = instancesBeanList.get(i);

            String item = mInstancesBean.instanceBegin;
            if (0 != item.length()) {
                values.put("instance_begin", item);
            }
            item = mInstancesBean.instanceEnd;
            if (0 != item.length()) {
                values.put("instance_end", item);
            }
            item = mInstancesBean.instanceEndDay;
            if (0 != item.length()) {
                values.put("instance_end_day", item);
            }
            item = mInstancesBean.instanceEndMinute;
            if (0 != item.length()) {
                values.put("instance_end_minute", item);
            }
            item = mInstancesBean.instanceEventId;
            if (0 != item.length()) {
                values.put("instance_event_id", item);
            }
            item = mInstancesBean.instanceStartDay;
            if (0 != item.length()) {
                values.put("instance_start_day", item);
            }
            item = mInstancesBean.instanceStartMinute;
            if (0 != item.length()) {
                values.put("instance_start_minute", item);
            }

            if (!add(values, CalendarTableNamesV2.LIST_OF_INSTANCES_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addReminders_list() {
        dbgTrace();

        ArrayList<CalendersV2.RemindersBean> remindersBeanList = mCurrentItem.listofReminders;
        int count = remindersBeanList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.RemindersBean mRemindersBean = remindersBeanList.get(i);

            String item = mRemindersBean.reminderEventId;
            if (0 != item.length()) {
                values.put("reminder_eventId", item);
            }
            item = mRemindersBean.reminderMethod;
            if (0 != item.length()) {
                values.put("reminder_method", item);
            }
            item = mRemindersBean.reminderMinutes;
            if (0 != item.length()) {
                values.put("reminder_minutes", item);
            }

            if (!add(values, CalendarTableNamesV2.REMINDERS_LIST_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addRRule_list() {
        dbgTrace();

        ArrayList<CalendersV2.Rrobj> RrobjList = mCurrentItem.rrObjs;
        int count = RrobjList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.Rrobj mRrobj = RrobjList.get(i);

            String item = mRrobj.interval;
            if (0 != item.length()) {
                values.put("interval", item);
            }
            item = mRrobj.frequency;
            if (0 != item.length()) {
                values.put("frequency", item);
            }
            item = mRrobj.rrend;
            if (0 != item.length()) {
                values.put("rrend", item);
            }
            int intVar = mRrobj.occurenceCount;
            values.put("occurence_count", (int) intVar);


            if (!add(values, CalendarTableNamesV2.RRULE_TABLE)) {
                return false;
            } else {

                if (mRrobj.daysOfTheWeek.size() > 0) {
                    if (!addDays_of_week(mRrobj)) dbgTrace("Failed to Add to Rrobj-Days of the Week table");
                }
                if (mRrobj.daysOfTheMonth.size() > 0) {
                    if (!addDays_of_Month(mRrobj)) dbgTrace("Failed to Add to Rrobj-Days of the Month table");
                }
                if (mRrobj.daysOfTheYear.size() > 0) {
                    if (!addDays_of_Year(mRrobj)) dbgTrace("Failed to Add to Rrobj-Days of the Year table");
                }
                if (mRrobj.weeksOfTheYear.size() > 0) {
                    if (!addWeek_of_year(mRrobj)) dbgTrace("Failed to Add to Rrobj-week of the Year table");
                }
                if (mRrobj.monthsOfTheYear.size() > 0) {
                    if (!addMonths_of_year(mRrobj)) dbgTrace("Failed to Add to Rrobj-months of the Year table");
                }
                if (mRrobj.setPositions.size() > 0) {
                    if (!addSetPositions(mRrobj)) dbgTrace("Failed to Add to Rrobj-setPosition Table");
                }
            }

        }

        return true;
    }


    private boolean addDays_of_week(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<CalendersV2.DaysOfTheWeek> daysOfWeekList = currRrobj.daysOfTheWeek;
        int count = daysOfWeekList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            CalendersV2.DaysOfTheWeek mDaysOfweek = daysOfWeekList.get(i);

            int item = mDaysOfweek.dayOfTheWeek;
            values.put("daysofweek", (int) item);

            item = mDaysOfweek.weekNumber;
            values.put("week_num", (int) item);

            if (!add(values, CalendarTableNamesV2.DAYSOFWEEK_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addDays_of_Month(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<Integer> daysOfMonthList = currRrobj.daysOfTheMonth;
        int count = daysOfMonthList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int item = daysOfMonthList.get(i);
            values.put("daysofmonth", (int) item);

            if (!add(values, CalendarTableNamesV2.DAYSOFMONTH_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addDays_of_Year(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<Integer> daysOfYearList = currRrobj.daysOfTheYear;
        int count = daysOfYearList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int item = daysOfYearList.get(i);
            values.put("daysofyear", (int) item);

            if (!add(values, CalendarTableNamesV2.DAYSOFYEAR_TABLE)) {
                return false;
            }

        }

        return true;
    }


    private boolean addWeek_of_year(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<Integer> weekOfYearList = currRrobj.weeksOfTheYear;
        int count = weekOfYearList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int item = weekOfYearList.get(i);
            values.put("weekofyear", (int) item);

            if (!add(values, CalendarTableNamesV2.WEEKOFYEAR_TABLE)) {
                return false;
            }

        }

        return true;
    }

    private boolean addMonths_of_year(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<Integer> monthsOfYearList = currRrobj.monthsOfTheYear;
        int count = monthsOfYearList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int item = monthsOfYearList.get(i);
            values.put("monthsofyear", (int) item);

            if (!add(values, CalendarTableNamesV2.MONTHSOFYEAR_TABLE)) {
                return false;
            }

        }

        return true;
    }

    private boolean addSetPositions(CalendersV2.Rrobj currRrobj) {
        dbgTrace();

        ArrayList<Integer> setPositionsList = currRrobj.setPositions;
        int count = setPositionsList.size();


        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int item = setPositionsList.get(i);
            values.put("setpositions", (int) item);

            if (!add(values, CalendarTableNamesV2.SETPOSITIONS_TABLE)) {
                return false;
            }

        }

        return true;
    }

    /**
     * Get DB Methods
     */

    public CalendersV2.Event getEventForChecksum(int cSum) {
        CalendersV2.Event currDbItem = new CalendersV2.Event();
        currDbItem.checksum = cSum;

        if (!getCalendarEventTableForChecksum(currDbItem)) dbgTrace("getCalendarEventTableForChecksum " + currDbItem.checksum + "failed");
        if (!getCalColorTableForChecksum(currDbItem)) dbgTrace("getCalColorTableForChecksum " + currDbItem.checksum + "failed");
        if (!getReminderListTableForChecksum(currDbItem)) dbgTrace("getReminderListTableForChecksum " + currDbItem.checksum + "failed");
        if (!getRRuleTableForChecksum(currDbItem)) dbgTrace("getRRuleTableForChecksum " + currDbItem.checksum + "failed");
        if (!getListofAttendeesTableForChecksum(currDbItem))
            dbgTrace("getListofAttendeesTableForChecksum " + currDbItem.checksum + "failed");
        if (!getListofInstancesTableForChecksum(currDbItem))
            dbgTrace("getListofInstancesTableForChecksum " + currDbItem.checksum + "failed");


        return currDbItem;

    }

    /**
     * Get DB Methods
     */

    public CalendersV2.Event getEventForRow(int row) {
        CalendersV2.Event currDbItem = new CalendersV2.Event();
        String sqlStmt = "select * from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE + " LIMIT 1 OFFSET " + row;
        currDbItem.checksum = rawQuerryGetInt(sqlStmt, 0);

        if (!getCalendarEventTableForChecksum(currDbItem)) dbgTrace("getCalendarEventTableForChecksum " + currDbItem.checksum + "failed");
        if (!getCalColorTableForChecksum(currDbItem)) dbgTrace("getCalColorTableForChecksum " + currDbItem.checksum + "failed");
        if (!getReminderListTableForChecksum(currDbItem)) dbgTrace("getReminderListTableForChecksum " + currDbItem.checksum + "failed");
        if (!getRRuleTableForChecksum(currDbItem)) dbgTrace("getRRuleTableForChecksum " + currDbItem.checksum + "failed");
        if (!getListofAttendeesTableForChecksum(currDbItem))
            dbgTrace("getListofAttendeesTableForChecksum " + currDbItem.checksum + "failed");
        if (!getListofInstancesTableForChecksum(currDbItem))
            dbgTrace("getListofInstancesTableForChecksum " + currDbItem.checksum + "failed");


        return currDbItem;

    }

    private boolean getCalendarEventTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            cursor.moveToNext();

            currItem.hasAlarm = cursor.getString(1);
            if (null == currItem.hasAlarm) {
                currItem.hasAlarm = "";
            }

            currItem.eventTimezone = cursor.getString(2);
            if (null == currItem.eventTimezone) {
                currItem.eventTimezone = "";
            }

            currItem.description = cursor.getString(3);
            if (null == currItem.description) {
                currItem.description = "";
            }

            currItem.eventLocation = cursor.getString(4);
            if (null == currItem.eventLocation) {
                currItem.eventLocation = "";
            }

            currItem.dtstart = cursor.getString(5);
            if (null == currItem.dtstart) {
                currItem.dtstart = "";
            }

            currItem.dtend = cursor.getString(6);
            if (null == currItem.dtend) {
                currItem.dtend = "";
            }

            currItem.allDay = cursor.getString(7);
            if (null == currItem.allDay) {
                currItem.allDay = "";
            }

            currItem.title = cursor.getString(8);
            if (null == currItem.title) {
                currItem.title = "";
            }

            currItem.calendarDisplayName = cursor.getString(9);
            if (null == currItem.calendarDisplayName) {
                currItem.calendarDisplayName = "";
            }

            currItem.duration = cursor.getString(10);
            if (null == currItem.duration) {
                currItem.duration = "";
            }

            currItem.supEventAvailabilities = cursor.getString(11);
            if (null == currItem.supEventAvailabilities) {
                currItem.supEventAvailabilities = "";
            }

            currItem.calId = cursor.getString(12);
            if (null == currItem.calId) {
                currItem.calId = "";
            }

            currItem.calItemId = cursor.getString(13);
            if (null == currItem.calItemId) {
                currItem.calItemId = "";
            }

            currItem.calItemExternalId = cursor.getString(14);
            if (null == currItem.calItemExternalId) {
                currItem.calItemExternalId = "";
            }

            currItem.source = cursor.getString(15);
            if (null == currItem.source) {
                currItem.source = "";
            }

            currItem.subscribed = cursor.getString(16);
            if (null == currItem.subscribed) {
                currItem.subscribed = "";
            }

            currItem.eventId = cursor.getString(17);
            if (null == currItem.eventId) {
                currItem.eventId = "";
            }

            currItem.availability = cursor.getString(18);
            if (null == currItem.availability) {
                currItem.availability = "";
            }

            currItem.isDetached = cursor.getString(19);
            if (null == currItem.isDetached) {
                currItem.isDetached = "";
            }

            currItem.status = cursor.getString(20);
            if (null == currItem.status) {
                currItem.status = "";
            }

            currItem.birthdayPersonId = cursor.getString(21);
            if (null == currItem.birthdayPersonId) {
                currItem.birthdayPersonId = "";
            }

            currItem.creationDate = cursor.getString(22);
            if (null == currItem.creationDate) {
                currItem.creationDate = "";
            }

            currItem.lastModifiedDate = cursor.getString(23);
            if (null == currItem.lastModifiedDate) {
                currItem.lastModifiedDate = "";
            }

            currItem.url = cursor.getString(24);
            if (null == currItem.url) {
                currItem.url = "";
            }

            currItem.hasNotes = cursor.getString(25);
            if (null == currItem.hasNotes) {
                currItem.hasNotes = "";
            }

            currItem.hasRrule = cursor.getString(26);
            if (null == currItem.hasRrule) {
                currItem.hasRrule = "";
            }

            currItem.isContentModifiable = cursor.getString(27);
            if (null == currItem.isContentModifiable) {
                currItem.isContentModifiable = "";
            }

            currItem.isImmutable = cursor.getString(28);
            if (null == currItem.isImmutable) {
                currItem.isImmutable = "";
            }

            currItem.type = cursor.getString(29);
            if (null == currItem.type) {
                currItem.type = "";
            }

            currItem.allowedEntityTypes = cursor.getString(30);
            if (null == currItem.allowedEntityTypes) {
                currItem.allowedEntityTypes = "";
            }

            currItem.rrule = cursor.getString(31);
            if (null == currItem.rrule) {
                currItem.rrule = "";
            }


            currItem._id = cursor.getString(32);
            if (null == currItem._id) {
                currItem._id = "";
            }

            currItem.accountName = cursor.getString(33);
            if (null == currItem.accountName) {
                currItem.accountName = "";
            }

            currItem.accountType = cursor.getString(34);
            if (null == currItem.accountType) {
                currItem.accountType = "";
            }

            currItem.allowedEntityTypes = cursor.getString(35);
            if (null == currItem.allowedEntityTypes) {
                currItem.allowedEntityTypes = "";
            }

            currItem.allowedAvailability = cursor.getString(36);
            if (null == currItem.allowedAvailability) {
                currItem.allowedAvailability = "";
            }

            currItem.allowedReminders = cursor.getString(37);
            if (null == currItem.allowedReminders) {
                currItem.allowedReminders = "";
            }


            currItem.calSync1 = cursor.getString(38);
            if (null == currItem.calSync1) {
                currItem.calSync1 = "";
            }

            currItem.calSync2 = cursor.getString(39);
            if (null == currItem.calSync2) {
                currItem.calSync2 = "";
            }

            currItem.calSync3 = cursor.getString(40);
            if (null == currItem.calSync3) {
                currItem.calSync3 = "";
            }

            currItem.calSync4 = cursor.getString(41);
            if (null == currItem.calSync4) {
                currItem.calSync4 = "";
            }

            currItem.calSync5 = cursor.getString(42);
            if (null == currItem.calSync5) {
                currItem.calSync5 = "";
            }

            currItem.calSync6 = cursor.getString(43);
            if (null == currItem.calSync6) {
                currItem.calSync6 = "";
            }

            currItem.calSync7 = cursor.getString(44);
            if (null == currItem.calSync7) {
                currItem.calSync7 = "";
            }

            currItem.calSync8 = cursor.getString(45);
            if (null == currItem.calSync8) {
                currItem.calSync8 = "";
            }

            currItem.calSync9 = cursor.getString(46);
            if (null == currItem.calSync9) {
                currItem.calSync9 = "";
            }

            currItem.calSync10 = cursor.getString(47);
            if (null == currItem.calSync10) {
                currItem.calSync10 = "";
            }


            currItem.calendarAccessLevel = cursor.getString(48);
            if (null == currItem.calendarAccessLevel) {
                currItem.calendarAccessLevel = "";
            }

            currItem.calendarColorIndex = cursor.getString(49);
            if (null == currItem.calendarColorIndex) {
                currItem.calendarColorIndex = "";
            }

            currItem.calendarId = cursor.getString(50);
            if (null == currItem.calendarId) {
                currItem.calendarId = "";
            }

            currItem.calendarTimezone = cursor.getString(51);
            if (null == currItem.calendarTimezone) {
                currItem.calendarTimezone = "";
            }

            currItem.canOrganizerRespond = cursor.getString(52);
            if (null == currItem.canOrganizerRespond) {
                currItem.canOrganizerRespond = "";
            }

            currItem.dirty = cursor.getString(53);
            if (null == currItem.dirty) {
                currItem.dirty = "";
            }

            currItem.eventColorIndex = cursor.getString(54);
            if (null == currItem.eventColorIndex) {
                currItem.eventColorIndex = "";
            }

            currItem.eventStatus = cursor.getString(55);
            if (null == currItem.eventStatus) {
                currItem.eventStatus = "";
            }

            currItem.hasAttendeeData = cursor.getString(56);
            if (null == currItem.hasAttendeeData) {
                currItem.hasAttendeeData = "";
            }

            currItem.hasExtendedProperties = cursor.getString(57);
            if (null == currItem.hasExtendedProperties) {
                currItem.hasExtendedProperties = "";
            }

            if (cursor.getInt(58) > 0) {
                currItem.isApiLevel17 = true;
            } else {
                currItem.isApiLevel17 = false;
            }
            currItem.isOrganizer = cursor.getString(59);
            if (null == currItem.isOrganizer) {
                currItem.isOrganizer = "";
            }

            currItem.lastDate = cursor.getString(60);
            if (null == currItem.lastDate) {
                currItem.lastDate = "";
            }

            currItem.lastSynced = cursor.getString(61);
            if (null == currItem.lastSynced) {
                currItem.lastSynced = "";
            }

            currItem.maxReminders = cursor.getString(62);
            if (null == currItem.maxReminders) {
                currItem.maxReminders = "";
            }

            currItem.minApiLevel = cursor.getInt(63);

            currItem.organizer = cursor.getString(64);
            if (null == currItem.organizer) {
                currItem.organizer = "";
            }

            currItem.originalId = cursor.getString(65);
            if (null == currItem.originalId) {
                currItem.originalId = "";
            }

            currItem.ownerAccount = cursor.getString(66);
            if (null == currItem.ownerAccount) {
                currItem.ownerAccount = "";
            }

            currItem.selfAttendeeStatus = cursor.getString(67);
            if (null == currItem.selfAttendeeStatus) {
                currItem.selfAttendeeStatus = "";
            }

            currItem.syncId = cursor.getString(68);
            if (null == currItem.syncId) {
                currItem.syncId = "";
            }

            currItem.visible = cursor.getString(69);
            if (null == currItem.visible) {
                currItem.visible = "";
            }


            currItem.syncData1 = cursor.getString(70);
            if (null == currItem.syncData1) {
                currItem.syncData1 = "";
            }

            currItem.syncData2 = cursor.getString(71);
            if (null == currItem.syncData2) {
                currItem.syncData2 = "";
            }

            currItem.syncData3 = cursor.getString(72);
            if (null == currItem.syncData3) {
                currItem.syncData3 = "";
            }

            currItem.syncData4 = cursor.getString(73);
            if (null == currItem.syncData4) {
                currItem.syncData4 = "";
            }

            currItem.syncData5 = cursor.getString(74);
            if (null == currItem.syncData5) {
                currItem.syncData5 = "";
            }

            currItem.syncData6 = cursor.getString(75);
            if (null == currItem.syncData6) {
                currItem.syncData6 = "";
            }

            currItem.syncData7 = cursor.getString(76);
            if (null == currItem.syncData7) {
                currItem.syncData7 = "";
            }

            currItem.syncData8 = cursor.getString(77);
            if (null == currItem.syncData8) {
                currItem.syncData8 = "";
            }

            currItem.syncData9 = cursor.getString(78);
            if (null == currItem.syncData9) {
                currItem.syncData9 = "";
            }

            currItem.syncData10 = cursor.getString(79);
            if (null == currItem.syncData10) {
                currItem.syncData10 = "";
            }


            currItem.calendarColor = cursor.getString(80);
            if (null == currItem.calendarColor) {
                currItem.calendarColor = "";
            }

            currItem.rdate = cursor.getString(81);
            if (null == currItem.rdate) {
                currItem.rdate = "";
            }

            currItem.eventColor = cursor.getString(82);
            if (null == currItem.eventColor) {
                currItem.eventColor = "";
            }

            currItem.eventEndTimezone = cursor.getString(83);
            if (null == currItem.eventEndTimezone) {
                currItem.eventEndTimezone = "";
            }

            currItem.exrule = cursor.getString(84);
            if (null == currItem.exrule) {
                currItem.exrule = "";
            }

            currItem.exdate = cursor.getString(85);
            if (null == currItem.exdate) {
                currItem.exdate = "";
            }

            currItem.originalSyncId = cursor.getString(86);
            if (null == currItem.originalSyncId) {
                currItem.originalSyncId = "";
            }

            currItem.originalInstanceTime = cursor.getString(87);
            if (null == currItem.originalInstanceTime) {
                currItem.originalInstanceTime = "";
            }

            currItem.originalAllDay = cursor.getString(88);
            if (null == currItem.originalAllDay) {
                currItem.originalAllDay = "";
            }

            currItem.accessLevel = cursor.getString(89);
            if (null == currItem.accessLevel) {
                currItem.accessLevel = "";
            }

            currItem.customAppPackage = cursor.getString(90);
            if (null == currItem.customAppPackage) {
                currItem.customAppPackage = "";
            }

            currItem.customAppUri = cursor.getString(91);
            if (null == currItem.customAppUri) {
                currItem.customAppUri = "";
            }

            currItem.uid2445 = cursor.getString(92);
            if (null == currItem.uid2445) {
                currItem.uid2445 = "";
            }

            currItem.guestsCanModify = cursor.getString(93);
            if (null == currItem.guestsCanModify) {
                currItem.guestsCanModify = "";
            }

            currItem.guestsCanInviteOthers = cursor.getString(94);
            if (null == currItem.guestsCanInviteOthers) {
                currItem.guestsCanInviteOthers = "";
            }

            currItem.guestsCanSeeGuests = cursor.getString(95);
            if (null == currItem.guestsCanSeeGuests) {
                currItem.guestsCanSeeGuests = "";
            }


            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getCalColorTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.CAL_COLOR_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.CalColor currCalcolor = new CalendersV2.CalColor();

                currCalcolor.alpha = cursor.getString(1);
                if (null == currCalcolor.alpha) {
                    currCalcolor.alpha = "";
                }

                currCalcolor.red = cursor.getString(2);
                if (null == currCalcolor.red) {
                    currCalcolor.red = "";
                }

                currCalcolor.green = cursor.getString(3);
                if (null == currCalcolor.green) {
                    currCalcolor.green = "";
                }

                currCalcolor.blue = cursor.getString(4);
                if (null == currCalcolor.blue) {
                    currCalcolor.blue = "";
                }


                currItem.calColor.add(currCalcolor);
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    private boolean getReminderListTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.REMINDERS_LIST_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.RemindersBean currRBean = new CalendersV2.RemindersBean();

                currRBean.absoluteDate = cursor.getString(1);
                if (null == currRBean.absoluteDate) {
                    currRBean.absoluteDate = "";
                }

                currRBean.reminderMinutes = cursor.getString(2);
                if (null == currRBean.reminderMinutes) {
                    currRBean.reminderMinutes = "";
                }

                currRBean.reminderEventId = cursor.getString(3);
                if (null == currRBean.reminderEventId) {
                    currRBean.reminderEventId = "";
                }

                currRBean.reminderMethod = cursor.getString(4);
                if (null == currRBean.reminderMethod) {
                    currRBean.reminderMethod = "";
                }


                currItem.listofReminders.add(currRBean);
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    private boolean getRRuleTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.RRULE_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.Rrobj currRrobj = new CalendersV2.Rrobj();

                currRrobj.interval = cursor.getString(1);
                if (null == currRrobj.interval) {
                    currRrobj.interval = "";
                }

                currRrobj.frequency = cursor.getString(2);
                if (null == currRrobj.frequency) {
                    currRrobj.frequency = "";
                }

                currRrobj.rrend = cursor.getString(3);
                if (null == currRrobj.rrend) {
                    currRrobj.rrend = "";
                }

                currRrobj.occurenceCount = cursor.getInt(4);


                if (!getDaysoftheWeekTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from DaysoftheWeekTable  Failed");
                if (!getDaysoftheMonthTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from DaysoftheMonthTable  Failed");
                if (!getDaysoftheYearTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from DaysoftheYearTable  Failed");
                if (!getWeeksOftheYearTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from WeeksOftheYearTable  Failed");
                if (!getMonthsOftheYearTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from MonthsOftheYearTable  Failed");
                if (!getSetpositionsTableForChecksum(currRrobj, currItem.checksum))
                    dbgTrace("Getting records from SetpositionsTable Failed");

                currItem.rrObjs.add(currRrobj);


            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getDaysoftheWeekTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.DAYSOFWEEK_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.DaysOfTheWeek currDaysoftheweek = new CalendersV2.DaysOfTheWeek();
                currDaysoftheweek.dayOfTheWeek = cursor.getInt(1);
                currDaysoftheweek.weekNumber = cursor.getInt(2);

                robj.daysOfTheWeek.add(currDaysoftheweek);
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    private boolean getDaysoftheMonthTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.DAYSOFMONTH_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                robj.daysOfTheMonth.add(cursor.getInt(1));
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    private boolean getDaysoftheYearTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.DAYSOFYEAR_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                robj.daysOfTheYear.add(cursor.getInt(1));
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getWeeksOftheYearTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.WEEKOFYEAR_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                robj.weeksOfTheYear.add(cursor.getInt(1));
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getMonthsOftheYearTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.MONTHSOFYEAR_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                robj.monthsOfTheYear.add(cursor.getInt(1));
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getSetpositionsTableForChecksum(CalendersV2.Rrobj robj, int cSum) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.SETPOSITIONS_TABLE + " where checksum = " + cSum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                robj.setPositions.add(cursor.getInt(1));
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    private boolean getListofAttendeesTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.LIST_OF_ATTENDEES_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.AttendessBean currAttendee = new CalendersV2.AttendessBean();

                currAttendee.attendeeEmail = cursor.getString(1);
                if (null == currAttendee.attendeeEmail) {
                    currAttendee.attendeeEmail = "";
                }

                currAttendee.attendeeIdentity = cursor.getString(2);
                if (null == currAttendee.attendeeIdentity) {
                    currAttendee.attendeeIdentity = "";
                }

                currAttendee.attendeeIdNamespace = cursor.getString(3);
                if (null == currAttendee.attendeeIdNamespace) {
                    currAttendee.attendeeIdNamespace = "";
                }

                currAttendee.attendeeName = cursor.getString(4);
                if (null == currAttendee.attendeeName) {
                    currAttendee.attendeeName = "";
                }

                currAttendee.attendeeRelationship = cursor.getString(5);
                if (null == currAttendee.attendeeRelationship) {
                    currAttendee.attendeeRelationship = "";
                }

                currAttendee.attendeeStatus = cursor.getString(6);
                if (null == currAttendee.attendeeStatus) {
                    currAttendee.attendeeStatus = "";
                }

                currAttendee.attendeeType = cursor.getString(7);
                if (null == currAttendee.attendeeType) {
                    currAttendee.attendeeType = "";
                }

                currAttendee.attendeeEventId = cursor.getString(8);
                if (null == currAttendee.attendeeEventId) {
                    currAttendee.attendeeEventId = "";
                }

                currItem.listofAttendess.add(currAttendee);
            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }

    private boolean getListofInstancesTableForChecksum(CalendersV2.Event currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + CalendarTableNamesV2.LIST_OF_INSTANCES_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                CalendersV2.InstancesBean currInstances = new CalendersV2.InstancesBean();

                currInstances.instanceBegin = cursor.getString(1);
                currInstances.instanceEnd = cursor.getString(2);
                currInstances.instanceEndDay = cursor.getString(3);
                currInstances.instanceEndMinute = cursor.getString(4);
                currInstances.instanceEventId = cursor.getString(5);
                currInstances.instanceStartDay = cursor.getString(6);
                currInstances.instanceStartMinute = cursor.getString(7);

                currItem.listofInstances.add(currInstances);

            }

            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }
        return result;
    }


    public boolean deleteCalendarForChecksum(int cSum) {
        boolean result = true;

        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + CalendarTableNamesV2.LINKS_COUNT_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.REMINDERS_LIST_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.CAL_COLOR_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.RRULE_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFWEEK_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFMONTH_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.DAYSOFYEAR_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.WEEKOFYEAR_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.MONTHSOFYEAR_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.SETPOSITIONS_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.LIST_OF_ATTENDEES_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + CalendarTableNamesV2.LIST_OF_INSTANCES_TABLE + " where checksum = " + cSum);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public boolean deleteAllCalendars() {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete  from " + CalendarTableNamesV2.VAULT_CALENDAR_TABLE);
            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public int getRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlStmt = "Select count(*) from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE;
        Cursor cursor = db.rawQuery(sqlStmt, null);
        int item = 0;
        if (null == cursor) {
            return 0;
        }

        while (cursor.moveToNext()) {
            item = cursor.getInt(0);
        }


        if (null == cursor) {
            cursor.close();
        }

        db.close();
        return item;
    }

    public int getDBSchemaVersion() {

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "pragma user_version ";

        Cursor cursor = db.rawQuery(query, null);
        int ver = 0;
        while (cursor.moveToNext()) {
            ver = cursor.getInt(0);
        }
        return ver;
    }


    public void setDBSchemaVersion(int ver) {

        SQLiteDatabase db = this.getReadableDatabase();

        String sqlStmt = "pragma user_version = " + ver;
        db.execSQL(sqlStmt);

    }

    public boolean isCalendarForCSM(int csm) {
        boolean isPresent = false;
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlStmt = "Select * from " + CalendarTableNamesV2.CALENDAR_EVENT_TABLE + " where checksum = " + csm + " ";
        Cursor cursor = db.rawQuery(sqlStmt, null);
        if (null == cursor) {
            isPresent = false;
        }

        if (cursor.getCount() > 0)
            isPresent = true;

        if (null == cursor) {
            cursor.close();
        }

        db.close();
        return isPresent;
    }


}