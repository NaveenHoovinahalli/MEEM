package com.meem.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.meem.utils.GenUtils;

import java.util.ArrayList;

/**
 * Created by hercules on 23/8/16.
 */
public class ContactsDb extends MeemSqlDBManager {
    private String tag = "ContactsDb";
    private Contacts.ContactInfo mCurrentItem;
    private String mUpid;
    private boolean abortFlag = false;

    public ContactsDb(Context context, String upid, String dbPath, ArrayList<String> tableName) {
        super(context, dbPath, tableName);
        mUpid = upid;
        dbgTrace();
    }

    // individual restore related contructor

    public ContactsDb(Context context, String dbPath) {
        super(context, dbPath);
        dbgTrace();
    }

    // To delete All Entries
    public ContactsDb(Context context, String dbPath, String upid) {
        super(context, dbPath);
        mUpid = upid;
        dbgTrace();
    }

    /**
     * Encodes the byte array into base64 string
     *
     * @param imageByteArray - byte array
     *
     * @return String a {@link String}
     */
    private static String byteArrToStr64(byte[] imageByteArray) {
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
    }

    /**
     * Decodes the base64 string into byte array
     *
     * @param imageDataString - a {@link String}
     *
     * @return byte array
     */
    private static byte[] str64TobyteArr(String imageDataString) {
        return Base64.decode(imageDataString, Base64.DEFAULT);
    }

    public void addToSqlDataBase(Contacts.ContactInfo currItem, int isMirr) {
        dbgTrace();
        mCurrentItem = currItem;


        ContentValues values = new ContentValues();
        values.put("upid", mUpid);
        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("ismirr", (int) isMirr);


        if (!add(values, ContactsTableNames.TABLE_NAME_VAULT_CONTACTS)) {
            dbgTrace("Inserting to vault_contacts_table Failed");
            return;
        } else {
            int linkcount;
            linkcount = getLinkCountforCsum(mCurrentItem.checksum);
            dbgTrace("link count " + linkcount);
            if (linkcount == 0) {
                if (addnewLink(mCurrentItem.checksum)) {

                    if (!addContacts_table()) dbgTrace(" Adding  Contacts_table failed  ");

                    if (!addEmail_list_table()) dbgTrace(" Adding  Email_list_table failed  ");

                    if (!addPhone_list_table()) dbgTrace(" Adding  Phone_list_table failed  ");

                    if (!addIm_list_table()) dbgTrace(" Adding  Im_list_table failed  ");

                    if (!addEvent_list_table()) dbgTrace(" Adding  Event_list_table failed  ");

                    if (!addWebsite_list_table()) dbgTrace(" Adding  Website_list_table failed  ");

                    if (!addPostaladdress_list_table()) dbgTrace(" Adding  Postaladdress_list_table failed  ");

                    if (!addRelationship_list_table()) dbgTrace(" Adding  Relationship_list_table failed  ");

                    if (!addOrganization_list_table()) dbgTrace(" Adding  Organization_list_table failed  ");

                    if (!addGroup_names_list_table()) dbgTrace(" Adding  Group_names_list_table failed  ");

                    if (!addGroup_rowids()) dbgTrace(" Adding  Group_rowids failed  ");

                } else {
                    dbgTrace("adding new link to Links table failed ");
                    return;
                }
            } else if (linkcount > 0) {
                        /* if links count is less more than  */
                if (!addLinkscount(mCurrentItem.checksum)) dbgTrace(" Adding addLinkscount failed  ");
            }
        }


    }

    public boolean addLinks_count_table(int links) {
        dbgTrace();
        ContentValues values = new ContentValues();

        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("linkscount", (int) links);

        return add(values, ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE);
    }

    public boolean addVault_contacts_table(int isMirr) {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("upid", mUpid);
        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("ismirr", (int) isMirr);

        return add(values, ContactsTableNames.TABLE_NAME_VAULT_CONTACTS);
    }

    private boolean addContacts_table() {
        dbgTrace();
        ContentValues values = new ContentValues();

        values.put("checksum", (int) mCurrentItem.checksum);
        String item = mCurrentItem.contactId;
        if (0 != item.length()) {
            values.put("contactid", item);
        }
        item = mCurrentItem.note;
        if (0 != item.length()) {
            values.put("note", item);
        }
        item = mCurrentItem.displayName;
        if (0 != item.length()) {
            values.put("displayname", item);
        }
        item = mCurrentItem.namePrefix;
        if (0 != item.length()) {
            values.put("name_prefix", item);
        }
        item = mCurrentItem.nameSuffix;
        if (0 != item.length()) {
            values.put("name_suffix", item);
        }
        item = mCurrentItem.phoneticFname;
        if (0 != item.length()) {
            values.put("phonetic_fname", item);
        }
        item = mCurrentItem.phoneticMname;
        if (0 != item.length()) {
            values.put("phonetic_mname", item);
        }
        item = mCurrentItem.phoneticGname;
        if (0 != item.length()) {
            values.put("phonetic_gname", item);
        }
        item = mCurrentItem.birthday;
        if (0 != item.length()) {
            values.put("birthday", item);
        }
        item = mCurrentItem.firstName;
        if (0 != item.length()) {
            values.put("firstname", item);
        }
        item = mCurrentItem.lastName;
        if (0 != item.length()) {
            values.put("lastname", item);
        }
        if (null != mCurrentItem.photoBitmap) {
            item = byteArrToStr64(mCurrentItem.photoBitmap);
            if (0 != mCurrentItem.photoBitmap.length) values.put("photobitmap", item);
        }
        item = mCurrentItem.nickname;
        if (0 != item.length()) {
            values.put("nickname", item);
        }


		/*Android specific Fields*/
        item = mCurrentItem.nickname;
        if (0 != item.length()) {
            values.put("nickname", item);
        }
        item = mCurrentItem.accountName;
        if (0 != item.length()) {
            values.put("account_name", item);
        }
        item = mCurrentItem.source;
        if (0 != item.length()) {
            values.put("source", item);
        }

        int iValue = mCurrentItem.aggregationMode;
        values.put("aggregation_mode", (int) iValue);

        item = mCurrentItem.groupSourceId;
        if (0 != item.length()) {
            values.put("group_source_id", item);
        }

        iValue = mCurrentItem.timesContacted;
        values.put("times_contacted", (int) iValue);

        long lValue = mCurrentItem.lastTimeContacted;
        values.put("last_time_contacted", (int) lValue);

        iValue = mCurrentItem.sendToVoiceMail;
        values.put("send_to_voicemail", iValue);

        item = mCurrentItem.customRingtone;
        if (0 != item.length()) {
            values.put("custom_ringtone", item);
        }

        item = mCurrentItem.isStarred;
        if (0 != item.length()) {
            values.put("is_starred", item);
        }


        return add(values, ContactsTableNames.TABLE_NAME_CONTACTS_TABLE);

    }

    private boolean addEmail_list_table() {
        dbgTrace();

        ArrayList<Contacts.EmailList> emailList = mCurrentItem.getEmailList();
        int totalEmailList = emailList.size();

        for (int i = 0; i < totalEmailList; i++) {

            Contacts.EmailList email = emailList.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            String item = email.getEmailID();
            if (0 != item.length()) {
                values.put("email_id", item);
            }

            item = email.getEmailLabel();
            if (0 != item.length()) {
                values.put("email_label", item);
            }

            int iValue = email.getEmailType();
            values.put("email_type", (int) iValue);


            if (!add(values, ContactsTableNames.TABLE_NAME_EMAIL_LIST)) {
                return false;
            }

        }

        return true;
    }

    private boolean addPhone_list_table() {
        dbgTrace();

        ArrayList<Contacts.PhoneNumberList> phoneNumberBeans = mCurrentItem.getPhoneNumberList();
        int totalPhoneNumbers = phoneNumberBeans.size();
        for (int i = 0; i < totalPhoneNumbers; i++) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            Contacts.PhoneNumberList numberBean = phoneNumberBeans.get(i);

            String item = numberBean.getNumber();
            if (0 != item.length()) {
                values.put("number", item);
            }

            item = numberBean.getLabel();
            if (0 != item.length()) {
                values.put("label", item);
            }

            int iValue = numberBean.getType();
            values.put("type", (int) iValue);

            if (!add(values, ContactsTableNames.TABLE_NAME_PHONE_LIST)) {
                return false;
            }

        }

        return true;
    }

    private boolean addIm_list_table() {
        dbgTrace();


        ArrayList<Contacts.IMList> imBeans = mCurrentItem.getImList();
        int totalIMBeans = imBeans.size();
        for (int i = 0; i < totalIMBeans; i++) {
            Contacts.IMList imBean = imBeans.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = imBean.getImType();
            values.put("imtype", (int) iValue);

            String item = imBean.getCustomType();
            if (0 != item.length()) {
                values.put("customtype", item);
            }

            item = imBean.getImProtocol();
            if (0 != item.length()) {
                values.put("improtocol", item);
            }

            item = imBean.getCustomProtocol();
            if (0 != item.length()) {
                values.put("customprotocol", item);
            }

            item = imBean.getImAddress();
            if (0 != item.length()) {
                values.put("imaddress", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_IM_LIST)) {
                return false;
            }

        }

        return true;
    }

    private boolean addEvent_list_table() {
        dbgTrace();
        ArrayList<Contacts.EventList> eventList = mCurrentItem.getEventList();
        int totalEventLists = eventList.size();
        for (int i = 0; i < totalEventLists; i++) {

            Contacts.EventList eventBean = eventList.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = eventBean.getEtype();
            values.put("etype", (int) iValue);

            String item = eventBean.getStartDate();
            if (0 != item.length()) {
                values.put("startdate", item);
            }
            item = eventBean.getEventLabel();
            if (0 != item.length()) {
                values.put("eventlabel", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_EVENT_LIST)) {
                return false;
            }

        }
        return true;
    }

    private boolean addWebsite_list_table() {
        dbgTrace();
        ArrayList<Contacts.WebsitesList> currWebsiteList = mCurrentItem.getWebsiteList();
        for (int i = 0; i < currWebsiteList.size(); i++) {
            Contacts.WebsitesList websiteBean = currWebsiteList.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = websiteBean.getWebsiteType();
            values.put("websitetype", (int) iValue);

            String item = websiteBean.getWebsite();
            if (0 != item.length()) {
                values.put("website", item);
            }
            item = websiteBean.getWebsiteLabel();
            if (0 != item.length()) {
                values.put("websitelabel", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_WEBSITE_LIST)) {
                return false;
            }


        }
        return true;
    }

    private boolean addPostaladdress_list_table() {
        dbgTrace();
        ArrayList<Contacts.PostalAddresses> poBeans = mCurrentItem.getPostalAddresses();
        int totalAddresses = poBeans.size();
        for (int i = 0; i < totalAddresses; i++) {
            Contacts.PostalAddresses poBean = poBeans.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = poBean.getType();
            values.put("type", (int) iValue);

            String item = poBean.getAddress();
            if (0 != item.length()) {
                values.put("address", item);
            }

            item = poBean.getStreet();
            if (0 != item.length()) {
                values.put("street", item);
            }
            item = poBean.getCity();
            if (0 != item.length()) {
                values.put("city", item);
            }
            item = poBean.getZipcode();
            if (0 != item.length()) {
                values.put("zipcode", item);
            }
            item = poBean.getCountry();
            if (0 != item.length()) {
                values.put("country", item);
            }

            item = poBean.getCountry();
            if (0 != item.length()) {
                values.put("country", item);
            }
            item = poBean.getCountryCode();
            if (0 != item.length()) {
                values.put("countrycode", item);
            }
            item = poBean.getRegion();
            if (0 != item.length()) {
                values.put("region", item);
            }
            item = poBean.getNeighbourhood();
            if (0 != item.length()) {
                values.put("neighbourhood", item);
            }
            item = poBean.getLabel();
            if (0 != item.length()) {
                values.put("label", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_POSTALADDRESS_LIST)) {
                return false;
            }


        }
        return true;
    }

    private boolean addRelationship_list_table() {
        dbgTrace();
        ArrayList<Contacts.Relationship> relationshipBean = mCurrentItem.getRelationship();
        for (int i = 0; i < relationshipBean.size(); i++) {
            Contacts.Relationship relationBean = relationshipBean.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = relationBean.getRelationType();
            values.put("relationtype", (int) iValue);

            String item = relationBean.getRelationName();
            if (0 != item.length()) {
                values.put("relationname", item);
            }

            item = relationBean.getRelationLabel();
            if (0 != item.length()) {
                values.put("relationlabel", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_RELATIONSHIP_LIST)) {
                return false;
            }

        }
        return true;
    }

    private boolean addOrganization_list_table() {
        dbgTrace();
        ArrayList<Contacts.OrganizationList> main_organizationBean = mCurrentItem.getOrganizationList();
        for (int i = 0; i < main_organizationBean.size(); i++) {

            Contacts.OrganizationList organizationBean = main_organizationBean.get(i);
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);

            int iValue = organizationBean.getType();
            values.put("type", (int) iValue);

            String item = organizationBean.getOrganizationName();
            if (0 != item.length()) {
                values.put("organizationname", item);
            }

            item = organizationBean.getTitle();
            if (0 != item.length()) {
                values.put("title", item);
            }
            item = organizationBean.getJobDepartment();
            if (0 != item.length()) {
                values.put("jobdepartment", item);
            }
            item = organizationBean.getJobDescription();
            if (0 != item.length()) {
                values.put("jobdescription", item);
            }
            item = organizationBean.getOfficeSymbol();
            if (0 != item.length()) {
                values.put("officesymbol", item);
            }
            item = organizationBean.getOfficePhoneticName();
            if (0 != item.length()) {
                values.put("office_phoneticname", item);
            }
            item = organizationBean.getOfficeLocation();
            if (0 != item.length()) {
                values.put("office_locantion", item);
            }
            item = organizationBean.getTypeLabel();
            if (0 != item.length()) {
                values.put("typelabel", item);
            }

            if (!add(values, ContactsTableNames.TABLE_NAME_ORGANIZATION_LIST)) {
                return false;
            }

        }
        return true;
    }

    private boolean addGroup_names_list_table() {
        dbgTrace();
        ArrayList<String> groupNamesList = mCurrentItem.groupNames;
        for (String item : groupNamesList) {


            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);
            if (item != null) {
                if (0 != item.length()) {
                    values.put("group_names", item);
                }
            }
            if (!add(values, ContactsTableNames.TABLE_NAME_GROUPNAMES_LIST)) {
                return false;
            }

        }


        return true;
    }

    private boolean addGroup_rowids() {
        dbgTrace();
        ArrayList<String> groupRowIdsList = mCurrentItem.groupRowIds;
        for (String item : groupRowIdsList) {
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);
            if (item != null) {
                if (0 != item.length()) {
                    values.put("group_rowids", item);
                }
            }
            if (!add(values, ContactsTableNames.TABLE_NAME_GROUPROWID_LIST)) {
                return false;
            }

        }


        return true;
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("ContactsSqlDb.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("ContactsSqlDb.log", trace);
    }

    private int getLinkCountforCsum(int checkSum) {
        dbgTrace();

        String querry = "select * from " + ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE + " where checksum = " + checkSum;
        return rawQuerryGetInt(querry, 1);
    }

    private boolean addnewLink(int checkSum) {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("checksum", (int) checkSum);
        values.put("linkscount", (int) 1);

        return add(values, ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE);
    }

    private boolean addLinkscount(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE +
                " set linkscount = linkscount + 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean decrementLinkscountForChecksum(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE +
                " set linkscount = linkscount - 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }


    /**
     * Get DB Methods
     */
    public Contacts.ContactInfo getContactForChecksum(int cSum) {
        Contacts.ContactInfo currDbItem = new Contacts.ContactInfo();
        currDbItem.groupNames = new ArrayList<String>();
        currDbItem.groupRowIds = new ArrayList<String>();


//        String sqlStmt = "select * from "+ ContactsTableNames.TABLE_NAME_VAULT_CONTACTS +" where upid = "+ upid +
//        " and ismirr = "+ isMirr +" LIMIT 1 OFFSET "+ row ;
//        currDbItem.checksum = rawQuerryGetInt(sqlStmt, 1);

        currDbItem.checksum = cSum;
        if (!getFromContactsTableForChecksum(currDbItem)) {
            dbgTrace("getFromContactsTableForChecksum Failed ");
        }
        ;

        if (!getFromEmailListTableForChecksum(currDbItem)) {
            dbgTrace("getFromEmailListTableForChecksum Failed ");
        }
        ;

        if (!getFromPhoneListTableForChecksum(currDbItem)) {
            dbgTrace("getFromPhoneListTableForChecksum Failed ");
        }
        ;

        if (!getFromImListTableForChecksum(currDbItem)) {
            dbgTrace("getFromImListTableForChecksum Failed ");
        }
        ;

        if (!getFromEventListTableForChecksum(currDbItem)) {
            dbgTrace("getFromEventListTableForChecksum Failed ");
        }
        ;

        if (!getFromWebsiteListTableForChecksum(currDbItem)) {
            dbgTrace("getFromWebsiteListTableForChecksum Failed ");
        }
        ;

        if (!getFromPostalAddListTableForChecksum(currDbItem)) {
            dbgTrace("getFromPostalAddListTableForChecksum Failed ");
        }
        ;

        if (!getFromRelationListTableForChecksum(currDbItem)) {
            dbgTrace("getFromRelationListTableForChecksum Failed ");
        }
        ;

        if (!getFromOrgListTableForChecksum(currDbItem)) {
            dbgTrace("getFromOrgListTableForChecksum Failed ");
        }
        ;

        if (!getFormGroupNamesTableforChecksum(currDbItem)) {
            dbgTrace("getFormGroupNamesTableforChecksum Failed ");
        }
        ;

        if (!getFormGroupRowIdTableforChecksum(currDbItem)) {
            dbgTrace("getFormGroupRowIdTableforChecksum Failed ");
        }
        ;


        return currDbItem;
    }


    private boolean getFromContactsTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_CONTACTS_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            cursor.moveToNext();
            currItem.contactId = cursor.getString(1);
            if (null == currItem.contactId) {
                currItem.contactId = "";
            }


            currItem.note = cursor.getString(2);
            if (null == currItem.note) {
                currItem.note = "";
            }

            currItem.displayName = cursor.getString(3);
            if (null == currItem.displayName) {
                currItem.displayName = "";
            }

            currItem.namePrefix = cursor.getString(4);
            if (null == currItem.namePrefix) {
                currItem.namePrefix = "";
            }

            currItem.nameSuffix = cursor.getString(5);
            if (null == currItem.nameSuffix) {
                currItem.nameSuffix = "";
            }

            currItem.phoneticFname = cursor.getString(6);
            if (null == currItem.phoneticFname) {
                currItem.phoneticFname = "";
            }

            currItem.phoneticMname = cursor.getString(7);
            if (null == currItem.phoneticMname) {
                currItem.phoneticMname = "";
            }

            currItem.phoneticGname = cursor.getString(8);
            if (null == currItem.phoneticGname) {
                currItem.phoneticGname = "";
            }

            currItem.birthday = cursor.getString(9);
            if (null == currItem.birthday) {
                currItem.birthday = "";
            }

            currItem.firstName = cursor.getString(10);
            if (null == currItem.firstName) {
                currItem.firstName = "";
            }

            currItem.lastName = cursor.getString(11);
            if (null == currItem.lastName) {
                currItem.lastName = "";
            }

            currItem.middleName = cursor.getString(12);
            if (null == currItem.middleName) {
                currItem.middleName = "";
            }

            currItem.photoBitmap = str64TobyteArr(cursor.getString(13));
            //currItem.photoBitmap = cursor.getBlob(13);

            currItem.nickname = cursor.getString(14);
            if (null == currItem.nickname) {
                currItem.nickname = "";
            }

            currItem.accountName = cursor.getString(15);
            if (null == currItem.accountName) {
                currItem.accountName = "";
            }

            currItem.source = cursor.getString(16);
            if (null == currItem.source) {
                currItem.source = "";
            }

            currItem.groupSourceId = cursor.getString(17);
            if (null == currItem.groupSourceId) {
                currItem.groupSourceId = "";
            }

            currItem.aggregationMode = cursor.getInt(18);
            currItem.timesContacted = cursor.getInt(19);
            currItem.lastTimeContacted = cursor.getInt(20);
            currItem.sendToVoiceMail = cursor.getInt(21);
            currItem.customRingtone = cursor.getString(22);
            currItem.isStarred = cursor.getString(23);


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


    private boolean getFromEmailListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_EMAIL_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.EmailList dbEmailList = new Contacts.EmailList();

                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                dbEmailList.setEmailID(item);
                dbEmailList.setEmailType(cursor.getInt(2));

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbEmailList.setEmailLabel(item);

                currItem.emailList.add(dbEmailList);
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

    private boolean getFromPhoneListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_PHONE_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.PhoneNumberList dbPhnumList = new Contacts.PhoneNumberList();

                dbPhnumList.setType(cursor.getInt(1));

                String item = cursor.getString(2);
                if (item == null) {
                    item = "";
                }
                dbPhnumList.setNumber(item);

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbPhnumList.setLabel(item);

                currItem.phoneNumberList.add(dbPhnumList);


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


    private boolean getFromEventListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_EVENT_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.EventList dbEventList = new Contacts.EventList();


                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                dbEventList.setStartDate(item);
                dbEventList.setEtype(cursor.getInt(2));

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbEventList.setEventLabel(item);

                currItem.eventList.add(dbEventList);

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

    private boolean getFromImListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_IM_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.IMList dbIMList = new Contacts.IMList();

                dbIMList.setImType(cursor.getInt(1));
                String item = cursor.getString(2);
                if (item == null) {
                    item = "";
                }
                dbIMList.setCustomType(item);
                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbIMList.setImProtocol(item);
                item = cursor.getString(4);
                if (item == null) {
                    item = "";
                }
                dbIMList.setCustomProtocol(item);
                item = cursor.getString(5);
                if (item == null) {
                    item = "";
                }
                dbIMList.setImAddress(item);

                currItem.imList.add(dbIMList);

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

    private boolean getFromWebsiteListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_WEBSITE_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.WebsitesList dbWebsiteList = new Contacts.WebsitesList();

                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                dbWebsiteList.setWebsite(item);
                dbWebsiteList.setWebsiteType(cursor.getInt(2));

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbWebsiteList.setWebsiteLabel(item);

                currItem.websiteList.add(dbWebsiteList);
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


    private boolean getFromPostalAddListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_POSTALADDRESS_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.PostalAddresses dbPostalAddrList = new Contacts.PostalAddresses();

                dbPostalAddrList.setType(cursor.getInt(1));

                String item = cursor.getString(2);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setAddress(item);

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setStreet(item);

                item = cursor.getString(4);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setCity(item);

                item = cursor.getString(5);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setZipcode(item);

                item = cursor.getString(6);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setCountry(item);

                item = cursor.getString(7);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setCountryCode(item);

                item = cursor.getString(8);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setRegion(item);

                item = cursor.getString(9);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setNeighbourhood(item);

                item = cursor.getString(10);
                if (item == null) {
                    item = "";
                }
                dbPostalAddrList.setLabel(item);

                currItem.postalAddresses.add(dbPostalAddrList);
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


    private boolean getFromRelationListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_RELATIONSHIP_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.Relationship dbRealationshipList = new Contacts.Relationship();


                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                dbRealationshipList.setRelationName(item);
                dbRealationshipList.setRelationType(cursor.getInt(2));

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbRealationshipList.setRelationLabel(item);

                currItem.relationship.add(dbRealationshipList);
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


    private boolean getFromOrgListTableForChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_ORGANIZATION_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                Contacts.OrganizationList dbOrganizationList = new Contacts.OrganizationList();


                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setOrganizationName(item);

                item = cursor.getString(2);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setTitle(item);

                item = cursor.getString(3);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setJobDepartment(item);

                dbOrganizationList.setType(cursor.getInt(4));

                item = cursor.getString(5);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setJobDescription(item);

                item = cursor.getString(6);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setOfficeSymbol(item);

                item = cursor.getString(7);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setOfficePhoneticName(item);

                item = cursor.getString(8);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setOfficeLocation(item);

                item = cursor.getString(9);
                if (item == null) {
                    item = "";
                }
                dbOrganizationList.setTypeLabel(item);

                currItem.organizationList.add(dbOrganizationList);
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


    private boolean getFormGroupNamesTableforChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_GROUPNAMES_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {

                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                currItem.groupNames.add(item);
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


    private boolean getFormGroupRowIdTableforChecksum(Contacts.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_GROUPROWID_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                String item = cursor.getString(1);
                if (item == null) {
                    item = "";
                }
                currItem.groupRowIds.add(item);
            }


            if (null == cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }

        return result;

    }


    public boolean deleteAll() {
        boolean result = true;

        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_VAULT_CONTACTS);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_CONTACTS_TABLE);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_EMAIL_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_PHONE_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_IM_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_EVENT_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_WEBSITE_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_POSTALADDRESS_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_ORGANIZATION_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_GROUPNAMES_LIST);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_GROUPROWID_LIST);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }
        return result;
    }


    public boolean deleteRowsForChecksum(int cSum) {
        int linksCount = getLinkCountforCsum(cSum);
        dbgTrace("deleteRowsForChecksum " + cSum + "Links Count " + linksCount);
        if (linksCount > 1) {
            // Decrement links count
            // TODO : Handling Duplication of contact of same upid


            dbgTrace("Subtract links Count for CSum " + cSum);
            if (!decrementLinkscountForChecksum(cSum)) {
                dbgTrace(" decrementLinkscountForChecksum Failed for csum " + cSum);
                return false;
            }


        } else {
            // Delete in All Tables
            dbgTrace("Delete in All Tables");
            if (!deleteRowInAllOtherTablesForChecksum(cSum)) {
                dbgTrace(" deleteRowInAllOtherTablesForChecksum Failed for csum " + cSum);
                return false;
            }
        }

        return true;
    }


    public boolean deleteRowInAllOtherTablesForChecksum(int cSum) {
        boolean result = true;

        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_LINKS_COUNT_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_CONTACTS_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_EMAIL_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_PHONE_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_IM_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_EVENT_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_WEBSITE_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_POSTALADDRESS_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_ORGANIZATION_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_GROUPNAMES_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_GROUPROWID_LIST + " where checksum = " + cSum);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    private boolean deleteRowsInVaultsTableForUpid(String upid, int isMirr) {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + ContactsTableNames.TABLE_NAME_VAULT_CONTACTS + " where upid = '" + upid + "' and ismirr = " + isMirr);
            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public boolean deleteContactsForUpid(String upid, int isMirr) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNames.TABLE_NAME_VAULT_CONTACTS + " where upid = '" + upid + "'" +
                " and ismirr = " + isMirr;
        try {
            ArrayList<Integer> cSumArr = new ArrayList<Integer>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext() && !abortFlag) {
                cSumArr.add(cursor.getInt(1));
            }
            if (null != cursor) {
                cursor.close();
            }
            db.close();

            for (int cSum : cSumArr) {
                if (!abortFlag) deleteRowsForChecksum(cSum);
                else break;
            }
            deleteRowsInVaultsTableForUpid(upid, isMirr);

        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }

        return result;
    }


    public boolean delteAllEntriesOfUpidInDb() {
        boolean res = true;

        if (!deleteContactsForUpid(mUpid, 0)) {
            dbgTrace("Failed! Delete Contacts of Upid " + mUpid + "Mirror Items");
            res = false;
        } else dbgTrace("Deleted All Contacts of Upid " + mUpid + "Mirror Items");

        if (!deleteContactsForUpid(mUpid, 1)) {
            dbgTrace("Failed! Delete Contacts of Upid " + mUpid + "MirrorPlus Items");
            res = false;
        } else dbgTrace("Deleted All Contacts of Upid " + mUpid + "MirrorPlus Items");


        return res;
    }

}
