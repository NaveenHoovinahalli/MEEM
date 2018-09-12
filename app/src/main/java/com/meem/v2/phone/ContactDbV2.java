package com.meem.v2.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Base64;

import com.meem.phone.MeemSqlDBManager;
import com.meem.utils.GenUtils;

import java.util.ArrayList;

/**
 * Created by Karthik B S on 3/4/17.
 */
public class ContactDbV2 extends MeemSqlDBManager {
    private String tag = "ContactsDbV2";
    private ContactsV2.ContactInfo mCurrentItem;
    private boolean abortFlag = false;

    public ContactDbV2(Context context, String dbPath, ArrayList<String> tableName) {
        super(context, dbPath, tableName);
        dbgTrace();
    }

    // individual restore related contructor

    public ContactDbV2(Context context, String dbPath) {
        super(context, dbPath);
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

    private boolean isContactPresentForDisplayname(String name){
        boolean isPresent = false;

        SQLiteDatabase db = this.getReadableDatabase();
        String table = ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE;
        String selection = "displayname =?";
        String[] selectionArgs = { name }; // matched to "?" in selection
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null);

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

    private int getContactCsumForDisplayname(String name) {
        boolean isPresent = false;

        SQLiteDatabase db = this.getReadableDatabase();
        String table = ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE;
        String[] columnsToReturn = { "checksum" };
        String selection = "displayname =?";
        String[] selectionArgs = { name }; // matched to "?" in selection
        Cursor cursor = db.query(table, columnsToReturn, selection, selectionArgs, null, null, null);

        if (null == cursor) {
            return 0;
        }

        int item=0;
        if (cursor.moveToNext()) {
            item = cursor.getInt(0);
        }


        if (null != cursor) {
            cursor.close();
        }

        db.close();
        return item;
    }

    private boolean isDuplicateCsumContactPresentForMainChecksum(int maincsum, int dupcsum){
        boolean isPresent = false;

        SQLiteDatabase db = this.getReadableDatabase();
        String table = ContactsTableNamesV2.TABLE_NAME_DUPLICATES_MAPPING_CONTACTS;
        String selection = "checksum =? and dupchecksum =?";
        String[] selectionArgs = { String.valueOf(maincsum) ,String.valueOf(dupcsum) }; // matched to "?" in selection
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null);

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

    public boolean addToDuplicateTableForMainChecksum(int maincsum,int dupcsum) {
        dbgTrace();
        ContentValues values = new ContentValues();

        values.put("checksum", (int) maincsum);
        values.put("dupchecksum", (int) dupcsum);

        return add(values, ContactsTableNamesV2.TABLE_NAME_DUPLICATES_MAPPING_CONTACTS);
    }

    public boolean addToSqlDataBase(ContactsV2.ContactInfo currItem) {
        dbgTrace();
        mCurrentItem = currItem;
        boolean ret = true;

        if (isContactForCSM(mCurrentItem.checksum))
            return true;

        // Check if contact for same display name present, if present merge with existing one. Else add as new contact.
        dbgTrace("Inserting or merging contact");

        if (isContactPresentForDisplayname(currItem.displayName)){
            // Contact with same name is present
            dbgTrace("Merging with existing contact for displayname: "+currItem.displayName);
            // Before merging Check with duplicates checksum table
            // If checksum is already present in duplicates table dont add or else add.

            // Get the checksum of existing contact to merge with
            int csum=getContactCsumForDisplayname(currItem.displayName);
            dbgTrace("getContactCsumForDisplayname: "+csum);

            if (csum==0){
                dbgTrace("checksum is zero, something wrong");
                return true; // something wrong
            }

            boolean isCsumAlreadyPresentInDupDb=isDuplicateCsumContactPresentForMainChecksum(csum ,currItem.checksum);

            if (isCsumAlreadyPresentInDupDb) {
                return true; // already present in db as duplicate
            }else{
                // add to checksum to duplicate table
                addToDuplicateTableForMainChecksum(csum ,currItem.checksum);
            }

            // merge with existing one
            if (csum!=0) {
                currItem.checksum=csum;  // replace checksum with main checksum
            }else{
                dbgTrace("checksum is nil for existing contacts, something is wrong");
                return true;  // This case shouldnt come.
            }

            if (!addEmail_list_table()) {
                dbgTrace(" Adding  Email_list_table failed  ");
                ret = false;
            }

            if (!addPhone_list_table()) {
                dbgTrace(" Adding  Phone_list_table failed  ");
                ret = false;
            }

            if (!addIm_list_table()) {
                dbgTrace(" Adding  Im_list_table failed  ");
                ret = false;
            }

            if (!addEvent_list_table()) {
                dbgTrace(" Adding  Event_list_table failed  ");
                ret = false;
            }

            if (!addWebsite_list_table()) {
                dbgTrace(" Adding  Website_list_table failed  ");
                ret = false;
            }

            if (!addPostaladdress_list_table()) {
                dbgTrace(" Adding  Postaladdress_list_table failed  ");
                ret = false;
            }

            if (!addRelationship_list_table()) {
                dbgTrace(" Adding  Relationship_list_table failed  ");
                ret = false;
            }

            if (!addOrganization_list_table()) {
                dbgTrace(" Adding  Organization_list_table failed  ");
                ret = false;
            }

//            if (!addGroup_names_list_table()) {
//                dbgTrace(" Adding  Group_names_list_table failed  ");
//                ret = false;
//            }
//
//            if (!addGroup_rowids()) {
//                dbgTrace(" Adding  Group_rowids failed  ");
//                ret = false;
//            }

        }else{
            ContentValues values = new ContentValues();
            values.put("checksum", (int) mCurrentItem.checksum);
            values.put("firstname", mCurrentItem.firstName);
            values.put("lastname", mCurrentItem.lastName);
            if (mCurrentItem.getPhoneNumberList().size() > 0) {
                values.put("number", mCurrentItem.getPhoneNumberList().get(0).getNumber());
            }


            if (!add(values, ContactsTableNamesV2.TABLE_NAME_VAULT_CONTACTS)) {
                dbgTrace("Inserting to vault_contacts_table Failed");
                return false;
            } else {
                int linkcount;
                linkcount = getLinkCountforCsum(mCurrentItem.checksum);
                dbgTrace("link count " + linkcount);
                if (linkcount == 0) {
                    if (addnewLink(mCurrentItem.checksum)) {

                        if (!addContacts_table()) {
                            dbgTrace(" Adding  Contacts_table failed  ");
                            ret = false;
                        }

                        if (!addEmail_list_table()) {
                            dbgTrace(" Adding  Email_list_table failed  ");
                            ret = false;
                        }

                        if (!addPhone_list_table()) {
                            dbgTrace(" Adding  Phone_list_table failed  ");
                            ret = false;
                        }

                        if (!addIm_list_table()) {
                            dbgTrace(" Adding  Im_list_table failed  ");
                            ret = false;
                        }

                        if (!addEvent_list_table()) {
                            dbgTrace(" Adding  Event_list_table failed  ");
                            ret = false;
                        }

                        if (!addWebsite_list_table()) {
                            dbgTrace(" Adding  Website_list_table failed  ");
                            ret = false;
                        }

                        if (!addPostaladdress_list_table()) {
                            dbgTrace(" Adding  Postaladdress_list_table failed  ");
                            ret = false;
                        }

                        if (!addRelationship_list_table()) {
                            dbgTrace(" Adding  Relationship_list_table failed  ");
                            ret = false;
                        }

                        if (!addOrganization_list_table()) {
                            dbgTrace(" Adding  Organization_list_table failed  ");
                            ret = false;
                        }

                        if (!addGroup_names_list_table()) {
                            dbgTrace(" Adding  Group_names_list_table failed  ");
                            ret = false;
                        }

                        if (!addGroup_rowids()) {
                            dbgTrace(" Adding  Group_rowids failed  ");
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
        }


        return ret;
    }

    public boolean addLinks_count_table(int links) {
        dbgTrace();
        ContentValues values = new ContentValues();

        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("linkscount", (int) links);

        return add(values, ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE);
    }

    public boolean addVault_contacts_table() {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("firstname", mCurrentItem.firstName);
        values.put("lastname", mCurrentItem.lastName);
        values.put("number", mCurrentItem.getPhoneNumberList().get(0).getNumber());


        return add(values, ContactsTableNamesV2.TABLE_NAME_VAULT_CONTACTS);
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


        return add(values, ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE);

    }

    private boolean addEmail_list_table() {
        dbgTrace();

        ArrayList<ContactsV2.EmailList> emailList = mCurrentItem.getEmailList();
        int totalEmailList = emailList.size();

        for (int i = 0; i < totalEmailList; i++) {

            ContactsV2.EmailList email = emailList.get(i);

            ////////
            boolean isPresent = false;
            SQLiteDatabase db = this.getReadableDatabase();

            String emailid=email.getEmailID();
            if (emailid==null){
                emailid="com";
            }
            // arun: 13feb2017: changing to parameterized query which is the recommended way to handle special characters in arguments
            // String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST + " where checksum = "+(int)mCurrentItem.checksum+" and email_id = '" + emailid + "' ";
            // Cursor cursor = db.rawQuery(sqlStmt, null);
            String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST + " where checksum = ? and email_id = ?";
            Cursor cursor = db.rawQuery(sqlStmt, new String[] { String.valueOf((int)mCurrentItem.checksum), emailid });
            if (null == cursor) {
                isPresent = false;
            }

            if (cursor.getCount() > 0)
                isPresent = true;

            if (null == cursor) {
                cursor.close();
            }

            db.close();

            ////////
            if (!isPresent) {
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


                if (!add(values, ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST)) {
                    return false;
                }
            }

        }

        return true;
    }

    private boolean addPhone_list_table() {
        dbgTrace();

        ArrayList<ContactsV2.PhoneNumberList> phoneNumberBeans = mCurrentItem.getPhoneNumberList();
        int totalPhoneNumbers = phoneNumberBeans.size();
        for (int i = 0; i < totalPhoneNumbers; i++) {

            ContactsV2.PhoneNumberList numberBean = phoneNumberBeans.get(i);

            ////////
            boolean isPresent = false;
            SQLiteDatabase db = this.getReadableDatabase();

            String num = numberBean.getNumber();
            if (0 != num.length()) {
                String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_PHONE_LIST + " where checksum = ? and number = ?";
                Cursor cursor = db.rawQuery(sqlStmt, new String[] { String.valueOf((int)mCurrentItem.checksum), String.valueOf(num) });
                if (null == cursor) {
                    isPresent = false;
                }

                if (cursor.getCount() > 0)
                    isPresent = true;

                if (null == cursor) {
                    cursor.close();
                }
            }

            db.close();

            ////////
            if (!isPresent) {
                ContentValues values = new ContentValues();
                values.put("checksum", (int) mCurrentItem.checksum);

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

                if (!add(values, ContactsTableNamesV2.TABLE_NAME_PHONE_LIST)) {
                    return false;
                }
            }

        }

        return true;
    }

    private boolean addIm_list_table() {
        dbgTrace();


        ArrayList<ContactsV2.IMList> imBeans = mCurrentItem.getImList();
        int totalIMBeans = imBeans.size();
        for (int i = 0; i < totalIMBeans; i++) {
            ContactsV2.IMList imBean = imBeans.get(i);
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

            if (!add(values, ContactsTableNamesV2.TABLE_NAME_IM_LIST)) {
                return false;
            }

        }

        return true;
    }

    private boolean addEvent_list_table() {
        dbgTrace();
        ArrayList<ContactsV2.EventList> eventList = mCurrentItem.getEventList();
        int totalEventLists = eventList.size();
        for (int i = 0; i < totalEventLists; i++) {

            ContactsV2.EventList eventBean = eventList.get(i);
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

            if (!add(values, ContactsTableNamesV2.TABLE_NAME_EVENT_LIST)) {
                return false;
            }

        }
        return true;
    }

    private boolean addWebsite_list_table() {
        dbgTrace();
        ArrayList<ContactsV2.WebsitesList> currWebsiteList = mCurrentItem.getWebsiteList();
        for (int i = 0; i < currWebsiteList.size(); i++) {
            ContactsV2.WebsitesList websiteBean = currWebsiteList.get(i);

            ////////
            boolean isPresent = false;
            SQLiteDatabase db = this.getReadableDatabase();

            String websitename = websiteBean.getWebsite();
            if (0 != websitename.length()) {

                String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_WEBSITE_LIST + " where checksum = ? and website = ?";
                Cursor cursor = db.rawQuery(sqlStmt, new String[] { String.valueOf((int)mCurrentItem.checksum), websitename });
                if (null == cursor) {
                    isPresent = false;
                }

                if (cursor.getCount() > 0)
                    isPresent = true;

                if (null == cursor) {
                    cursor.close();
                }
            }

            db.close();

            ////////
            if (!isPresent) {
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

                if (!add(values, ContactsTableNamesV2.TABLE_NAME_WEBSITE_LIST)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addPostaladdress_list_table() {
        dbgTrace();
        ArrayList<ContactsV2.PostalAddresses> poBeans = mCurrentItem.getPostalAddresses();
        int totalAddresses = poBeans.size();
        for (int i = 0; i < totalAddresses; i++) {
            ContactsV2.PostalAddresses poBean = poBeans.get(i);

            ////////
            boolean isPresent = false;
            SQLiteDatabase db = this.getReadableDatabase();

            String postaladdressname = poBean.getAddress();
            if (0 != postaladdressname.length()) {

                String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_POSTALADDRESS_LIST + " where checksum = ? and address = ?";
                Cursor cursor = db.rawQuery(sqlStmt, new String[] { String.valueOf((int)mCurrentItem.checksum), postaladdressname });

                if (null == cursor) {
                    isPresent = false;
                }

                if (cursor.getCount() > 0)
                    isPresent = true;

                if (null == cursor) {
                    cursor.close();
                }
            }

            db.close();

            ////////
            if (!isPresent) {
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

                if (!add(values, ContactsTableNamesV2.TABLE_NAME_POSTALADDRESS_LIST)) {
                    return false;
                }

            }

        }
        return true;
    }

    private boolean addRelationship_list_table() {
        dbgTrace();
        ArrayList<ContactsV2.Relationship> relationshipBean = mCurrentItem.getRelationship();
        for (int i = 0; i < relationshipBean.size(); i++) {
            ContactsV2.Relationship relationBean = relationshipBean.get(i);
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

            if (!add(values, ContactsTableNamesV2.TABLE_NAME_RELATIONSHIP_LIST)) {
                return false;
            }

        }
        return true;
    }

    private boolean addOrganization_list_table() {
        dbgTrace();
        ArrayList<ContactsV2.OrganizationList> main_organizationBean = mCurrentItem.getOrganizationList();
        for (int i = 0; i < main_organizationBean.size(); i++) {

            ContactsV2.OrganizationList organizationBean = main_organizationBean.get(i);
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

            if (!add(values, ContactsTableNamesV2.TABLE_NAME_ORGANIZATION_LIST)) {
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
            if (!add(values, ContactsTableNamesV2.TABLE_NAME_GROUPNAMES_LIST)) {
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
            if (!add(values, ContactsTableNamesV2.TABLE_NAME_GROUPROWID_LIST)) {
                return false;
            }

        }


        return true;
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("ContactsSqlDbV2.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("ContactsSqlDbV2.log", trace);
    }

    private int getLinkCountforCsum(int checkSum) {
        dbgTrace();

        String querry = "select * from " + ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE + " where checksum = " + checkSum;
        return rawQuerryGetInt(querry, 1);
    }

    private boolean addnewLink(int checkSum) {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("checksum", (int) checkSum);
        values.put("linkscount", (int) 1);

        return add(values, ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE);
    }

    private boolean addLinkscount(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE +
                " set linkscount = linkscount + 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean decrementLinkscountForChecksum(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE +
                " set linkscount = linkscount - 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    /**
     * Get DB Methods
     */
    public ContactsV2.ContactInfo getContactForChecksum(int cSum) {
        dbgTrace("getContactForChecksum :"+cSum);
        ContactsV2.ContactInfo currDbItem = new ContactsV2.ContactInfo();
        currDbItem.groupNames = new ArrayList<String>();
        currDbItem.groupRowIds = new ArrayList<String>();


        currDbItem.checksum = cSum;

        if (!getFromContactsTableForChecksum(currDbItem)) {
            dbgTrace("getFromContactsTableForChecksum Failed ");
        }


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


    /**
     * Get DB Methods
     */
    public ContactsV2.ContactInfo getContactForRow(int row) {
        ContactsV2.ContactInfo currDbItem = new ContactsV2.ContactInfo();
        currDbItem.groupNames = new ArrayList<String>();
        currDbItem.groupRowIds = new ArrayList<String>();


        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE + " LIMIT 1 OFFSET " + row;
        currDbItem.checksum = rawQuerryGetInt(sqlStmt, 0);

        if (!getFromContactsTableForChecksum(currDbItem)) {
            dbgTrace("getFromContactsTableForChecksum Failed ");
        }


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


    private boolean getFromContactsTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE + " where checksum = " + currItem.checksum;

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

            if (null != cursor.getString(13)) { // Image if not null
                currItem.photoBitmap = str64TobyteArr(cursor.getString(13));
                //currItem.photoBitmap = cursor.getBlob(13);
            }

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
            if (null == currItem.customRingtone) {
                currItem.customRingtone = "";
            }
            currItem.isStarred = cursor.getString(23);
            if (null == currItem.isStarred) {
                currItem.isStarred = "";
            }


            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("1.Exception: " + GenUtils.getStackTrace(ex));

        }

        return result;
    }


    private boolean getFromEmailListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.EmailList dbEmailList = new ContactsV2.EmailList();

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
            dbgTrace("2.Exception: " + ex.getMessage());
        }

        return result;
    }

    private boolean getFromPhoneListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_PHONE_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.PhoneNumberList dbPhnumList = new ContactsV2.PhoneNumberList();

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
            dbgTrace("3.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFromEventListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_EVENT_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.EventList dbEventList = new ContactsV2.EventList();


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
            dbgTrace("4.Exception: " + ex.getMessage());
        }

        return result;

    }

    private boolean getFromImListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_IM_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.IMList dbIMList = new ContactsV2.IMList();

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
            dbgTrace("5.Exception: " + ex.getMessage());
        }

        return result;

    }

    private boolean getFromWebsiteListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_WEBSITE_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.WebsitesList dbWebsiteList = new ContactsV2.WebsitesList();

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
            dbgTrace("6.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFromPostalAddListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_POSTALADDRESS_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.PostalAddresses dbPostalAddrList = new ContactsV2.PostalAddresses();

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
            dbgTrace("7.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFromRelationListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_RELATIONSHIP_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.Relationship dbRealationshipList = new ContactsV2.Relationship();


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
            dbgTrace("8.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFromOrgListTableForChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_ORGANIZATION_LIST + " where checksum = " + currItem.checksum;


        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                ContactsV2.OrganizationList dbOrganizationList = new ContactsV2.OrganizationList();


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
            dbgTrace("9.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFormGroupNamesTableforChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_GROUPNAMES_LIST + " where checksum = " + currItem.checksum;


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
            dbgTrace("10.Exception: " + ex.getMessage());
        }

        return result;

    }


    private boolean getFormGroupRowIdTableforChecksum(ContactsV2.ContactInfo currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + ContactsTableNamesV2.TABLE_NAME_GROUPROWID_LIST + " where checksum = " + currItem.checksum;


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
            dbgTrace("11.Exception: " + ex.getMessage());
        }

        return result;

    }


    public boolean deleteAll() {
        boolean result = true;

        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_VAULT_CONTACTS);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_PHONE_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_IM_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_EVENT_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_WEBSITE_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_POSTALADDRESS_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_ORGANIZATION_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_GROUPNAMES_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_GROUPROWID_LIST);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_DUPLICATES_MAPPING_CONTACTS);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }
        return result;
    }


    public boolean deleteRowForChecksum(int cSum) {
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

            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_LINKS_COUNT_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_VAULT_CONTACTS + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_EMAIL_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_PHONE_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_IM_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_EVENT_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_WEBSITE_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_POSTALADDRESS_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_ORGANIZATION_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_GROUPNAMES_LIST + " where checksum = " + cSum);
            db.execSQL("delete from " + ContactsTableNamesV2.TABLE_NAME_GROUPROWID_LIST + " where checksum = " + cSum);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("1.SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public boolean deleteAllContacts() {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete  from " + ContactsTableNamesV2.TABLE_NAME_VAULT_CONTACTS);
            db.close();
        } catch (SQLException ex) {
            dbgTrace("2.SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }

    public int getRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlStmt = "Select count(*) from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE;
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

    public boolean isContactForCSM(int csm) {
        boolean isPresent = false;
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlStmt = "Select * from " + ContactsTableNamesV2.TABLE_NAME_CONTACTS_TABLE + " where checksum = " + csm + " ";
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

    public int[]  getDupCsumArrayForMainCsumInDupMappTable(int csum){

        SQLiteDatabase db = this.getReadableDatabase();
        String table = ContactsTableNamesV2.TABLE_NAME_DUPLICATES_MAPPING_CONTACTS;
        String[] columnsToReturn = { "dupchecksum" };
        String selection = "checksum =?";
        String[] selectionArgs = { String.valueOf(csum)  }; // matched to "?" in selection
        Cursor cursor = db.query(table, columnsToReturn, selection, selectionArgs, null, null, null);


        int item = 0;
        if (null == cursor) {
            return new int[0];
        }

        int[] duparray = new int[cursor.getCount()];
        int index=0;
        while (cursor.moveToNext()) {
            item = cursor.getInt(0);
            duparray[index]=item;
            index++;
        }


        if (null == cursor) {
            cursor.close();
        }

        db.close();

        return duparray;
    }
}
