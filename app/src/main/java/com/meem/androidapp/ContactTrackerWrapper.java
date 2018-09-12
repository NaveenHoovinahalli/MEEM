package com.meem.androidapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;

/*
 * Wrapper class for accessing local table contact database if their is any
 * change
 */
public class ContactTrackerWrapper {
    public static final int CONTACT_PROCESS = 10;
    public static final int CONTACT_APPEND_PROCESS = 20;
    public static final int CONTACT_APPEND_NEW = 30;
    public static final int CONTACT_BOTH_PROCESS_PREPARE = 50;
    public static final int CONTACT_PREPARE = 60;
    public static final int CONTACT_SKIP = 0;
    public static String tag = "ContactTrackingWrapper";
    public static int CONTACT_STATUS_DELETED = 1;
    public static int CONTACT_STATUS_UPDATED = 3;
    public static int CONTACT_STATUS_NEWCONTACTS = 2;
    ContentResolver mContentResolver;
    ArrayList<TrackedContact> mTrackedContactList;
    int[] mNewTrackedContactList;

    SharedPreferences sharedpreferences;

    @SuppressWarnings("static-access")
    public ContactTrackerWrapper() {
        super();
        mContentResolver = MeemApplication.mAppContext.getContentResolver();
        sharedpreferences = MeemApplication.mAppContext.getSharedPreferences(MeemApplication.mPrefs, MeemApplication.mAppContext.MODE_PRIVATE);

    }

    public int getDeleteContactDbStatus() {
        int res = 0;
        Cursor mDelContacts = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, ContactTrackerDataBase.CONTACT_STATUS + " =? ", new String[]{"deleted"}, null);

        if (mDelContacts != null && mDelContacts.getCount() > 0) {
            res = CONTACT_STATUS_DELETED;
        }

        if (mDelContacts != null) {
            mDelContacts.close();
        }

        return res;
    }

    public int getNewContactDbStatus() {
        int res = 0;
        Cursor mNewContacts = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, ContactTrackerDataBase.CONTACT_STATUS + " =? ", new String[]{"new"}, null);
        if (mNewContacts != null && mNewContacts.getCount() > 0) {
            res = CONTACT_STATUS_NEWCONTACTS;
        }

        if (mNewContacts != null) {
            mNewContacts.close();
        }

        return res;
    }

    public int getUpdatedContactDbStatus() {
        int res = 0;
        Cursor mUpdatedContacts = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, ContactTrackerDataBase.CONTACT_STATUS + " =? ", new String[]{"updated"}, null);

        if (mUpdatedContacts != null && mUpdatedContacts.getCount() > 0) {

            res = CONTACT_STATUS_UPDATED;
        }

        if (mUpdatedContacts != null) {
            mUpdatedContacts.close();
        }

        return res;
    }

    public int getContactDbStatus() {

        int temp = 0;
        int res = 0;

        if (getDeleteContactDbStatus() != 0) {
            temp += CONTACT_STATUS_DELETED; // 1 - only process
            Log.d("ContactTracker", "deleted contact found");
        }

        if (getNewContactDbStatus() != 0) {
            // 1 APPEND_PREPARE_PROCESS, 0 - APPEND_NEW
            switch (temp) {
                case 1:
                    res = CONTACT_APPEND_PROCESS;
                    break;
            }
            temp += CONTACT_STATUS_NEWCONTACTS;
            Log.d("ContactTracker", "new contact found");

        }

        if (getUpdatedContactDbStatus() != 0) // 1+2 -BOTH ,0 -PREPARE , 2
        // -APPEND_PREPARE ,1- BOTH
        {
            Log.d("ContactTracker", "updated contact found");

            switch (temp) {
                case 0:
                case 2:
                    res = CONTACT_PREPARE;
                    break;
                case 1:
                case 3:
                    res = CONTACT_BOTH_PROCESS_PREPARE;
                    break;

            }
            temp += CONTACT_STATUS_UPDATED;
        }
        if (temp == 1) {
            res = CONTACT_PROCESS;
        } else if (temp == 2) {
            if (isFristBackup().equals("1")) {
                res = CONTACT_BOTH_PROCESS_PREPARE;
            } else {
                res = CONTACT_APPEND_NEW;
            }
        }

        return res;
    }

    // returns the list of contact that is being modified
    public ArrayList<TrackedContact> getAllContact() {
        mTrackedContactList = new ArrayList<TrackedContact>();
        Cursor mDelContacts = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, ContactTrackerDataBase.CONTACT_STATUS + " = ? ", new String[]{"deleted"}, null);
        if (mDelContacts != null && mDelContacts.getCount() > 0) {
            for (int i = 0; i < mDelContacts.getCount(); i++) {
                mDelContacts.moveToPosition(i);
                TrackedContact mTrackedContact = new TrackedContact();
                mTrackedContact.setId(mDelContacts.getString(mDelContacts.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)));
                mTrackedContact.setStatus(mDelContacts.getString(mDelContacts.getColumnIndex(ContactTrackerDataBase.CONTACT_STATUS)));
                mTrackedContactList.add(mTrackedContact);
            }
        }

        if (mDelContacts != null) {
            mDelContacts.close();
        }

        return mTrackedContactList;
    }

    public Cursor getAllNewContactsCursor() {

        Cursor mDelContacts = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, ContactTrackerDataBase.CONTACT_STATUS + " =? ", new String[]{"new"}, null);

		/*
         * if (mDelContacts != null && mDelContacts.getCount() > 0) {
		 * mNewTrackedContactList=new int[mDelContacts.getCount()]; for (int i =
		 * 0; i < mDelContacts.getCount(); i++) {
		 * mDelContacts.moveToPosition(i);
		 * mNewTrackedContactList[i]=mDelContacts.getInt(mDelContacts
		 * .getColumnIndex(ContactTrackingDataBase.CONTACT_RAW_ID)); } }
		 * mDelContacts.close();
		 */

        return mDelContacts;
    }

    /**
     * important: This method must be called after backup sessions where ever contacts were being processed.
     */
    public void onContactBackupCompleted() {

        // remove deleted contact from local db after backup
        int mCount = mContentResolver.delete(ContactTrackerDataBase.CONTENT_URI, ContactTrackerDataBase.CONTACT_STATUS + "=?", new String[]{"deleted"});

        Log.d(tag, "Number of row delted after processing contact :" + mCount);
        ContentValues ids = new ContentValues();
        ids.put(ContactTrackerDataBase.CONTACT_STATUS, "backup");

        int mUpdateCount = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_STATUS + "=?", new String[]{"updated"});
        Log.d(tag, "Number of row mUpdateCount after processing contact :" + mUpdateCount);

        ContentValues update = new ContentValues();
        update.put(ContactTrackerDataBase.CONTACT_STATUS, "backup");

        int mNewCount = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, update, ContactTrackerDataBase.CONTACT_STATUS + "=?", new String[]{"new"});
        updateSharedPref();
        Log.d(tag, "Number of row mNewCount after processing contact :" + mNewCount);
    }

    public void onContactRestoreCompleted() {

        // remove deleted contact from local db after backup, we will not update
        // new updated and backuped contacts as we havent backuped yet
        /*
         * int mCount =
		 * mContentResolver.delete(ContactTrackerDataBase.CONTENT_URI,
		 * ContactTrackerDataBase.CONTACT_STATUS + "=?", new String[] {
		 * "deleted" });
		 * 
		 * Log.d(tag, "Number of rows deleted after restore contacts :" +
		 * mCount);
		 */
    }

    public boolean isSyncPending() {
        Log.d(tag, "IsSynced: " + sharedpreferences.getBoolean("sync", false));
        return sharedpreferences.getBoolean("sync", false);
    }

    public String isFristBackup() {
        String temp = sharedpreferences.getString("firstBackup", " ");
        return temp;
    }

    public void updateSharedPref() {
        Editor mEditor = sharedpreferences.edit();
        mEditor.putString("firstBackup", "2");
        mEditor.commit();
    }

    // Note: On cable reset or delete contact category operation we need to
    // update this information.
    public void resetLocalContactDatabase() {
        Editor mEditor = sharedpreferences.edit();
        mEditor.putBoolean("sync", true);
        mEditor.commit();

        ContentValues update = new ContentValues();
        update.put(ContactTrackerDataBase.CONTACT_STATUS, "new");

        int mNewCount = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, update, ContactTrackerDataBase.CONTACT_STATUS + "=?", new String[]{"backup"});
        Log.d(tag, "Number of row mbackupCount after processing contact category delete or cable reset operation: " + mNewCount);
        mEditor.putBoolean("sync", false);
        mEditor.commit();
        Log.d(tag, "resetLocalContactDatabase operation done");
    }
}
