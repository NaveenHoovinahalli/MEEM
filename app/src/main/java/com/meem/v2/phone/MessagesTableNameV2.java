package com.meem.v2.phone;

/**
 * Created by Karthik B S on 3/4/17.
 */
public class MessagesTableNameV2 {


    public static final String LINKS_COUNT_TABLE = "linkscount_table";
    public static final String VAULT_MESSAGE_TABLE = "vault_message_table";
    public static final String SMS_MSG_TABLE = "sms_msg_table";


    public static final String CREATE_VAULT_LINKS_COUNT_TABLE = "CREATE TABLE if not exists " + LINKS_COUNT_TABLE + "( " + "checksum INTEGER, " + "linkscount INTEGER )";

    public static final String CREATE_VAULT_MESSAGE_TABLE = "CREATE TABLE if not exists " + VAULT_MESSAGE_TABLE + "(checksum INTEGER)";
    public static final String CREATE_SMS_MSG_TABLE = "CREATE TABLE if not exists " + SMS_MSG_TABLE + "( " + "checksum  INTEGER, " + "identi2 TEXT, " + "address TEXT, " + "thread_id TEXT, " + "person TEXT, " + "date TEXT, " + "date_sent TEXT, " + "protocol TEXT, " + "read TEXT, " + "status TEXT, " + "reply_path_present TEXT, " + "subject TEXT, " + "body TEXT, " + "service_center TEXT, " + "locked TEXT, " + "error_code TEXT, " + "seen TEXT, " + "type TEXT, "


            + "sub_id TEXT, " + "semc_message_priority TEXT, " + "parent_id TEXT, " + "delivery_status TEXT, " + "star_status TEXT, " + "delivery_date TEXT, "


            + "message_type_all INTEGER, " + "message_type_draft INTEGER, " + "message_type_failed INTEGER, " + "message_type_inbox INTEGER, " + "message_type_outbox INTEGER, " + "message_type_queued INTEGER, " + "message_type_sent INTEGER )";


}