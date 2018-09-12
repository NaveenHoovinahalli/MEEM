package com.meem.phone;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.meem.androidapp.MeemApplication;
import com.meem.androidapp.ProductSpecs;

/**
 * NOTE: 03Aug2015: This class is useless for now for the app e need to get the count of these smart data cats as soon as we start backup -
 * so we need to query these guys from main thread itself. But android lollipop on Nexus simply locks up and show ANR - especially if there
 * are large number of SMS.
 *
 * @author Keyur Thumar, Arun
 */
public class SmartDataStats {

    public SmartDataStats() {
        super();
    }

    // we need to pass ContactsContract.RawContacts.DELETED + " = 0 " this selection filter while using contact
    public int smartDataTotalCount(Uri uri, String columName) {
        int mValue = 0;

        try {
            Cursor countCursor = MeemApplication.mAppContext.getContentResolver().query(uri, null, columName, null, null);

            if (countCursor != null) {
                mValue = countCursor.getCount();
                countCursor.close();
            }
        } catch (Exception e) {
            Log.wtf("SmartDataStats", "Exception while getting count of: " + uri, e);
        }

        return mValue;
    }

    public int contactTotalCount() {
        // return smartDataTotalCount(ContactsContract.RawContacts.CONTENT_URI, ContactsContract.RawContacts.DELETED + " = 0 ");
        return ProductSpecs.ANDROID_CONTACTS_COUNT_BUG_WORKAROUND;
    }

    public int messageTotalCount() {
        /**
         * A bug in the Lollipop on Nexus5 prevents the use of this query from
         * UI thread. So, until a better solution is found, we will blindly
         * return 500 sms. No harm done as this information is used only for
         * initial time estimation.
         */
        // return smartDataTotalCount(Uri.parse("content://sms"), null);
        return ProductSpecs.ANDROID_SMS_COUNT_BUG_WORKAROUND;
    }

    public int calenderTotalCount() {
        // return smartDataTotalCount(Events.CONTENT_URI, null);
        return ProductSpecs.ANDROID_CALENDAR_COUNT_BUG_WORKAROUND;
    }
}
