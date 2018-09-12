package com.meem.v2.phone;

/**
 * Created by karthik B S on 3/4/17.
 */
public class CalendarTableNamesV2 {

    public static final String LINKS_COUNT_TABLE = "linkscount_table";
    public static final String VAULT_CALENDAR_TABLE = "vault_calendar_table";
    public static final String CALENDAR_EVENT_TABLE = "calendar_event_table";
    public static final String REMINDERS_LIST_TABLE = "reminders_list_table";
    public static final String CAL_COLOR_TABLE = "cal_color_table";
    public static final String RRULE_TABLE = "rrule_table";
    public static final String DAYSOFWEEK_TABLE = "daysofweek_table";
    public static final String DAYSOFMONTH_TABLE = "daysofmonth_table";
    public static final String DAYSOFYEAR_TABLE = "daysofyear_table";
    public static final String WEEKOFYEAR_TABLE = "weekofyear_table";
    public static final String MONTHSOFYEAR_TABLE = "monthsofyear_table";
    public static final String SETPOSITIONS_TABLE = "setpositions_table";
    public static final String LIST_OF_ATTENDEES_TABLE = "list_of_attendees";
    public static final String LIST_OF_INSTANCES_TABLE = "list_of_instances";


    public static final String CREATE_VAULT_LINKS_COUNT_TABLE = "CREATE TABLE  if not exists " + LINKS_COUNT_TABLE + "( " + "checksum INTEGER, " + "linkscount INTEGER )";

    public static final String CREATE_VAULT_CALENDAR_TABLE = "CREATE TABLE if not exists " + VAULT_CALENDAR_TABLE + "( checksum INTEGER , dtstart TEXT )";

    public static final String CREATE_CALENDAR_EVENT_TABLE = "CREATE TABLE if not exists " + CALENDAR_EVENT_TABLE + "( " + "checksum  INTEGER, " + "has_alarm TEXT, " + "event_timezone TEXT, " + "description TEXT, " + "event_location TEXT, " + "dtstart TEXT, " + "dtend TEXT, " + "allday TEXT, " + "title TEXT, " + "calendar_displayname TEXT, " + "duration TEXT, " + "sup_event_availabilities TEXT, " + "cal_id TEXT, " + "cal_itemid TEXT, " + "cal_item_externalid TEXT, " + "source TEXT, " + "subscribed TEXT, " + "eventid TEXT, " + "availability TEXT, " + "isdetached TEXT, " + "status TEXT, " + "birthday_personid TEXT, " + "creationdate TEXT, " + "last_modifieddate TEXT, " + "url TEXT, " + "hasnotes TEXT, " + "has_rrule TEXT, " + "iscontent_modifiable TEXT, " + "isimmutable TEXT, " + "type TEXT, " + "allowed_entitytypes TEXT, " + "rrule TEXT, "
            //  + "frequecy TEXT, "


            // Android
            + "id TEXT, " + "account_name TEXT, " + "account_type TEXT, " + "allowed_attendee_types TEXT, " + "allowed_availability TEXT, " + "allowed_reminders TEXT, "

            + "cal_sync1 TEXT, " + "cal_sync2 TEXT, " + "cal_sync3 TEXT, " + "cal_sync4 TEXT, " + "cal_sync5 TEXT, " + "cal_sync6 TEXT, " + "cal_sync7 TEXT, " + "cal_sync8 TEXT, " + "cal_sync9 TEXT, " + "cal_sync10 TEXT, "

            + "calendar_access_level TEXT, " + "calendar_color_index TEXT, " + "calendar_id TEXT, " + "calendar_timezone TEXT, " + "can_organizer_respond TEXT, " + "dirty TEXT, " + "event_color_index TEXT, " + "event_status TEXT, " + "has_attendee_data TEXT, " + "has_extended_properties TEXT, " + "is_apiLevel_17 TEXT, " + "is_organizer TEXT, " + "last_date TEXT, " + "last_synced TEXT, " + "max_reminders TEXT, " + "min_apiLevel INTEGER, " + "organizer TEXT, " + "original_id TEXT, " + "owner_account TEXT, " + "self_attendeeStatus TEXT, " + "sync_id TEXT, " + "visible TEXT, "


            + "sync_data1 TEXT, " + "sync_data2 TEXT, " + "sync_data3 TEXT, " + "sync_data4 TEXT, " + "sync_data5 TEXT, " + "sync_data6 TEXT, " + "sync_data7 TEXT, " + "sync_data8 TEXT, " + "sync_data9 TEXT, " + "sync_data10 TEXT, "


            + "calender_color TEXT, " + "rdate TEXT, " + "event_color TEXT, " + "event_end_timezone TEXT, " + "exrule TEXT, " + "exdate TEXT, " + "original_sync_id TEXT, " + "original_instance_time TEXT, " + "original_allday TEXT, " + "access_level TEXT, " + "customapp_package TEXT, " + "customapp_uri TEXT, " + "uid_2445 TEXT, " + "guestcan_modify TEXT, " + "guestcan_inviteothers TEXT, " + "guestcan_see_guests TEXT )";

    public static final String CREATE_CAL_COLOR_TABLE = "CREATE TABLE if not exists " + CAL_COLOR_TABLE + "( " + "checksum  INTEGER, " + "alpha TEXT, " + "red TEXT, " + "green TEXT, " + "blue TEXT )";


    public static final String CREATE_REMINDERS_LIST_TABLE = "CREATE TABLE if not exists " + REMINDERS_LIST_TABLE + "( " + "checksum  INTEGER, " + "absolutedate TEXT, " + "reminder_minutes TEXT, " + "reminder_eventId TEXT, " + "reminder_method  TEXT )";


    public static final String CREATE_RRULE_TABLE = "CREATE TABLE if not exists " + RRULE_TABLE + "( " + "checksum  INTEGER, " + "interval TEXT, " + "frequency TEXT, " + "rrend TEXT, " + "occurence_count INTEGER )";

    public static final String CREATE_DAYSOFWEEK_TABLE = "CREATE TABLE if not exists " + DAYSOFWEEK_TABLE + "( " + "checksum  INTEGER, " + "daysofweek INTEGER, " + "week_num INTEGER )";
    public static final String CREATE_DAYSOFMONTH_TABLE = "CREATE TABLE if not exists " + DAYSOFMONTH_TABLE + "( " + "checksum  INTEGER, " + "daysofmonth INTEGER )";
    public static final String CREATE_DAYSOFYEAR_TABLE = "CREATE TABLE if not exists " + DAYSOFYEAR_TABLE + "( " + "checksum  INTEGER, " + "daysofyear INTEGER )";
    public static final String CREATE_WEEKOFYEAR_TABLE = "CREATE TABLE if not exists " + WEEKOFYEAR_TABLE + "( " + "checksum  INTEGER, " + "weekofyear INTEGER )";
    public static final String CREATE_MONTHSOFYEAR_TABLE = "CREATE TABLE if not exists " + MONTHSOFYEAR_TABLE + "( " + "checksum  INTEGER, " + "monthsofyear INTEGER )";
    public static final String CREATE_SETPOSITIONS_TABLE = "CREATE TABLE if not exists " + SETPOSITIONS_TABLE + "( " + "checksum  INTEGER, " + "setpositions INTEGER )";
    public static final String CREATE_LIST_OF_ATTENDEES_TABLE = "CREATE TABLE if not exists " + LIST_OF_ATTENDEES_TABLE + "( " + "checksum  INTEGER, " + "attendee_email TEXT, " + "attendee_identity TEXT, " + "attendee_idnamespace TEXT, " + "attendee_name TEXT, " + "attendee_relationship TEXT, " + "attendee_status TEXT, " + "attendee_type TEXT, " + "attendee_eventid TEXT )";

    public static final String CREATE_LIST_OF_INSTANCES_TABLE = "CREATE TABLE if not exists " + LIST_OF_INSTANCES_TABLE + "( " + "checksum  INTEGER, " + "instance_begin TEXT, " + "instance_end TEXT, " + "instance_end_day TEXT, " + "instance_end_minute TEXT, " + "instance_event_id TEXT, " + "instance_start_day TEXT, " + "instance_start_minute TEXT )";

}