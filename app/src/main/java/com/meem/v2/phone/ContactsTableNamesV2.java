package com.meem.v2.phone;

/**
 * Created by Karthik B S on 3/4/17.
 */
public class ContactsTableNamesV2 {


    public static final String TABLE_NAME_LINKS_COUNT_TABLE = "linkscount_table";
    public static final String CREATE_VAULT_LINKS_COUNT_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_LINKS_COUNT_TABLE + "( " + "checksum INTEGER, " + "linkscount INTEGER )";

    public static final String TABLE_NAME_VAULT_CONTACTS = "vault_contacts_table";
    public static final String CREATE_VAULT_CONTACTS_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_VAULT_CONTACTS + "(checksum INTEGER , " + " lastname TEXT, " + "firstname TEXT, " + "number TEXT )";

    public static final String TABLE_NAME_DUPLICATES_MAPPING_CONTACTS = "duplicate_mapping_table";
    public static final String CREATE_DUPLICATES_MAPPING_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_DUPLICATES_MAPPING_CONTACTS + "(checksum INTEGER , " + "dupchecksum INTEGER )";

    public static final String TABLE_NAME_CONTACTS_TABLE = "contacts_table";
    public static final String CREATE_CONTACTS_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_CONTACTS_TABLE +
            "( " + "checksum  INTEGER, " + "contactid TEXT, " + "note TEXT, " + "displayname TEXT, " + "name_prefix TEXT, " + "name_suffix TEXT, " + "phonetic_fname TEXT, "
            + "phonetic_mname TEXT, " + "phonetic_gname TEXT, " + "birthday TEXT, " + "firstname TEXT, " + "lastname TEXT, " + "middlename TEXT, " + "photobitmap TEXT, " +
            "nickname TEXT, " + "account_name TEXT, " + "source TEXT, " + "group_source_id TEXT, " + "aggregation_mode INTEGER, " + "times_contacted INTEGER, " +
            "last_time_contacted INTEGER, " + "send_to_voicemail INTEGER, " + "custom_ringtone TEXT, " + "is_starred  TEXT )";


    public static final String TABLE_NAME_EMAIL_LIST = "email_list_table";
    public static final String CREATE_EMAIL_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_EMAIL_LIST + "( " + "checksum INTEGER, " + "email_id TEXT, " + "email_type INTEGER, " + "email_label INTEGER )";


    public static final String TABLE_NAME_PHONE_LIST = "phone_list_table";
    public static final String CREATE_PHONE_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_PHONE_LIST + "( " + "checksum INTEGER, " + "type INTEGER, " + "number TEXT, " + "label TEXT )";

    public static final String TABLE_NAME_IM_LIST = "im_list_table";
    public static final String CREATE_IM_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_IM_LIST + "( " + "checksum INTEGER, " + "imtype INTEGER, " + "customtype TEXT, " + "improtocol TEXT, " + "customprotocol TEXT, " + "imaddress TEXT )";


    public static final String TABLE_NAME_EVENT_LIST = "event_list_table";
    public static final String CREATE_EVENT_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_EVENT_LIST + "( " + "checksum INTEGER, " + "startdate TEXT, " + "etype INTEGER, " + "eventlabel TEXT )";


    public static final String TABLE_NAME_WEBSITE_LIST = "website_list_table";
    public static final String CREATE_WEBSITE_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_WEBSITE_LIST + "( " + "checksum INTEGER, " + "website TEXT, " + "websitetype INTEGER, " + "websitelabel TEXT )";

    public static final String TABLE_NAME_SOCIAL_LIST = "social_list_table";
    public static final String CREATE_SOCIAL_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_SOCIAL_LIST + "( " + "checksum INTEGER, " + "profilename TEXT, " + "type INTEGER, " + "custom_profilename TEXT, " + "service TEXT, " + "url TEXT )";


    public static final String TABLE_NAME_POSTALADDRESS_LIST = "postaladdress_list_table";
    public static final String CREATE_POSTALADDRESS_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_POSTALADDRESS_LIST + "( " + "checksum INTEGER, " + "type INTEGER, " + "address TEXT, " + "street TEXT, " + "city TEXT, " + "zipcode TEXT, " + "country TEXT, " + "countrycode TEXT, " + "region TEXT, " + "neighbourhood TEXT, " + "label TEXT )";


    public static final String TABLE_NAME_RELATIONSHIP_LIST = "relationship_list_table";
    public static final String CREATE_RELATIONSHIP_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_RELATIONSHIP_LIST + "( " + "checksum INTEGER, " + "relationname TEXT, " + "relationtype INTEGER, " + "relationlabel TEXT )";

    public static final String TABLE_NAME_ORGANIZATION_LIST = "organization_list_table";
    public static final String CREATE_ORGANIZATION_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_ORGANIZATION_LIST + "( " + "checksum INTEGER, " + "organizationname TEXT, " + "title TEXT, " + "jobdepartment TEXT, " + "type INTEGER, " + "jobdescription TEXT, " + "officesymbol TEXT, " + "office_phoneticname TEXT, " + "office_locantion TEXT, " + "typelabel TEXT )";


    public static final String TABLE_NAME_GROUPNAMES_LIST = "group_names_list_table";
    public static final String CREATE_GROUPNAMES_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_GROUPNAMES_LIST + "( " + "checksum INTEGER, " + "group_names TEXT )";


    public static final String TABLE_NAME_GROUPROWID_LIST = "group_rowids_list_table";
    public static final String CREATE_GROUPROWID_LIST_TABLE = "CREATE TABLE if not exists " + TABLE_NAME_GROUPROWID_LIST + "( " + "checksum INTEGER, " + "group_rowids TEXT )";


}