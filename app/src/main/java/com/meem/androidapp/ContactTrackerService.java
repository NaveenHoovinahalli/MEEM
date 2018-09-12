/**
 * Created by keyur on 4/11/15.
 */
package com.meem.androidapp;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.meem.utils.GenUtils;

/*
 * This service observers any change in contact class
 */
public class ContactTrackerService extends Service {

    private static final String tag = "ContactTrackerService";
    private static final String mPrefs = "ContactTrackerServicePrefs";
    private static final int PROCESSING_DELAY = 5000;
    ContentResolver mContentResolver;
    ContactObserver mContactObserver = new ContactObserver();
    boolean mRunningFlag = false;
    SharedPreferences sharedPreferences;
    Editor editor;
    Runnable mDbUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(tag, "isRunning: " + mRunningFlag);
            new ListViewContactsLoader().execute("updateTableRaw");
        }
    };
    private Handler mHandler = new Handler();
    private int mDelayForNextProcessing = PROCESSING_DELAY; // ms

    private static void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("ContactTrackerService.log", trace);
    }

    private static void dbgTrace() {
        GenUtils.logMethodToFile("ContactTrackerService.log");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        dbgTrace();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        dbgTrace();
        super.onCreate();
        // getting ContentResolver for registering and query to content provider
        mContentResolver = getContentResolver();
        sharedPreferences = getSharedPreferences(mPrefs, MODE_PRIVATE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dbgTrace();
        // Registering ContactObserver for RawContacts
        mContentResolver.registerContentObserver(RawContacts.CONTENT_URI, false, mContactObserver);
        new ListViewContactsLoader().execute("onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        dbgTrace();
        mContentResolver.unregisterContentObserver(mContactObserver);
        if (mHandler != null) {
            mHandler.removeCallbacks(mDbUpdateRunnable);
        }
        Intent mRestartServiceCall = new Intent(getApplicationContext(), ContactTrackerBroadcast.class);
        mRestartServiceCall.putExtra("service", "1");
        sendBroadcast(mRestartServiceCall);

        super.onDestroy();
    }

    private void updateTableRaw() {
        mRunningFlag = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mDbUpdateRunnable);
        }
        mDelayForNextProcessing = PROCESSING_DELAY;

        // get list of non deleted contacts
        Cursor rawContactCursor = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts.DELETED + "= 0 ", null, null);

        int count = sharedPreferences.getInt("count", 0);

        if (rawContactCursor != null && rawContactCursor.getCount() > 0) {

            Log.d(tag, "Current counter: " + rawContactCursor.getCount() + " Previous Counter: " + count);

            if (count > rawContactCursor.getCount()) {
                // if any deleted contact found
                Log.d(tag, "Deleted contact found");
                Cursor deletedCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);
                Log.d(tag, "Deleted cursor: " + deletedCursor.getCount());
                if (deletedCursor.getCount() > 0) {
                    // compare local database with contact database for deletion
                    for (int i = 0; i < deletedCursor.getCount(); i++) {
                        deletedCursor.moveToPosition(i);
                        Cursor delCheck = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + "= " + deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                        Log.d(tag, "Deleted [inner] cursor count: " + delCheck.getCount());

                        if (delCheck.getCount() > 0) {
                            delCheck.moveToFirst();

                            if (delCheck.getString(delCheck.getColumnIndex(RawContacts.DELETED)).equals("1")) {
                                // deleted contact found

                                ContentValues ids = new ContentValues();

                                ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");

                                int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                                Log.d(tag, "Deleted [inner] contact found: " + deleted);

                            }
                        } else {

                            // deleted contact found
                            ContentValues ids = new ContentValues();

                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                            int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                            Log.d(tag, "Outer else part inner deleted contact found (only Keyur knows what is this!) :" + deleted);
                        }
                        if (delCheck != null) {
                            delCheck.close();
                        }
                    }
                    if (deletedCursor != null) {
                        deletedCursor.close();
                    }
                }
            } else if (count == rawContactCursor.getCount()) {
                // processing if any updated contact found
                Log.d(tag, "Checking for updates in contacts");
                Cursor updateCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);
                if ((updateCursor != null) && (updateCursor.getCount() > 0)) {
                    Log.d(tag, "Updated counter: " + updateCursor.getCount());

                    if (updateCursor.getCount() > 0) {
                        for (int i = 0; i < updateCursor.getCount(); i++) {
                            updateCursor.moveToPosition(i);
                            Cursor updateCheckCursor = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + " = " + updateCursor.getString(updateCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                            // Log.d(tag, "Updated internal counter: " + updateCheckCursor.getCount());
                            if (updateCheckCursor.getCount() > 0) {
                                updateCheckCursor.moveToFirst();
                                if (updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)) > updateCursor.getInt(updateCursor.getColumnIndex(ContactTrackerDataBase.VERSION_CODE))) {
                                    // updated contact founded in database
                                    // according to version code field
                                    Log.d(tag, "Updated internal version: " + updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)));
                                    ContentValues ids = new ContentValues();
                                    ids.put(ContactTrackerDataBase.CONTACT_STATUS, "updated");
                                    ids.put(ContactTrackerDataBase.VERSION_CODE, updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)));
                                    mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + "= ?", new String[]{updateCursor.getString(updateCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                                }
                            }
                            if (updateCheckCursor != null) {
                                updateCheckCursor.close();
                            }
                        }

                    }

                } else {
                    Log.d(tag, "Apparently, running first time. ");

                    if (rawContactCursor != null && rawContactCursor.getCount() > 0) {
                        for (int i = 0; i < rawContactCursor.getCount(); i++) {
                            rawContactCursor.moveToPosition(i);
                            ContentValues ids = new ContentValues();
                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "new");
                            ids.put(ContactTrackerDataBase.CONTACT_RAW_ID, rawContactCursor.getString(rawContactCursor.getColumnIndex(BaseColumns._ID)));
                            ids.put(ContactTrackerDataBase.VERSION_CODE, rawContactCursor.getString(rawContactCursor.getColumnIndex(RawContacts.VERSION)));
                            mContentResolver.insert(ContactTrackerDataBase.CONTENT_URI, ids);
                        }
                    }
                }
                if (updateCursor != null) {
                    updateCursor.close();
                }
            } else {
                Log.d(tag, "New contact found: adding");
                // adding new inserted contact in contact class.
                if (rawContactCursor != null && rawContactCursor.getCount() > 0) {

                    for (int i = count; i < rawContactCursor.getCount(); i++) {
                        rawContactCursor.moveToPosition(i);
                        ContentValues ids = new ContentValues();
                        ids.put(ContactTrackerDataBase.CONTACT_STATUS, "new");
                        ids.put(ContactTrackerDataBase.CONTACT_RAW_ID, rawContactCursor.getString(rawContactCursor.getColumnIndex(BaseColumns._ID)));
                        ids.put(ContactTrackerDataBase.VERSION_CODE, rawContactCursor.getString(rawContactCursor.getColumnIndex(RawContacts.VERSION)));
                        mContentResolver.insert(ContactTrackerDataBase.CONTENT_URI, ids);
                    }
                }
            }
        } else {
            // if any deleted contact found
            Log.d(tag, "All contacts has been deleted");
            Cursor deletedCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);
            Log.d(tag, "Deleted cursor count: " + deletedCursor.getCount());
            if (deletedCursor.getCount() > 0) {
                // compare local database with contact database for deletion
                for (int i = 0; i < deletedCursor.getCount(); i++) {
                    deletedCursor.moveToPosition(i);
                    Cursor DelCheck = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + "= " + deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                    Log.d(tag, "Deleted [inner] cursor count: " + DelCheck.getCount());

                    if (DelCheck.getCount() > 0) {
                        DelCheck.moveToFirst();

                        if (DelCheck.getString(DelCheck.getColumnIndex(RawContacts.DELETED)).equals("1")) {

                            // deleted contact found

                            ContentValues ids = new ContentValues();

                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                            int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                            Log.d(tag, "Deleted [inner] contact found: " + deleted);
                        }
                    } else {

                        // deleted contact found
                        ContentValues ids = new ContentValues();

                        ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                        int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                        Log.d(tag, " Outer else part inner deleted (only Keyu knows what is this!) ::" + deleted);

                    }
                    if (DelCheck != null) {
                        DelCheck.close();
                    }
                }
                if (deletedCursor != null) {
                    deletedCursor.close();
                }
            }
        }
        editor = sharedPreferences.edit();
        editor.putBoolean("sync", false);

        editor.putInt("count", (rawContactCursor != null) ? rawContactCursor.getCount() : 0);
        editor.commit();

        if (rawContactCursor != null) {
            rawContactCursor.close();
        }
    }

    private void intialUpdateTableRaw() {

        Log.d(tag, "Initialy update:");

        editor = sharedPreferences.edit();
        editor.putBoolean("sync", true);
        editor.commit();

        // get list of non deleted contacts
        Cursor rawContactCursor = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts.DELETED + "= 0 ", null, null);
        int count = sharedPreferences.getInt("count", 0);
        if (rawContactCursor != null && rawContactCursor.getCount() > 0) {
            Log.d(tag, "Current Counter: " + rawContactCursor.getCount() + " Previous Counter: " + count);
            if (count > rawContactCursor.getCount()) {
                // if any deleted contact found
                Log.d(tag, "Deleted contact found");
                Cursor deletedCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);
                Log.d(tag, "Deleted cursor: " + deletedCursor.getCount());
                if (deletedCursor.getCount() > 0) {
                    // compare local database with contact database for deletion
                    for (int i = 0; i < deletedCursor.getCount(); i++) {
                        deletedCursor.moveToPosition(i);
                        Cursor DelCheck = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + "= " + deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                        Log.d(tag, "Deleted inner cursor: " + DelCheck.getCount());

                        if (DelCheck.getCount() > 0) {
                            DelCheck.moveToFirst();
                            if (DelCheck.getString(DelCheck.getColumnIndex(RawContacts.DELETED)).equals("1")) {
                                // deleted contact found
                                ContentValues ids = new ContentValues();
                                ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                                int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                                Log.d(tag, "Deleted inner deleted: " + deleted);

                            }
                        } else {

                            // deleted contact found
                            ContentValues ids = new ContentValues();
                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                            int deleted = mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                            Log.d(tag, " Outer Else part inner contact found deleted: " + deleted);

                        }
                        if (DelCheck != null) {
                            DelCheck.close();
                        }
                    }
                    if (deletedCursor != null) {
                        deletedCursor.close();
                    }
                }
            } else if (count == rawContactCursor.getCount()) {
                // processing if any updated contact found
                Log.d(tag, "Updated contact found");
                Cursor updateCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);

                if ((updateCursor != null) && (updateCursor.getCount() > 0)) {
                    Log.d(tag, "Updated counter: " + updateCursor.getCount());

                    // is no need to of this every time when service is started
                    if (updateCursor.getCount() < rawContactCursor.getCount()) {

                        Log.d(tag, "Apparently, running 2nd time. ");

                        for (int i = 0; i < rawContactCursor.getCount(); i++) {
                            rawContactCursor.moveToPosition(i);
                            ContentValues ids = new ContentValues();
                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "new");
                            ids.put(ContactTrackerDataBase.CONTACT_RAW_ID, rawContactCursor.getString(rawContactCursor.getColumnIndex(BaseColumns._ID)));
                            ids.put(ContactTrackerDataBase.VERSION_CODE, rawContactCursor.getString(rawContactCursor.getColumnIndex(RawContacts.VERSION)));
                            mContentResolver.insert(ContactTrackerDataBase.CONTENT_URI, ids);
                        }
                        Log.d(tag, "Apparently, running 2nd time finisdhed. ");
                    }
                    for (int i = 0; i < updateCursor.getCount(); i++) {
                        updateCursor.moveToPosition(i);
                        Cursor updateCheckCursor = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + " = " + updateCursor.getString(updateCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                        //Log.d(tag, "Updated internal counter: " + updateCheckCursor.getCount());
                        if (updateCheckCursor.getCount() > 0) {
                            updateCheckCursor.moveToFirst();
                            if (updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)) > updateCursor.getInt(updateCursor.getColumnIndex(ContactTrackerDataBase.VERSION_CODE))) {

								/*
                                 * updated contact founded in database according
								 * to version code field
								 */

                                Log.d(tag, "Updating contact internal version: " + updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)));
                                ContentValues ids = new ContentValues();
                                ids.put(ContactTrackerDataBase.CONTACT_STATUS, "updated");
                                ids.put(ContactTrackerDataBase.VERSION_CODE, updateCheckCursor.getInt(updateCheckCursor.getColumnIndex(RawContacts.VERSION)));
                                mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + "= ?", new String[]{updateCursor.getString(updateCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                            }
                        }
                        if (updateCheckCursor != null) {
                            updateCheckCursor.close();
                        }
                    }

                } else {
                    Log.d(tag, "Apparently, running first time. ");
                    if (rawContactCursor != null && rawContactCursor.getCount() > 0) {

                        for (int i = 0; i < rawContactCursor.getCount(); i++) {
                            rawContactCursor.moveToPosition(i);
                            ContentValues ids = new ContentValues();
                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "new");
                            ids.put(ContactTrackerDataBase.CONTACT_RAW_ID, rawContactCursor.getString(rawContactCursor.getColumnIndex(BaseColumns._ID)));
                            ids.put(ContactTrackerDataBase.VERSION_CODE, rawContactCursor.getString(rawContactCursor.getColumnIndex(RawContacts.VERSION)));
                            mContentResolver.insert(ContactTrackerDataBase.CONTENT_URI, ids);
                        }
                        Log.d(tag, "Apparently, running first time finished.");
                    }
                }
                if (updateCursor != null) {
                    updateCursor.close();
                }
            } else {
                Log.d(tag, "New contact found: adding");

                // adding new inserted contact in contact class.
                for (int i = count; i < rawContactCursor.getCount(); i++) {
                    rawContactCursor.moveToPosition(i);
                    ContentValues ids = new ContentValues();
                    ids.put(ContactTrackerDataBase.CONTACT_STATUS, "new");
                    ids.put(ContactTrackerDataBase.CONTACT_RAW_ID, rawContactCursor.getString(rawContactCursor.getColumnIndex(BaseColumns._ID)));
                    ids.put(ContactTrackerDataBase.VERSION_CODE, rawContactCursor.getString(rawContactCursor.getColumnIndex(RawContacts.VERSION)));
                    mContentResolver.insert(ContactTrackerDataBase.CONTENT_URI, ids);
                }
            }
        } else {
            Log.d(tag, "All contacts has been deleted!");

            Cursor deletedCursor = mContentResolver.query(ContactTrackerDataBase.CONTENT_URI, null, null, null, null);
            Log.d(tag, "Deleted cursor: " + deletedCursor.getCount());
            if (deletedCursor.getCount() > 0) {
                // compare local database with contact database for deletion
                for (int i = 0; i < deletedCursor.getCount(); i++) {
                    deletedCursor.moveToPosition(i);
                    Cursor DelCheck = mContentResolver.query(RawContacts.CONTENT_URI, null, RawContacts._ID + "= " + deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID)), null, null);
                    Log.d(tag, "Deleted inner cursor: " + DelCheck.getCount());

                    if (DelCheck.getCount() > 0) {
                        DelCheck.moveToFirst();
                        if (DelCheck.getString(DelCheck.getColumnIndex(RawContacts.DELETED)).equals("1")) {
                            // deleted contact found
                            Log.d(tag, "Deleted [inner] deleted contact found: " + DelCheck.getString(DelCheck.getColumnIndex(RawContacts.DELETED)));
                            ContentValues ids = new ContentValues();
                            ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                            mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                        } else {
                            Log.d(tag, "Else part [inner] deleted contact found: " + DelCheck.getString(DelCheck.getColumnIndex(RawContacts.DELETED)));
                        }
                    } else {
                        Log.d(tag, " Outer Else part inner deleted (only Keyur knowns what is this!)");
                        // deleted contact found
                        ContentValues ids = new ContentValues();
                        ids.put(ContactTrackerDataBase.CONTACT_STATUS, "deleted");
                        mContentResolver.update(ContactTrackerDataBase.CONTENT_URI, ids, ContactTrackerDataBase.CONTACT_RAW_ID + " = ? ", new String[]{deletedCursor.getString(deletedCursor.getColumnIndex(ContactTrackerDataBase.CONTACT_RAW_ID))});
                    }
                    if (DelCheck != null) {
                        DelCheck.close();
                    }
                }
                if (deletedCursor != null) {
                    deletedCursor.close();
                }

            }
        }
        editor = sharedPreferences.edit();
        editor.putBoolean("sync", false);

        editor.putInt("count", (rawContactCursor != null) ? rawContactCursor.getCount() : 0);
        editor.commit();
        if (rawContactCursor != null) {
            rawContactCursor.close();
        }
    }

    private class ContactObserver extends ContentObserver {
        public ContactObserver() {
            super(null);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(tag, "Onchanged");
            editor = sharedPreferences.edit();
            editor.putBoolean("sync", true);
            editor.commit();
            if (mRunningFlag) {
                mDelayForNextProcessing += PROCESSING_DELAY;
                if (mDelayForNextProcessing >= 10000) {
                    mDelayForNextProcessing = PROCESSING_DELAY;
                }
                mHandler.removeCallbacks(mDbUpdateRunnable);
                mHandler.postDelayed(mDbUpdateRunnable, mDelayForNextProcessing);
            } else {
                mRunningFlag = true;
                mHandler.postDelayed(mDbUpdateRunnable, mDelayForNextProcessing);
            }
        }
    }

    private class ListViewContactsLoader extends AsyncTask<String, Void, Cursor> {
        @Override
        protected Cursor doInBackground(String... params) {
            dbgTrace("ContactsLoader: AsyncTask starting");

            // TODO: Arun: 03July2018: Check for permission and return as needed (this will happen during app uninstall).
            // Check crash report mails on 14 June 2018 to arun@meemmemory.com from ta.arun@gmail.com

            if (params[0].equals("onStartCommand")) {
                intialUpdateTableRaw();
            } else if (params[0].equals("updateTableRaw")) {
                updateTableRaw();
            }
            return null;
        }
    }
}
