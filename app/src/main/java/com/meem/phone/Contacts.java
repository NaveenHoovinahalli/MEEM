/*
 * ------------------------------------------------------------ Copyright (c)
 * 2014, Silvan Innovation Labs Pvt. Ltd. (http://www.silvanlabs.com). All
 * rights reserved.
 * 
 * Unauthorized distribution, redistribution or usage of this software in source
 * or binary forms are strictly prohibited.
 * ------------------------------------------------------------
 * 
 * Description: Implementation of Contacts backup and restore . 19-May-2014
 * 
 * @author KARTHIK.B.S [karthik.bs@silvanlabs.com]
 */

package com.meem.phone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.ContactTrackerDataBase;
import com.meem.androidapp.ContactTrackerWrapper;
import com.meem.androidapp.TrackedContact;
import com.meem.ui.SmartDataInfo;
import com.meem.utils.CRC32;
import com.meem.utils.GenUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Implementation of contacts backup and restore methods
 *
 * @author karthik B S
 */

public class Contacts extends SmartDataCategory {

    private static final String tag = "Contacts";
    ArrayList<TrackedContact> mTrackedContact;
    ContactTrackerWrapper mContactTrackingWrapper;
    ContactInfo mCurrentMirrorItem, mCurrentPlusItem, mCurrentPhoneItem;
    // Sql DB related
    private ContactsDb mContactsDb;
    private AppLocalData mAppData = AppLocalData.getInstance();


    public Contacts(Context context, String inMirr, String inPlus, String outMirr, String outPlus) {
        super(context, ContactsContract.RawContacts.CONTENT_URI, ContactsContract.RawContacts.DELETED + " = 0 ", inMirr, inPlus, outMirr, outPlus);

        dbgTrace();
        mContactTrackingWrapper = new ContactTrackerWrapper();

        mTrackedContact = mContactTrackingWrapper.getAllContact();


    }

    public Contacts(Context context, String inMirr, String inPlus, String upid) {
        super(context, inMirr, inPlus);
        mContext = context;
        ArrayList<String> mtableNamesList = new ArrayList<String>();
        mtableNamesList.add(ContactsTableNames.CREATE_VAULT_LINKS_COUNT_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_VAULT_CONTACTS_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_CONTACTS_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_EMAIL_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_PHONE_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_IM_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_EVENT_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_WEBSITE_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_SOCIAL_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_POSTALADDRESS_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_RELATIONSHIP_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_ORGANIZATION_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_GROUPNAMES_LIST_TABLE);
        mtableNamesList.add(ContactsTableNames.CREATE_GROUPROWID_LIST_TABLE);

        mContactsDb = new ContactsDb(context, upid, mAppData.getContactDbFullPath(), mtableNamesList);

        if (inMirr != null) {
            if (!mContactsDb.deleteContactsForUpid(upid, 0)) dbgTrace("deleteContactsForUpid Failed for upid" + upid + " For Mirr");
        }
        if (inPlus != null) {
            if (!mContactsDb.deleteContactsForUpid(upid, 1)) dbgTrace("deleteContactsForUpid Failed for upid" + upid + " For MirrPlus");
        }

        dbgTrace();

    }

    // for individual restore related
    public Contacts(Context context) {
        super();
        mContext = context;
        mContactsDb = new ContactsDb(context, mAppData.getContactDbFullPath());
    }

    public ContactTrackerWrapper getContactTrackingWrapper() {
        return mContactTrackingWrapper;
    }

    @Override
    long getNextPhoneDbNewItem(int id) {
        mCurrentPhoneItem = new ContactInfo();
        // TODO test  this one
        Cursor mCursor = mContentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts._ID + " = " + id, null, null);
        if (null == mCursor) {
            return -1;
        }

        if (mCursor.getCount() < 0) {
            return -1;
        }

        mCursor.moveToFirst();
        try {
            if (mColumnNames.contains(BaseColumns._ID)) {
                mCurrentPhoneItem.setContactId(mCursor.getString(mCursor.getColumnIndex(BaseColumns._ID)));
            }
            Log.d(tag, "ID: " + mCurrentPhoneItem.getContactId());
            Uri contactUri = Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, mCurrentPhoneItem.getContactId());
            Log.d(tag, "ID: " + contactUri);
            if (mColumnNames.contains(ContactsContract.RawContacts.DELETED)) {
                Log.d(tag, "before ");
                mCurrentPhoneItem.isDeleted = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.DELETED));

                Log.d(tag, "Deleted: " + mCurrentPhoneItem.isDeleted);
            }
            Log.d(tag, "After ");

            if (mColumnNames.contains(ContactsContract.RawContacts.ACCOUNT_NAME)) {
                mCurrentPhoneItem.accountName = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                if (mCurrentPhoneItem.accountName == null) {
                    mCurrentPhoneItem.accountName = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.CUSTOM_RINGTONE)) {
                mCurrentPhoneItem.customRingtone = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.CUSTOM_RINGTONE));
                if (mCurrentPhoneItem.customRingtone == null) {
                    mCurrentPhoneItem.customRingtone = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.TIMES_CONTACTED)) {
                mCurrentPhoneItem.timesContacted = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.TIMES_CONTACTED));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.LAST_TIME_CONTACTED)) {
                mCurrentPhoneItem.lastTimeContacted = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.RawContacts.LAST_TIME_CONTACTED));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.SEND_TO_VOICEMAIL)) {
                mCurrentPhoneItem.sendToVoiceMail = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.SEND_TO_VOICEMAIL));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.ACCOUNT_TYPE)) {
                mCurrentPhoneItem.source = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
                if (mCurrentPhoneItem.source == null) {
                    mCurrentPhoneItem.source = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.AGGREGATION_MODE)) {
                mCurrentPhoneItem.aggregationMode = (mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.AGGREGATION_MODE)));
            }
            Log.d(tag, "groupCursor");

            Cursor groupCursor = mContext.getContentResolver().query(Data.CONTENT_URI, null, Data.RAW_CONTACT_ID + "=" + mCurrentPhoneItem.getContactId() + " AND " + Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'", null, null);
            if (groupCursor != null && groupCursor.getCount() > 0) {
                groupCursor.moveToFirst();
                mCurrentPhoneItem.groupRowIds = new ArrayList<String>();
                mCurrentPhoneItem.groupNames = new ArrayList<String>();

                while (!groupCursor.isAfterLast()) {
                    mCurrentPhoneItem.groupRowIds.add(groupCursor.getString(groupCursor.getColumnIndex(Data.DATA1)));

                    // Log.d(tag,
                    // "group id:"
                    // + mCurrentPhoneItem.groupRowIds
                    // .get(groupCursor.getPosition()));

                    mCurrentPhoneItem.groupNames.add(getGroupNameFor(Integer.parseInt(mCurrentPhoneItem.groupRowIds.get(groupCursor.getPosition()))));
                    groupCursor.moveToNext();
                }

            }
            if (groupCursor != null) {
                groupCursor.close();
            }

            Log.d(tag, "nameCursor");

            String[] projection1 = new String[]{Data.STARRED, Relation.TYPE, StructuredName.GIVEN_NAME, StructuredName.FAMILY_NAME, StructuredName.MIDDLE_NAME, StructuredName.DISPLAY_NAME, StructuredName.PREFIX, StructuredName.SUFFIX, StructuredName.PHONETIC_GIVEN_NAME, StructuredName.PHONETIC_MIDDLE_NAME, StructuredName.PHONETIC_FAMILY_NAME,

            };
            String where = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
            String[] whereParameters = new String[]{mCurrentPhoneItem.getContactId(), StructuredName.CONTENT_ITEM_TYPE};

            Cursor nameCursor = null;

            nameCursor = mContext.getContentResolver().query(Data.CONTENT_URI, projection1, where, whereParameters, null);

            if (nameCursor != null && nameCursor.getCount() > 0) {

                if (nameCursor.moveToFirst()) {

                    String strName = "";
                    mCurrentPhoneItem.isStarred = nameCursor.getString(nameCursor.getColumnIndex(Data.STARRED));

                    if (mCurrentPhoneItem.isStarred != null) {

                        strName = mCurrentPhoneItem.isStarred.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.isStarred = strName;
                    } else {
                        mCurrentPhoneItem.isStarred = "";
                    }

                    mCurrentPhoneItem.firstName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.GIVEN_NAME));

                    if (mCurrentPhoneItem.firstName != null) {

                        strName = mCurrentPhoneItem.firstName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.firstName = strName;
                    } else {
                        mCurrentPhoneItem.firstName = "";
                    }

                    mCurrentPhoneItem.middleName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.MIDDLE_NAME));

                    if (mCurrentPhoneItem.middleName != null) {

                        strName = mCurrentPhoneItem.middleName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.middleName = strName;
                    } else {
                        mCurrentPhoneItem.middleName = "";
                    }

                    mCurrentPhoneItem.lastName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.FAMILY_NAME));
                    if (mCurrentPhoneItem.lastName != null) {
                        strName = mCurrentPhoneItem.lastName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.lastName = strName;
                    } else {
                        mCurrentPhoneItem.lastName = "";
                    }

                    mCurrentPhoneItem.phoneticGname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_GIVEN_NAME));

                    if (mCurrentPhoneItem.phoneticGname != null) {
                        strName = mCurrentPhoneItem.phoneticGname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticGname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticGname = "";
                    }

                    mCurrentPhoneItem.phoneticMname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_MIDDLE_NAME));
                    if (mCurrentPhoneItem.phoneticMname != null) {
                        strName = mCurrentPhoneItem.phoneticMname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticMname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticMname = "";
                    }

                    mCurrentPhoneItem.phoneticFname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_FAMILY_NAME));
                    if (mCurrentPhoneItem.phoneticFname != null) {
                        strName = mCurrentPhoneItem.phoneticFname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticFname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticFname = "";
                    }

                    mCurrentPhoneItem.namePrefix = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PREFIX));

                    if (mCurrentPhoneItem.namePrefix != null) {
                        strName = mCurrentPhoneItem.namePrefix.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.namePrefix = strName;
                    } else {
                        mCurrentPhoneItem.namePrefix = "";
                    }

                    mCurrentPhoneItem.nameSuffix = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.SUFFIX));
                    if (mCurrentPhoneItem.nameSuffix != null) {
                        strName = mCurrentPhoneItem.nameSuffix.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.nameSuffix = strName;
                    } else {
                        mCurrentPhoneItem.nameSuffix = "";
                    }

                    mCurrentPhoneItem.displayName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.DISPLAY_NAME));

                    if (mCurrentPhoneItem.displayName != null) {
                        strName = mCurrentPhoneItem.displayName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.displayName = strName;

                        Log.d(tag, "Name: " + strName);
                    } else {
                        mCurrentPhoneItem.displayName = "";
                    }

                }

            }
            if (nameCursor != null) {
                nameCursor.close();
            }

            Log.d(tag, "Lists");

            mCurrentPhoneItem.emailList = getAllEmail(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.emailList == null) {
                return -1;
            }

            mCurrentPhoneItem.phoneNumberList = getAllPhoneNumbers(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.phoneNumberList == null) {
                return -1;
            }

            mCurrentPhoneItem.postalAddresses = getPostalAddresses(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.postalAddresses == null) {
                return -1;
            }

            mCurrentPhoneItem.websiteList = getWebsites(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.websiteList == null) {
                return -1;
            }

            mCurrentPhoneItem.organizationList = getOrganization(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.organizationList == null) {
                return -1;
            }

            mCurrentPhoneItem.note = getDisplayDetails(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.note == null) {
                return -1;
            }

            mCurrentPhoneItem.nickname = getNickName(mCurrentPhoneItem.getContactId());

            mCurrentPhoneItem.eventList = getAllEvents(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.eventList == null) {
                return -1;
            }

            mCurrentPhoneItem.imList = getAllImAddresses(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.imList == null) {
                return -1;
            }

            mCurrentPhoneItem.relationship = getRelation(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.relationship == null) {
                return -1;
            }

            Log.d(tag, "photoBitmap");
            mCurrentPhoneItem.photoBitmap = getContactBitmap(mCurrentPhoneItem.getContactId());

            long checksum = mCurrentPhoneItem.crc32();
            mCurrentPhoneItem.checksum = (int) checksum;

            if (mCursor != null) {
                mCursor.close();

            }

        } catch (Exception e) {
            dbgTrace(GenUtils.getStackTrace(e));
            // Need to assign mCurrentPhoneItem.checksum with some Error number
        }

        return mCurrentPhoneItem.checksum;

    }

    @Override
    long getNextPhoneDbItem() {

        mCurrentPhoneItem = new ContactInfo();
        try {
            if (mColumnNames.contains(BaseColumns._ID)) {
                mCurrentPhoneItem.setContactId(mCursor.getString(mCursor.getColumnIndex(BaseColumns._ID)));
            }
            // Log.d(tag,"ID: "+mCurrentPhoneItem.getContactId());
            Uri contactUri = Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, mCurrentPhoneItem.getContactId());
            Log.d(tag, "ID: " + contactUri);
            if (mColumnNames.contains(ContactsContract.RawContacts.DELETED)) {
                mCurrentPhoneItem.isDeleted = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.DELETED));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.ACCOUNT_NAME)) {
                mCurrentPhoneItem.accountName = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                if (mCurrentPhoneItem.accountName == null) {
                    mCurrentPhoneItem.accountName = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.CUSTOM_RINGTONE)) {
                mCurrentPhoneItem.customRingtone = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.CUSTOM_RINGTONE));
                if (mCurrentPhoneItem.customRingtone == null) {
                    mCurrentPhoneItem.customRingtone = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.TIMES_CONTACTED)) {
                mCurrentPhoneItem.timesContacted = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.TIMES_CONTACTED));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.LAST_TIME_CONTACTED)) {
                mCurrentPhoneItem.lastTimeContacted = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.RawContacts.LAST_TIME_CONTACTED));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.SEND_TO_VOICEMAIL)) {
                mCurrentPhoneItem.sendToVoiceMail = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.SEND_TO_VOICEMAIL));
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.ACCOUNT_TYPE)) {
                mCurrentPhoneItem.source = mCursor.getString(mCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
                if (mCurrentPhoneItem.source == null) {
                    mCurrentPhoneItem.source = "";
                }
            }

            if (mColumnNames.contains(ContactsContract.RawContacts.AGGREGATION_MODE)) {
                mCurrentPhoneItem.aggregationMode = (mCursor.getInt(mCursor.getColumnIndex(ContactsContract.RawContacts.AGGREGATION_MODE)));
            }

            Cursor groupCursor = mContext.getContentResolver().query(Data.CONTENT_URI, null, Data.RAW_CONTACT_ID + "=" + mCurrentPhoneItem.getContactId() + " AND " + Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'", null, null);
            if (groupCursor != null && groupCursor.getCount() > 0) {
                groupCursor.moveToFirst();
                mCurrentPhoneItem.groupRowIds = new ArrayList<String>();
                mCurrentPhoneItem.groupNames = new ArrayList<String>();

                while (!groupCursor.isAfterLast()) {
                    mCurrentPhoneItem.groupRowIds.add(groupCursor.getString(groupCursor.getColumnIndex(Data.DATA1)));

                    // Log.d(tag,
                    // "group id:"
                    // + mCurrentPhoneItem.groupRowIds
                    // .get(groupCursor.getPosition()));

                    mCurrentPhoneItem.groupNames.add(getGroupNameFor(Integer.parseInt(mCurrentPhoneItem.groupRowIds.get(groupCursor.getPosition()))));
                    groupCursor.moveToNext();
                }

            }
            if (groupCursor != null) {
                groupCursor.close();
            }

            String[] projection1 = new String[]{Data.STARRED, Relation.TYPE, StructuredName.GIVEN_NAME, StructuredName.FAMILY_NAME, StructuredName.MIDDLE_NAME, StructuredName.DISPLAY_NAME, StructuredName.PREFIX, StructuredName.SUFFIX, StructuredName.PHONETIC_GIVEN_NAME, StructuredName.PHONETIC_MIDDLE_NAME, StructuredName.PHONETIC_FAMILY_NAME,

            };
            String where = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
            String[] whereParameters = new String[]{mCurrentPhoneItem.getContactId(), StructuredName.CONTENT_ITEM_TYPE};

            Cursor nameCursor = null;

            nameCursor = mContext.getContentResolver().query(Data.CONTENT_URI, projection1, where, whereParameters, null);

            if (nameCursor != null) {

                if (nameCursor.moveToFirst()) {

                    String strName = "";
                    mCurrentPhoneItem.isStarred = nameCursor.getString(nameCursor.getColumnIndex(Data.STARRED));

                    if (mCurrentPhoneItem.isStarred != null) {

                        strName = mCurrentPhoneItem.isStarred.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.isStarred = strName;
                    } else {
                        mCurrentPhoneItem.isStarred = "";
                    }

                    mCurrentPhoneItem.firstName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.GIVEN_NAME));

                    if (mCurrentPhoneItem.firstName != null) {

                        strName = mCurrentPhoneItem.firstName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.firstName = strName;
                    } else {
                        mCurrentPhoneItem.firstName = "";
                    }

                    mCurrentPhoneItem.middleName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.MIDDLE_NAME));

                    if (mCurrentPhoneItem.middleName != null) {

                        strName = mCurrentPhoneItem.middleName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.middleName = strName;
                    } else {
                        mCurrentPhoneItem.middleName = "";
                    }

                    mCurrentPhoneItem.lastName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.FAMILY_NAME));
                    if (mCurrentPhoneItem.lastName != null) {
                        strName = mCurrentPhoneItem.lastName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.lastName = strName;
                    } else {
                        mCurrentPhoneItem.lastName = "";
                    }

                    mCurrentPhoneItem.phoneticGname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_GIVEN_NAME));

                    if (mCurrentPhoneItem.phoneticGname != null) {
                        strName = mCurrentPhoneItem.phoneticGname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticGname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticGname = "";
                    }

                    mCurrentPhoneItem.phoneticMname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_MIDDLE_NAME));
                    if (mCurrentPhoneItem.phoneticMname != null) {
                        strName = mCurrentPhoneItem.phoneticMname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticMname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticMname = "";
                    }

                    mCurrentPhoneItem.phoneticFname = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PHONETIC_FAMILY_NAME));
                    if (mCurrentPhoneItem.phoneticFname != null) {
                        strName = mCurrentPhoneItem.phoneticFname.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.phoneticFname = strName;
                    } else {
                        mCurrentPhoneItem.phoneticFname = "";
                    }

                    mCurrentPhoneItem.namePrefix = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.PREFIX));

                    if (mCurrentPhoneItem.namePrefix != null) {
                        strName = mCurrentPhoneItem.namePrefix.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.namePrefix = strName;
                    } else {
                        mCurrentPhoneItem.namePrefix = "";
                    }

                    mCurrentPhoneItem.nameSuffix = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.SUFFIX));
                    if (mCurrentPhoneItem.nameSuffix != null) {
                        strName = mCurrentPhoneItem.nameSuffix.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.nameSuffix = strName;
                    } else {
                        mCurrentPhoneItem.nameSuffix = "";
                    }

                    mCurrentPhoneItem.displayName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.DISPLAY_NAME));

                    if (mCurrentPhoneItem.displayName != null) {
                        strName = mCurrentPhoneItem.displayName.trim();
                        if (strName.length() > 0) mCurrentPhoneItem.displayName = strName;
                    } else {
                        mCurrentPhoneItem.displayName = "";
                    }

                }

            }
            if (nameCursor != null) {
                nameCursor.close();
            }
            mCurrentPhoneItem.emailList = getAllEmail(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.emailList == null) {
                return -1;
            }

            mCurrentPhoneItem.phoneNumberList = getAllPhoneNumbers(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.phoneNumberList == null) {
                return -1;
            }

            mCurrentPhoneItem.postalAddresses = getPostalAddresses(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.postalAddresses == null) {
                return -1;
            }

            mCurrentPhoneItem.websiteList = getWebsites(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.websiteList == null) {
                return -1;
            }

            mCurrentPhoneItem.organizationList = getOrganization(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.organizationList == null) {
                return -1;
            }

            mCurrentPhoneItem.note = getDisplayDetails(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.note == null) {
                return -1;
            }

            mCurrentPhoneItem.nickname = getNickName(mCurrentPhoneItem.getContactId());

            mCurrentPhoneItem.eventList = getAllEvents(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.eventList == null) {
                return -1;
            }

            mCurrentPhoneItem.imList = getAllImAddresses(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.imList == null) {
                return -1;
            }

            mCurrentPhoneItem.relationship = getRelation(mCurrentPhoneItem.getContactId());
            if (mCurrentPhoneItem.relationship == null) {
                return -1;
            }

            mCurrentPhoneItem.photoBitmap = getContactBitmap(mCurrentPhoneItem.getContactId());

            long checksum = mCurrentPhoneItem.crc32();
            mCurrentPhoneItem.checksum = (int) checksum;

        } catch (Exception e) {
            dbgTrace(GenUtils.getStackTrace(e));
            // Need to assign mCurrentPhoneItem.checksum with some Error number
        }

        mCursor.moveToNext();

        return mCurrentPhoneItem.checksum;

    }

    @Override
    long getNextMirrorItem() {

        ContactInfo contactObj = null;
        try {

            contactObj = mReaderMapper.readValue(mMirrorParser, ContactInfo.class);
            mCurrentMirrorItem = contactObj;

        } catch (JsonProcessingException ex) {
            dbgTrace(GenUtils.getStackTrace(ex));
        } catch (IOException ex) {
            dbgTrace(GenUtils.getStackTrace(ex));
        }

        return (contactObj != null) ? contactObj.checksum : -1;
    }

    @Override
    long getNextMirrorPlusItem() {

        ContactInfo contactObj = null;
        try {

            contactObj = mReaderMapper.readValue(mPlusParser, ContactInfo.class);
            mCurrentPlusItem = contactObj;

        } catch (JsonProcessingException ex) {
            dbgTrace(GenUtils.getStackTrace(ex));
        } catch (IOException ex) {
            dbgTrace(GenUtils.getStackTrace(ex));
        }
        return (contactObj != null) ? contactObj.checksum : -1;
    }

    @Override
    boolean addMirrorItemToPhone() {

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();


        if (!checkAccountIfExist(mCurrentMirrorItem.accountName)) {
            dbgTrace("Inserting as device type account contact ");
            mCurrentMirrorItem.accountName = null;
            mCurrentMirrorItem.source = null;
        }

        if (mCurrentMirrorItem.aggregationMode == ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT) {
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mCurrentMirrorItem.getSource()).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mCurrentMirrorItem.getAccountName()).withValue(ContactsContract.RawContacts.CUSTOM_RINGTONE, mCurrentMirrorItem.getCustomRingtone()).withValue(ContactsContract.RawContacts.TIMES_CONTACTED, mCurrentMirrorItem.getTimesContacted()).withValue(ContactsContract.RawContacts.LAST_TIME_CONTACTED, mCurrentMirrorItem.getLastTimeContacted()).withValue(ContactsContract.RawContacts.SEND_TO_VOICEMAIL, mCurrentMirrorItem.getSendToVoiceMail()).build());
        }
        if (mCurrentMirrorItem.aggregationMode == ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED) {
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mCurrentMirrorItem.getSource()).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mCurrentMirrorItem.getAccountName()).build());
        }
        if (mCurrentMirrorItem.aggregationMode == ContactsContract.RawContacts.AGGREGATION_MODE_SUSPENDED) {
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_SUSPENDED).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mCurrentMirrorItem.getSource()).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mCurrentMirrorItem.getAccountName()).build());
        }

        if (mCurrentMirrorItem.getPhotoBitmap() != null) {

            InputStream stream1 = null;

            Bitmap tempImage = null;
            try {

                stream1 = new ByteArrayInputStream(mCurrentMirrorItem.getPhotoBitmap());
                tempImage = BitmapFactory.decodeStream(stream1);

                if (tempImage != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    tempImage.compress(CompressFormat.PNG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE).withValue(Photo.PHOTO, bitmapdata).build());
                }
            } catch (Exception e) {
                dbgTrace(GenUtils.getStackTrace(e));
            }
        }

        // --------------Name

        operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                // .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                // ContactsContract.RawContacts.AGGREGATION_MODE_SUSPENDED)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.PREFIX, mCurrentMirrorItem.getNamePrefix()).withValue(StructuredName.GIVEN_NAME, mCurrentMirrorItem.getFirstName()).withValue(StructuredName.MIDDLE_NAME, mCurrentMirrorItem.getMiddleName()).withValue(StructuredName.FAMILY_NAME, mCurrentMirrorItem.getLastName()).withValue(Nickname.NAME, mCurrentMirrorItem.getNickname()).withValue(StructuredName.SUFFIX, mCurrentMirrorItem.getNameSuffix()).withValue(StructuredName.DISPLAY_NAME, mCurrentMirrorItem.getDisplay_name())
                // .withValue(ContactsContract.CommonDataKinds.Note.NOTE,
                // contactBean.getM_note())
                .withValue(StructuredName.PHONETIC_GIVEN_NAME, mCurrentMirrorItem.getPhonetic_fname()).withValue(StructuredName.PHONETIC_MIDDLE_NAME, mCurrentMirrorItem.getPhonetic_mname()).withValue(StructuredName.PHONETIC_FAMILY_NAME, mCurrentMirrorItem.getPhonetic_gname()).build());

        operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)

                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE).withValue(Note.NOTE, mCurrentMirrorItem.getNote()).build());


        if (mCurrentMirrorItem.groupSourceId != null) {
            if (!mCurrentMirrorItem.groupSourceId.equalsIgnoreCase("")) {
                // Log.d(tag, "group id exist");
                operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)

                        .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_SOURCE_ID, mCurrentMirrorItem.groupSourceId).build());
            }
        }
        if (mCurrentMirrorItem.groupRowIds != null) {
            int groupCount = mCurrentMirrorItem.groupRowIds.size();
            int i;
            for (i = 0; i < groupCount; i++) {

                if (checkIfGroupExist(mCurrentMirrorItem.groupNames.get(i))) {
                    operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, mCurrentMirrorItem.groupRowIds.get(i)).build());
                } else {
                    // create group and insert

                    ContentValues groupValues;
                    ContentResolver cr = mContext.getContentResolver();
                    groupValues = new ContentValues();
                    groupValues.put(Groups.TITLE, mCurrentMirrorItem.groupNames.get(i));
                    try {
                        cr.insert(Groups.CONTENT_URI, groupValues);
                    } catch (Exception e) {
                        dbgTrace("Exception: " + e.getMessage());
                    }
                    // Log.d(tag, "Inserted new group");
                    String newGroupId = getTheGroupId(mCurrentMirrorItem.groupNames.get(i));
                    if (newGroupId != null) {
                        operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, newGroupId).build());
                    }
                }
            }

        }

        operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)

                .withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE).withValue(Nickname.NAME, mCurrentMirrorItem.getNickname()).build());

        // --------------------phone number list

        ArrayList<PhoneNumberList> phoneNumberBeans = mCurrentMirrorItem.getPhoneNumberList();
        int totalPhoneNumbers = phoneNumberBeans.size();
        for (int i = 0; i < totalPhoneNumbers; i++) {
            if (mAbortFlag) {
                return false;
            }
            PhoneNumberList numberBean = phoneNumberBeans.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, numberBean.getNumber()).withValue(Phone.TYPE, numberBean.getType()).withValue(Phone.LABEL, numberBean.getLabel()).build());
        }

        // -------------- IM list
        ArrayList<IMList> imBeans = mCurrentMirrorItem.getImList();
        int totalIMBeans = imBeans.size();
        for (int i = 0; i < totalIMBeans; i++) {
            if (mAbortFlag) {
                return false;
            }
            IMList imBean = imBeans.get(i);
            if (imBean.getImType() == 11) {
                operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE).withValue(Im.PROTOCOL, imBean.getImProtocol()).withValue(Im.TYPE, -1).withValue(Im.DATA1, imBean.getImAddress()).withValue(Im.DATA3, imBean.getCustomType()).withValue(Im.CUSTOM_PROTOCOL, imBean.getCustomProtocol()).build());
            } else {
                operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE).withValue(Im.PROTOCOL, imBean.getImProtocol()).withValue(Im.TYPE, imBean.getImType()).withValue(Im.DATA1, imBean.getImAddress()).withValue(Im.DATA3, imBean.getCustomType()).withValue(Im.CUSTOM_PROTOCOL, imBean.getCustomProtocol()).build());
            }

        }
        // ---------------------Email list
        ArrayList<EmailList> emailList = mCurrentMirrorItem.getEmailList();
        int totalEmailList = emailList.size();

        for (int i = 0; i < totalEmailList; i++) {
            if (mAbortFlag) {
                return false;
            }
            EmailList email = emailList.get(i);

            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE).withValue(Email.TYPE, email.getEmailType()).withValue(Email.DATA, email.getEmailID()).withValue(Email.LABEL, email.getEmailLabel()).build());

        }
        // ---------------------postal address list
        ArrayList<PostalAddresses> poBeans = mCurrentMirrorItem.getPostalAddresses();
        int totalAddresses = poBeans.size();
        for (int i = 0; i < totalAddresses; i++) {
            if (mAbortFlag) {
                return false;
            }
            PostalAddresses poBean = poBeans.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE).withValue(StructuredPostal.TYPE, poBean.getType()).withValue(StructuredPostal.STREET, poBean.getStreet())
                    // .withValue(
                    // ContactsContract.CommonDataKinds.StructuredPostal.POBOX,
                    // poBean.getPoBox())
                    .withValue(StructuredPostal.NEIGHBORHOOD, poBean.getNeighbourhood()).withValue(StructuredPostal.CITY, poBean.getCity()).withValue(StructuredPostal.REGION, poBean.getRegion()).withValue(StructuredPostal.POSTCODE, poBean.getZipcode()).withValue(StructuredPostal.COUNTRY, poBean.getCountry()).withValue(StructuredPostal.LABEL, poBean.getLabel()).build());

        }
        // --------------------Event list
        ArrayList<EventList> eventList = mCurrentMirrorItem.getEventList();
        int totalEventLists = eventList.size();
        for (int i = 0; i < totalEventLists; i++) {
            if (mAbortFlag) {
                return false;
            }
            EventList eventBean = eventList.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE).withValue(Event.START_DATE, eventBean.getStartDate()).withValue(Event.TYPE, eventBean.getEtype()).withValue(Event.LABEL, eventBean.getEventLabel()).build());

        }
        // ---------------------Relationship list
        ArrayList<Relationship> relationshipBean = mCurrentMirrorItem.getRelationship();
        for (int i = 0; i < relationshipBean.size(); i++) {
            if (mAbortFlag) {
                return false;
            }

            Relationship relationBean = relationshipBean.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Relation.CONTENT_ITEM_TYPE).withValue(Relation.NAME, relationBean.getRelationName()).withValue(Relation.TYPE, relationBean.getRelationType()).withValue(Relation.LABEL, relationBean.getRelationLabel()).build());

        }
        // ------------------------------organization list
        ArrayList<OrganizationList> main_organizationBean = mCurrentMirrorItem.getOrganizationList();
        for (int i = 0; i < main_organizationBean.size(); i++) {
            if (mAbortFlag) {
                return false;
            }

            OrganizationList organizationBean = main_organizationBean.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organizationBean.getOrganizationName()).withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, organizationBean.getJobDepartment()).withValue(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION, organizationBean.getJobDescription()).withValue(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION, organizationBean.getOfficeLocation()).withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME, organizationBean.getOfficePhoneticName()).withValue(ContactsContract.CommonDataKinds.Organization.SYMBOL, organizationBean.getOfficeSymbol()).withValue(ContactsContract.CommonDataKinds.Organization.TITLE, organizationBean.getTitle()).withValue(ContactsContract.CommonDataKinds.Organization.TYPE, organizationBean.getType()).build());
        }
        // --------------------------website list
        ArrayList<WebsitesList> mainWebsiteList = mCurrentMirrorItem.getWebsiteList();
        for (int i = 0; i < mainWebsiteList.size(); i++) {
            if (mAbortFlag) {
                return false;
            }
            WebsitesList websiteBean = mainWebsiteList.get(i);
            operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, 0).withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE).withValue(Website.URL, websiteBean.getWebsite()).withValue(Website.TYPE, websiteBean.getWebsiteType()).withValue(Website.LABEL, websiteBean.getWebsiteLabel()).build());

        }
        // apply changes to phone
        try {
            @SuppressWarnings("unused") ContentProviderResult[] cpr = mContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
            dbgTrace("Insert failed :" + GenUtils.getStackTrace(e));
        }
        return true;
    }

    @Override
    boolean addPhoneItemToMirror() {
        return serialize(outMirrFileStream, mCurrentPhoneItem);
    }

    @Override
    boolean addMirrorItemToPlus() {

        return serialize(outPlusFileStream, mCurrentMirrorItem);
    }

    private boolean serialize(FileOutputStream fos, ContactInfo obj) {

        try {
            mObjectWriter.withType(ContactInfo.class).writeValue(fos, obj);
            return true;
        } catch (JsonGenerationException ex) {
            dbgTrace("JsonGenerationException: " + GenUtils.getStackTrace(ex));
        } catch (JsonMappingException ex) {
            dbgTrace("JsonMappingException: " + GenUtils.getStackTrace(ex));
        } catch (IOException ex) {
            dbgTrace("IOException: " + GenUtils.getStackTrace(ex));
        }
        return false;
    }

// for database insertion related

    @Override
    void refreshPhoneDb() {
        // Nothing to do in Contacts
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("Contacts.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("Contacts.log", trace);
    }

    /**
     * To get the list of relation fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<Relationship> getRelation(String contactId) {

        ArrayList<Relationship> rltnList = new ArrayList<Relationship>();
        ContentResolver cr = mContext.getContentResolver();
        int relationName = 0;
        int relationType = 0;
        int relationLabel = 0;

        String rbWhere = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
        String[] rbWhereParams = new String[]{contactId, Relation.CONTENT_ITEM_TYPE};
        final String[] projection = new String[]{Relation.NAME, Relation.TYPE, Relation.LABEL};

        try {
            Cursor rbCursor = cr.query(Data.CONTENT_URI, projection, rbWhere, rbWhereParams, null);
            if (rbCursor != null && rbCursor.getCount() > 0) {

                if (rbCursor != null && rbCursor.moveToFirst()) {
                    relationName = rbCursor.getColumnIndex(Relation.NAME);
                    relationType = rbCursor.getColumnIndex(Relation.TYPE);
                    relationLabel = rbCursor.getColumnIndex(Relation.LABEL);
                    while (!rbCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            rbCursor.close();
                            return null;
                        }
                        Relationship relObj = new Relationship();
                        relObj.setRelationName(rbCursor.getString(relationName));
                        if (relObj.relationName == null) {
                            relObj.relationName = "";
                        }
                        relObj.setRelationType(rbCursor.getInt(relationType));
                        relObj.setRelationLabel(rbCursor.getString(relationLabel));
                        if (relObj.relationLabel == null) {
                            relObj.relationLabel = "";
                        }
                        rltnList.add(relObj);
                        rbCursor.moveToNext();
                    }

                }
            }
            if (rbCursor != null) {
                rbCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        return rltnList;

    }

    /**
     * To get the list of IM fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<IMList> getAllImAddresses(String contactId) {

        ArrayList<IMList> imList = new ArrayList<IMList>();
        ContentResolver cr = mContext.getContentResolver();
        int contactIMAddress = 0;
        int contactIMType = 0;
        int contactIMLabel = 0;
        int contactProtocol = 0;
        int contactIMCustomProtocol = 0;

        String imWhere = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
        String[] imWhereParams = new String[]{contactId, Im.CONTENT_ITEM_TYPE};
        final String[] projection = new String[]{Im.DATA, Im.TYPE, Im.LABEL, Im.PROTOCOL, Im.CUSTOM_PROTOCOL};

        try {
            Cursor imCursor = cr.query(Data.CONTENT_URI, projection, imWhere, imWhereParams, null);
            if (imCursor != null && imCursor.getCount() > 0) {

                if (imCursor != null && imCursor.moveToFirst()) {
                    contactIMAddress = imCursor.getColumnIndex(Im.DATA);
                    contactIMType = imCursor.getColumnIndex(Im.TYPE);
                    contactIMLabel = imCursor.getColumnIndex(Im.LABEL);
                    contactProtocol = imCursor.getColumnIndex(Im.PROTOCOL);
                    contactIMCustomProtocol = imCursor.getColumnIndex(Im.CUSTOM_PROTOCOL);

                    while (!imCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            imCursor.close();
                            return null;
                        }
                        IMList imObj = new IMList();

                        imObj.setImAddress(imCursor.getString(contactIMAddress));
                        if (imObj.imAddress == null) {
                            imObj.imAddress = "";
                        }
                        imObj.setImType(imCursor.getInt(contactIMType));
                        imObj.setCustomType(imCursor.getString(contactIMLabel));
                        if (imObj.customType == null) {
                            imObj.customType = "";
                        }
                        imObj.setImProtocol(imCursor.getString(contactProtocol));
                        if (imObj.imProtocol == null) {
                            imObj.imProtocol = "";
                        }
                        imObj.setCustomProtocol(imCursor.getString(contactIMCustomProtocol));
                        if (imObj.customProtocol == null) {
                            imObj.customProtocol = "";
                        }
                        imList.add(imObj);
                        imCursor.moveToNext();
                    }

                }
            }
            if (imCursor != null) {
                imCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return imList;

    }

    /**
     * To get the list of Event fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<EventList> getAllEvents(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        Cursor eventCursor = null;
        int startDateColumnIndex = 0;
        int eventColumnIndex = 0;
        int eventLabelColumnIndex = 0;
        ArrayList<EventList> eventLists = new ArrayList<EventList>();

        try {
            eventCursor = cr.query(Data.CONTENT_URI, new String[]{BaseColumns._ID, Event.START_DATE, Event.TYPE, Event.LABEL}, Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Event.CONTENT_ITEM_TYPE + "'", new String[]{contactId}, null);
            if (eventCursor != null && eventCursor.getCount() > 0) {

                if (eventCursor != null && eventCursor.moveToFirst()) {

                    startDateColumnIndex = eventCursor.getColumnIndex(Event.START_DATE);
                    eventColumnIndex = eventCursor.getColumnIndex(Event.TYPE);
                    eventLabelColumnIndex = eventCursor.getColumnIndex(Event.LABEL);

                    while (!eventCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            eventCursor.close();
                            return null;
                        }
                        EventList eventObj = new EventList();
                        eventObj.setEtype(Integer.parseInt(eventCursor.getString(eventColumnIndex)));

                        eventObj.setStartDate(eventCursor.getString(startDateColumnIndex));
                        if (eventObj.startDate == null) {
                            eventObj.startDate = "";
                        }
                        eventObj.setEventLabel(eventCursor.getString(eventLabelColumnIndex));
                        if (eventObj.eventLabel == null) {
                            eventObj.eventLabel = "";
                        }
                        eventLists.add(eventObj);
                        eventCursor.moveToNext();
                    }
                }
            }
            if (eventCursor != null) {
                eventCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return eventLists;
    }

    /**
     * To get the bitmap image (thumbnail-sized) of a contact
     *
     * @param contactId
     *
     * @return byte[]
     */
    private byte[] getContactBitmap(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        String where = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
        String[] whereParameters = new String[]{contactId, Photo.CONTENT_ITEM_TYPE};
        byte[] photoBlob = null;
        try {
            Cursor contactCursor = cr.query(Data.CONTENT_URI, new String[]{Photo.PHOTO_ID}, where, whereParameters, null);
            if (contactCursor != null && contactCursor.getCount() > 0) {

                if (contactCursor != null && contactCursor.moveToFirst()) {

                    // final int photoId = contact.getInt(contact
                    // .getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                    String photoWhere = Photo.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
                    String[] photoWhereParams = new String[]{contactId, Photo.CONTENT_ITEM_TYPE};

                    Cursor photo = cr.query(Data.CONTENT_URI, new String[]{Photo.PHOTO}, photoWhere, photoWhereParams, null);
                    if (photo != null && photo.getCount() > 0) {
                        if (photo != null && photo.moveToFirst()) {
                            photoBlob = photo.getBlob(photo.getColumnIndex(Photo.PHOTO));

                        }
                    }
                    if (photo != null) {
                        photo.close();
                    }

                }
            }
            if (contactCursor != null) {
                contactCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        // dbgTrace("contains:image " + photoBlob);
        return photoBlob;

    }

    /**
     * To get the nickname of a contact if present
     *
     * @param contactId
     *
     * @return String
     */
    private String getNickName(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        Cursor nickCursor = null;
        String nickName = "";

        try {
            nickCursor = cr.query(Data.CONTENT_URI, new String[]{BaseColumns._ID, Nickname.NAME}, Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Nickname.CONTENT_ITEM_TYPE + "'", new String[]{contactId}, null);
            if (nickCursor != null && nickCursor.getCount() > 0) {

                if (nickCursor != null && nickCursor.moveToFirst()) {
                    final int nicknameIndex = nickCursor.getColumnIndex(Nickname.NAME);

                    while (!nickCursor.isAfterLast()) {
                        nickName = nickCursor.getString(nicknameIndex);
                        nickCursor.moveToNext();
                    }
                }
            }
            if (nickCursor != null) {
                nickCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        if (nickName == null) return "";
        else return nickName;

    }

    /**
     * To get the note information of a contact
     *
     * @param contactId
     *
     * @return String
     */
    private String getDisplayDetails(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        Cursor noteCursor = null;
        int noteIndex = 0;
        String note = "";

        try {
            noteCursor = cr.query(Data.CONTENT_URI, new String[]{BaseColumns._ID, Note.NOTE}, Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Note.CONTENT_ITEM_TYPE + "'", new String[]{contactId}, null);
            if (noteCursor != null && noteCursor.getCount() > 0) {

                if (noteCursor != null && noteCursor.moveToFirst()) {
                    noteIndex = noteCursor.getColumnIndex(Note.NOTE);
                    while (!noteCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            noteCursor.close();
                            return null;
                        }
                        note = noteCursor.getString(noteIndex);
                        noteCursor.moveToNext();
                    }
                }
            }
            if (noteCursor != null) {
                noteCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        if (note == null) return "";
        else return note;

    }

    /**
     * To get the list of organization fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<OrganizationList> getOrganization(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        ArrayList<OrganizationList> orgList = new ArrayList<OrganizationList>();

        String orgWhere = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
        String[] orgWhereParams = new String[]{contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

        try {
            Cursor orgCur = cr.query(Data.CONTENT_URI, null, orgWhere, orgWhereParams, null);
            if (orgCur != null && orgCur.getCount() > 0) {

                if (orgCur != null && orgCur.moveToFirst()) {

                    while (!orgCur.isAfterLast()) {
                        if (mAbortFlag) {
                            orgCur.close();
                            return null;
                        }
                        OrganizationList orgObj = new OrganizationList();
                        orgObj.title = orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (orgObj.title == null) {
                            orgObj.title = "";
                        }

                        orgObj.setJobDepartment(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)));
                        if (orgObj.jobDepartment == null) {
                            orgObj.jobDepartment = "";
                        }

                        orgObj.setJobDescription(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION)));
                        if (orgObj.jobDescription == null) {
                            orgObj.jobDescription = "";
                        }

                        orgObj.setOfficePhoneticName(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME)));
                        if (orgObj.officePhoneticName == null) {
                            orgObj.officePhoneticName = "";
                        }
                        orgObj.setOrganizationName(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA)));
                        if (orgObj.organizationName == null) {
                            orgObj.organizationName = "";
                        }
                        orgObj.setType(orgCur.getInt(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TYPE)));
                        orgObj.setOfficeSymbol(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.SYMBOL)));
                        if (orgObj.officeSymbol == null) {
                            orgObj.officeSymbol = "";
                        }
                        orgObj.setOfficeLocation(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION)));
                        if (orgObj.officeLocation == null) {
                            orgObj.officeLocation = "";
                        }
                        orgObj.setTypeLabel(orgCur.getString(orgCur.getColumnIndex(ContactsContract.CommonDataKinds.Organization.LABEL)));
                        if (orgObj.typeLabel == null) {
                            orgObj.typeLabel = "";
                        }
                        orgList.add(orgObj);
                        orgCur.moveToNext();
                    }

                }
            }
            if (orgCur != null) {
                orgCur.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return orgList;
    }

    /**
     * To get the list of website fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<WebsitesList> getWebsites(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        Cursor websiteCursor = null;
        String webURL = "";
        int webType = 0;
        String webL = "";

        ArrayList<WebsitesList> webArray = new ArrayList<WebsitesList>();

        try {
            websiteCursor = cr.query(Data.CONTENT_URI, new String[]{BaseColumns._ID, Website.URL, Website.TYPE, Website.LABEL}, Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Website.CONTENT_ITEM_TYPE + "'", new String[]{contactId}, null);
            if (websiteCursor != null && websiteCursor.getCount() > 0) {

                if (websiteCursor != null && websiteCursor.moveToFirst()) {

                    while (!websiteCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            websiteCursor.close();
                            return null;
                        }
                        webURL = websiteCursor.getString(websiteCursor.getColumnIndex(Website.URL));
                        if (webURL == null) {
                            webURL = "";
                        }

                        webType = websiteCursor.getInt(websiteCursor.getColumnIndex(Website.TYPE));

                        webL = websiteCursor.getString(websiteCursor.getColumnIndex(Website.LABEL));
                        if (webL == null) {
                            webL = "";
                        }

                        WebsitesList webObj = new WebsitesList();
                        webObj.setWebsite(webURL);
                        webObj.setWebsiteType(webType);
                        webObj.setWebsiteLabel(webL);

                        webArray.add(webObj);
                        websiteCursor.moveToNext();
                    }

                }
            }
            if (websiteCursor != null) {
                websiteCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        return webArray;
    }

    /**
     * To get the list of phone numbers of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<PhoneNumberList> getAllPhoneNumbers(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        final String[] projection = new String[]{Phone.NUMBER, Phone.TYPE, Phone.LABEL};
        ArrayList<PhoneNumberList> phnList = new ArrayList<PhoneNumberList>();
        int contactTypeColumnIndex = 0;
        int contactNumberColumnIndex = 0;
        int contactLabelColumnIndex = 0;

        try {
            Cursor phoneCursor = cr.query(Phone.CONTENT_URI, projection, Data.RAW_CONTACT_ID + "=?", new String[]{contactId}, null);
            if (phoneCursor != null && phoneCursor.getCount() > 0) {

                if (phoneCursor != null && phoneCursor.moveToFirst()) {

                    contactNumberColumnIndex = phoneCursor.getColumnIndex(Phone.NUMBER);
                    contactTypeColumnIndex = phoneCursor.getColumnIndex(Phone.TYPE);
                    contactLabelColumnIndex = phoneCursor.getColumnIndex(Phone.LABEL);

                    while (!phoneCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            phoneCursor.close();
                            return null;
                        }
                        PhoneNumberList pObj = new PhoneNumberList();
                        pObj.number = phoneCursor.getString(contactNumberColumnIndex);
                        if (pObj.number == null) {
                            pObj.number = "";
                        }
                        pObj.label = phoneCursor.getString(contactLabelColumnIndex);
                        if (pObj.label == null) {
                            pObj.label = "";
                        }
                        pObj.type = phoneCursor.getInt(contactTypeColumnIndex);

                        phnList.add(pObj);
                        phoneCursor.moveToNext();
                    }

                }
            }
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return phnList;
    }

    /**
     * To get the list of postal addresses of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<PostalAddresses> getPostalAddresses(String contactId) {

        // System.out.println("In getPostalAddresses");
        ContentResolver cr = mContext.getContentResolver();
        String[] projection = new String[]{StructuredPostal.TYPE, StructuredPostal.FORMATTED_ADDRESS, StructuredPostal.STREET, StructuredPostal.POBOX, StructuredPostal.NEIGHBORHOOD, StructuredPostal.CITY, StructuredPostal.REGION, StructuredPostal.POSTCODE, StructuredPostal.COUNTRY, StructuredPostal.LABEL};

        ArrayList<PostalAddresses> AddrList = new ArrayList<PostalAddresses>();

        try {
            Cursor addressCursor = cr.query(StructuredPostal.CONTENT_URI, projection, Data.RAW_CONTACT_ID + "=?", new String[]{contactId}, null);
            if (addressCursor != null && addressCursor.getCount() > 0) {
                if (addressCursor != null && addressCursor.moveToFirst()) {
                    while (!addressCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            addressCursor.close();
                            return null;
                        }
                        PostalAddresses addrObj = new PostalAddresses();
                        addrObj.type = addressCursor.getInt(addressCursor.getColumnIndex(StructuredPostal.TYPE));

                        addrObj.address = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS));
                        if (addrObj.address == null) {
                            addrObj.address = "";
                        }
                        addrObj.label = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.LABEL));
                        if (addrObj.label == null) {
                            addrObj.label = "";
                        }

                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.STREET)) != null) {
                            addrObj.setStreet(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.STREET)));
                        }
                        // if (addressCursor.getString(addressCursor
                        // .getColumnIndex(StructuredPostal.POBOX)) != null) {
                        // addrObj.setPoBox(addressCursor.getString(addressCursor
                        // .getColumnIndex(StructuredPostal.POBOX)));
                        // }
                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.NEIGHBORHOOD)) != null) {
                            addrObj.setNeighbourhood(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.NEIGHBORHOOD)));
                        }
                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.CITY)) != null) {
                            addrObj.setCity(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.CITY)));
                        }
                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.REGION)) != null) {
                            addrObj.setRegion(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.REGION)));
                        }
                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.POSTCODE)) != null) {
                            addrObj.setZipcode(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.POSTCODE)));
                        }
                        if (addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.COUNTRY)) != null) {
                            addrObj.setCountry(addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.COUNTRY)));
                        }

                        AddrList.add(addrObj);
                        addressCursor.moveToNext();
                    }

                }
            }
            if (addressCursor != null) {
                addressCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        return AddrList;

    }

    // Internal methods

    private String getValidmailId(String emailid){
        // remove front and rear single quotes
        while (emailid != null && emailid.length() > 0 && emailid.charAt(0) == '\'') { // Remove all the ' chars in front of the real string
            emailid = emailid.substring(1, emailid.length());
        }

        while (emailid != null && emailid.length() > 0 && emailid.charAt(emailid.length() - 1) == '\'') {  // Remove all the ' chars in rear of the real string
            emailid = emailid.substring(0, emailid.length() - 1);
        }

        // remove front and rear double quotes
        while (emailid != null && emailid.length() > 0 && emailid.charAt(0) == '\"') { // Remove all the " chars in front of the real string
            emailid = emailid.substring(1, emailid.length());
        }

        while (emailid != null && emailid.length() > 0 && emailid.charAt(emailid.length() - 1) == '\"') {  // Remove all the " chars in rear of the real string
            emailid = emailid.substring(0, emailid.length() - 1);
        }

        emailid = emailid.replaceAll(" ", "_");

        // arun: 13feb2018: take care of in between single quotes - e.g. Pedro.D'Armas@officedepot.com in bug report id: 74783583
        emailid = emailid.replaceAll("'","''");

        return emailid;

    }

    /**
     * To get the list of email fields of a contact
     *
     * @param contactId
     *
     * @return ArrayList
     */
    private ArrayList<EmailList> getAllEmail(String contactId) {

        ContentResolver cr = mContext.getContentResolver();
        ArrayList<EmailList> emailList = new ArrayList<EmailList>();
        final String[] projection = new String[]{Email.DATA, Email.TYPE, Email.LABEL};
        int contactEmailColumnIndex = 0;
        int contactTypeColumnIndex = 0;
        int contactLabelColumnIndex = 0;

        try {
            Cursor emailCursor = cr.query(Email.CONTENT_URI, projection, Data.RAW_CONTACT_ID + "=?", new String[]{contactId}, null);

            if (emailCursor != null && emailCursor.getCount() > 0) {
                if (emailCursor != null && emailCursor.moveToFirst()) {
                    contactEmailColumnIndex = emailCursor.getColumnIndex(Email.DATA);
                    contactTypeColumnIndex = emailCursor.getColumnIndex(Email.TYPE);
                    contactLabelColumnIndex = emailCursor.getColumnIndex(Email.LABEL);

                    while (!emailCursor.isAfterLast()) {
                        if (mAbortFlag) {
                            emailCursor.close();
                            return null;
                        }
                        EmailList emailObj = new EmailList();

                        emailObj.emailId = emailCursor.getString(contactEmailColumnIndex);
                        if (emailObj.emailId == null) {
                            emailObj.emailId = "";
                        }else {
                            // check for descripency
                            // ie if maild id having single quotes which comes from windows phone, need to be removed
                            dbgTrace("Before ID:"+emailObj.emailId);
                            emailObj.emailId=this.getValidmailId(emailObj.emailId);
                            dbgTrace("After ID:"+emailObj.emailId);
                            if (emailObj.emailId == null) {
                                emailObj.emailId = "";
                            }
                        }
                        emailObj.emailType = emailCursor.getInt(contactTypeColumnIndex);
                        emailObj.emailLabel = emailCursor.getString(contactLabelColumnIndex);
                        if (emailObj.emailLabel == null) {
                            emailObj.emailLabel = "";
                        }
                        emailList.add(emailObj);
                        emailCursor.moveToNext();
                    }

                }
            }
            if (emailCursor != null) {
                emailCursor.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return emailList;

    }

    /**
     * To get the groupname by knowing groupid of contact
     *
     * @param groupId
     *
     * @return String
     */
    private String getGroupNameFor(long groupId) {

        Uri uri = Groups.CONTENT_URI;
        String where = String.format("%s = ?", Groups._ID);
        String[] whereParams = new String[]{Long.toString(groupId)};
        String[] selectColumns = {Groups.TITLE};
        String groupName = "";

        try {
            Cursor c = mContext.getContentResolver().query(uri, selectColumns, where, whereParams, null);

            if (c != null && c.getCount() > 0) {
                if (c != null && c.moveToFirst()) {
                    // Log.d(tag, "group name:" + c.getString(0));
                    groupName = c.getString(0);
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return groupName;
    }

    /**
     * To check if the group is present in the phone, by knowing groupname
     *
     * @param groupName
     *
     * @return true if group is present else false
     */
    private boolean checkIfGroupExist(String groupName) {

        Uri uri = Groups.CONTENT_URI;
        String where = Groups.TITLE + " = ? AND " + Groups.DELETED + " !='1' ";
        String[] whereParameters = new String[]{groupName};

        try {
            Cursor c = mContext.getContentResolver().query(uri, null, where, whereParameters, null);

            if (c != null && c.getCount() != 0) {
                // Log.d(tag, "group already exists :during Restore");
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }

        return false;
    }

    /**
     * To get the groupid of a contact, by knowing groupname
     *
     * @param groupName
     *
     * @return String
     */
    private String getTheGroupId(String groupName) {

        final String[] projection = new String[]{Groups._ID};
        Uri uri = Groups.CONTENT_URI;
        String where = Groups.TITLE + " = ? ";
        String[] whereParameters = new String[]{groupName};

        try {
            Cursor c = mContext.getContentResolver().query(uri, projection, where, whereParameters, null);
            String groupId = null;

            if (c != null && c.getCount() > 0) {
                if (c.moveToFirst()) {
                    groupId = c.getString(c.getColumnIndex(Groups._ID));
                    // Log.d(tag, "new group id is:" + groupId);
                    c.close();
                    return groupId;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            dbgTrace("Exception: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if the account of contact present in phone.
     */
    private boolean checkAccountIfExist(String accname) {
        final AccountManager accManager = AccountManager.get(mContext);
        final Account accounts[] = accManager.getAccounts();

        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equalsIgnoreCase(accname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    String getNextMirrorId() {
        // TODO Auto-generated method stub

        return mCurrentMirrorItem.getContactId();
    }

    @Override
    boolean addNewPhoneItemToMirror() {

        dbgTrace("Contact appending");
        return serialize(outMirrFileStream, mCurrentPhoneItem);

    }

    @Override
    boolean isUsingTrackingMethod() {
        return true;
    }

    @Override
    boolean useTrackerToAddMirrorPlusItems() {
        boolean result = true;
        if (mTrackedContact != null && mTrackedContact.size() > 0) {
            Log.d(tag, "CursorSize:" + mTrackedContact.size());

            for (int i = 0; i < mTrackedContact.size(); i++) {
                if (mAbortFlag) {
                    return false;
                }
                Log.d(tag, "ContactID:" + getNextMirrorId());
                if (mTrackedContact.get(i).getId().equals(getNextMirrorId())) {
                    if (mTrackedContact.get(i).getStatus().equals("deleted")) {
                        Log.d(tag, "Deleted ContactId: " + getNextMirrorId());

                        boolean entryExists = false;
                        for (int j = 0; j < maPlusItemChecksum.size(); j++) {
                            if (checksum == maPlusItemChecksum.get(j)) {
                                entryExists = true;
                                break;
                            }
                        }
                        if (!entryExists) {
                            result = addMirrorItemToPlus();
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    void addMirrToDataBase() {
        dbgTrace();
        mContactsDb.addToSqlDataBase(mCurrentMirrorItem, 0);
    }

    @Override
    void addMirrPlusToDataBase() {
        dbgTrace();
        mContactsDb.addToSqlDataBase(mCurrentPlusItem, 1);
    }

    public boolean append(String inMirr, String outMirr) {
        dbgTrace();

        boolean res = true;
        try {
            outMirrFileStream = new FileOutputStream(inMirr, true);
        } catch (FileNotFoundException ex) {
            dbgTrace("Exception during mirror plus file opening: " + ex.getMessage());
            return res;
        }

        Cursor newContactsCursor = mContactTrackingWrapper.getAllNewContactsCursor();

        if (null == newContactsCursor) {
            return false;
        }

        long checksum;
        for (int i = 0; i < newContactsCursor.getCount(); i++) {
            if (mAbortFlag) {
                res = false;
                break;
            }

            newContactsCursor.moveToPosition(i);
            checksum = getNextPhoneDbNewItem(newContactsCursor.getInt(newContactsCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)));
            dbgTrace("CheckSum: " + checksum + " IDS: " + newContactsCursor.getInt(newContactsCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)));
            if (-1 == checksum) {
                dbgTrace("Error getting phone db item");
                res = false;
            }

            if (!addNewPhoneItemToMirror()) {
                dbgTrace("appending");
                res = false;
            }
        }
        try {
            if (newContactsCursor != null) {
                newContactsCursor.close();
            }
            outMirrFileStream.close();

            // Strange logic..huh (Since we are using inMirrStream as outMirrorStream in this optimization,
            // we need to re-init the outMirrStream as original outMirrStream.)
            outMirrFileStream = new FileOutputStream(outMirr);
        } catch (IOException ex) {
            dbgTrace("Exception during mirror plus file opening: " + ex.getMessage());
        }

        return res;
    }

    public boolean iterateNaddToPhoneDb(ArrayList<SmartDataInfo> smartDataInfoList) {
        dbgTrace();
        boolean res = true;
        for (SmartDataInfo sdInfo : smartDataInfoList) {
            if (mAbortFlag) {
                return false;
            }
            mCurrentMirrorItem = mContactsDb.getContactForChecksum(sdInfo.getChecksum());
            if (!addMirrorItemToPhone()) {
                res = false;
                break;
            }

        }


        dbgTrace("Finished Restoring Individual Contacts");
        return res;
    }

    // Marking this class as static is required for JSON de-serialization to
    // work.
    @SuppressWarnings("unused")
    @JsonInclude(Include.NON_DEFAULT)
    public static class ContactInfo {


        public int checksum;
        public String contactId = "";
        public String note = "";
        public String displayName = "";
        public String namePrefix = "";
        public String nameSuffix = "";
        public String phoneticFname = "";
        public String phoneticGname = "";
        public String phoneticMname = "";
        public String birthday = "";
        public String firstName = "";
        public String lastName = "";
        public String middleName = "";
        public byte[] photoBitmap = null;
        public String nickname = "";


        public String accountName = "";
        public String source = "";

        // public int relType; // default is made automatic with value 0
        // public String rawContactId1;
        // public String rawContactId2;

        public int aggregationMode;


        // public String groupName = "";
        public String groupSourceId = "";

        public int timesContacted = 0;
        public long lastTimeContacted = 0;
        public int sendToVoiceMail = 0;

        public String customRingtone = "";
        public String isStarred = "";
        // public String predefinedTags = "";


        @JsonIgnore
        public int isDeleted;   // variable
        // public int dirty;
        // public boolean ShouldBeMatched = true;
        // public boolean isContactMatching = false;

        public ArrayList<EmailList> emailList = new ArrayList<EmailList>();
        public ArrayList<PhoneNumberList> phoneNumberList = new ArrayList<PhoneNumberList>();
        public ArrayList<IMList> imList = new ArrayList<IMList>();

        public ArrayList<EventList> eventList = new ArrayList<EventList>();
        public ArrayList<WebsitesList> websiteList = new ArrayList<WebsitesList>();
        public ArrayList<SocialList> socialList = new ArrayList<SocialList>();
        public ArrayList<PostalAddresses> postalAddresses = new ArrayList<PostalAddresses>();

        public ArrayList<OrganizationList> organizationList = new ArrayList<OrganizationList>();
        public ArrayList<Relationship> relationship = new ArrayList<Relationship>();

        public ArrayList<String> groupNames = new ArrayList<String>();
        public ArrayList<String> groupRowIds = new ArrayList<String>();


        // this is crucial for logic. see the documentation.

        public long crc32() {
            if (this.contactId == null) {
                return 0;
            }
            CRC32 checksum = new CRC32();

            checksum.update(this.displayName.getBytes(), 0, this.displayName.length());
            // checksum.update(this.accountName.getBytes(), 0,
            // this.accountName.length());
            int emailsCount = emailList.size();
            for (int i = 0; i < emailsCount; i++) {
                checksum.update(this.emailList.get(i).emailId.getBytes(), 0, this.emailList.get(i).emailId.length());
            }
            int phoneNumberCount = phoneNumberList.size();
            for (int i = 0; i < phoneNumberCount; i++) {
                checksum.update(this.phoneNumberList.get(i).number.getBytes(), 0, this.phoneNumberList.get(i).number.length());
            }
            int postalAddressCount = postalAddresses.size();
            for (int i = 0; i < postalAddressCount; i++) {
                checksum.update(this.postalAddresses.get(i).address.getBytes(), 0, this.postalAddresses.get(i).address.length());
            }

            return checksum.getValue();
        }

        public String getContactId() {
            return contactId;
        }

        public void setContactId(String contactId) {
            this.contactId = contactId;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setIsStarred(String isStarred) {
            this.isStarred = isStarred;
        }

        public String getNote() {
            return note;
        }

        public String getDisplay_name() {
            return displayName;
        }

        public void setDisplay_name(String display_name) {
            this.displayName = display_name;
        }

        public String getNickname() {
            return nickname;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public void setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public String getNameSuffix() {
            return nameSuffix;
        }

        public void setNameSuffix(String nameSuffix) {
            this.nameSuffix = nameSuffix;
        }

        public String getPhonetic_fname() {
            return phoneticFname;
        }

        public void setPhonetic_fname(String phonetic_fname) {
            this.phoneticFname = phonetic_fname;
        }

        public String getPhonetic_mname() {
            return phoneticMname;
        }

        public void setPhonetic_mname(String phonetic_mname) {
            this.phoneticMname = phonetic_mname;
        }

        public String getPhonetic_gname() {
            return phoneticGname;
        }

        public void setPhonetic_gname(String phonetic_lname) {
            this.phoneticGname = phonetic_lname;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public void setMiddleName(String middleName) {
            this.middleName = middleName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public byte[] getPhotoBitmap() {
            return photoBitmap;
        }

        public ArrayList<EmailList> getEmailList() {
            return emailList;
        }

        public ArrayList<EventList> getEventList() {
            return eventList;
        }

        public ArrayList<IMList> getImList() {
            return imList;
        }

        public ArrayList<OrganizationList> getOrganizationList() {
            return organizationList;
        }

        public ArrayList<PhoneNumberList> getPhoneNumberList() {
            return phoneNumberList;
        }

        public ArrayList<PostalAddresses> getPostalAddresses() {
            return postalAddresses;
        }

        public ArrayList<WebsitesList> getWebsiteList() {
            return websiteList;
        }

        public ArrayList<Relationship> getRelationship() {
            return relationship;
        }

        public String getCustomRingtone() {
            return customRingtone;
        }

        public void setCustomRingtone(String customRingtone) {
            this.customRingtone = customRingtone;
        }

        public int getTimesContacted() {
            return timesContacted;
        }

        public void setTimesContacted(int timesContacted) {
            this.timesContacted = timesContacted;
        }

        public long getLastTimeContacted() {
            return lastTimeContacted;
        }

        public void setLastTimeContacted(long lastTimeContacted) {
            this.lastTimeContacted = lastTimeContacted;
        }

        public int getSendToVoiceMail() {
            return sendToVoiceMail;
        }

        public void setSendToVoiceMail(int sendToVoiceMail) {
            this.sendToVoiceMail = sendToVoiceMail;
        }
    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class SocialList {

        private String type = "";
        private String profileName = "";
        private String customProfileName = "";
        private String service = "";
        private String url = "";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProfileName() {
            return profileName;
        }

        public void setProfileName(String profileName) {
            this.profileName = profileName;
        }

        public String getCustomProfileName() {
            return customProfileName;
        }

        public void setCustomProfileName(String customProfileName) {
            this.customProfileName = customProfileName;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class Relationship {

        private String relationName = "";
        private int relationType;
        private String relationLabel = "";

        public String getRelationName() {
            return relationName;
        }

        public void setRelationName(String relationName) {
            this.relationName = relationName;
        }

        public int getRelationType() {
            return relationType;
        }

        public void setRelationType(int relationType) {
            this.relationType = relationType;
        }

        public String getRelationLabel() {
            return relationLabel;
        }

        public void setRelationLabel(String relationLabel) {
            this.relationLabel = relationLabel;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class WebsitesList {

        private int websiteType;
        private String website = "";
        private String websiteLabel = "";

        public int getWebsiteType() {
            return websiteType;
        }

        public void setWebsiteType(int websiteType) {
            this.websiteType = websiteType;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }

        public String getWebsiteLabel() {
            return websiteLabel;
        }

        public void setWebsiteLabel(String websiteLabel) {
            this.websiteLabel = websiteLabel;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class PostalAddresses {

        private int type;
        private String address = "";
        private String street = "";
        // private String PoBox = "";
        private String neighbourhood = "";
        private String city = "";
        private String region = "";
        private String zipcode = "";
        private String country = "";
        private String label = "";
        private String countryCode = "";

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        // public String getPoBox() {
        // return PoBox;
        // }
        //
        // public void setPoBox(String poBox) {
        // PoBox = poBox;
        // }

        public String getNeighbourhood() {
            return neighbourhood;
        }

        public void setNeighbourhood(String neighbourhood) {
            this.neighbourhood = neighbourhood;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getZipcode() {
            return zipcode;
        }

        public void setZipcode(String zipcode) {
            this.zipcode = zipcode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class PhoneNumberList {

        private String number = "";
        private int type;
        private String label = "";

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class OrganizationList {

        private String organizationName = "";
        private String title = "";
        private int type;
        private String jobDepartment = "";
        private String jobDescription = "";
        private String officeSymbol = "";
        private String officePhoneticName = "";
        private String officeLocation = "";
        private String typeLabel = "";

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getJobDescription() {
            return jobDescription;
        }

        public void setJobDescription(String jobDesc) {
            jobDescription = jobDesc;
        }

        public String getJobDepartment() {
            return jobDepartment;
        }

        public void setJobDepartment(String jobDepartment) {
            this.jobDepartment = jobDepartment;
        }

        public String getOfficeSymbol() {
            return officeSymbol;
        }

        public void setOfficeSymbol(String officeSymbol) {
            this.officeSymbol = officeSymbol;
        }

        public String getOfficePhoneticName() {
            return officePhoneticName;
        }

        public void setOfficePhoneticName(String officePhoneticName) {
            this.officePhoneticName = officePhoneticName;
        }

        public String getOfficeLocation() {
            return officeLocation;
        }

        public void setOfficeLocation(String officeLocation) {
            this.officeLocation = officeLocation;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class IMList {

        private String imAddress = "";
        private int imType;
        private String customType = "";
        private String imProtocol = "";
        private String customProtocol = "";

        public String getImAddress() {
            return imAddress;
        }

        public void setImAddress(String imAddress) {
            this.imAddress = imAddress;
        }

        public int getImType() {
            return imType;
        }

        public void setImType(int imType) {
            this.imType = imType;
        }

        public String getImProtocol() {
            return imProtocol;
        }

        public void setImProtocol(String imProtocol) {
            this.imProtocol = imProtocol;
        }

        public String getCustomType() {
            return customType;
        }

        public void setCustomType(String customType) {
            this.customType = customType;
        }

        public String getCustomProtocol() {
            return customProtocol;
        }

        public void setCustomProtocol(String customProtocol) {
            this.customProtocol = customProtocol;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class EventList {

        private String startDate = "";
        private int etype = 0;
        private String eventLabel = "";

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public int getEtype() {
            return etype;
        }

        public void setEtype(int etype) {
            this.etype = etype;
        }

        public String getEventLabel() {
            return eventLabel;
        }

        public void setEventLabel(String eventLabel) {
            this.eventLabel = eventLabel;
        }

    }

    @JsonInclude(Include.NON_DEFAULT)
    public static class EmailList {

        private int emailType;
        private String emailId = "";
        private String emailLabel = "";

        public int getEmailType() {
            return emailType;
        }

        public void setEmailType(int emailType) {
            this.emailType = emailType;
        }

        public String getEmailID() {
            return emailId;
        }

        public void setEmailID(String emailID) {
            this.emailId = emailID;
        }

        public String getEmailLabel() {
            return emailLabel;
        }

        public void setEmailLabel(String emailLabel) {
            this.emailLabel = emailLabel;
        }

    }


}
