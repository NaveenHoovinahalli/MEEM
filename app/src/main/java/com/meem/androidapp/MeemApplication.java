package com.meem.androidapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.AVAILABLE_MEM_SIZE;
import static org.acra.ReportField.ENVIRONMENT;
import static org.acra.ReportField.EVENTSLOG;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.THREAD_DETAILS;
import static org.acra.ReportField.USER_COMMENT;
import static org.acra.ReportField.USER_CRASH_DATE;

/**
 * @author Arun T A
 */

@ReportsCrashes(formKey = "", // will not be used
        customReportContent = {APP_VERSION_CODE, ANDROID_VERSION, PHONE_MODEL, PRODUCT, STACK_TRACE, ENVIRONMENT, USER_CRASH_DATE, AVAILABLE_MEM_SIZE, THREAD_DETAILS, EVENTSLOG, LOGCAT, USER_COMMENT},

        mailTo = ProductSpecs.CONTACT_EMAIL, resToastText = R.string.acra_crach_notice, mode = ReportingInteractionMode.TOAST)
public class MeemApplication extends Application {
    public static final String mPrefs = "ContactTrackerServicePrefs";
    public static Context mAppContext;
    SharedPreferences sharedpreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        mAppContext = getApplicationContext();
        sharedpreferences = getSharedPreferences(mPrefs, MODE_PRIVATE);
        if (sharedpreferences.getString("mValue", "0").equals("0")) {

            Cursor mTotalContactCursor = getApplicationContext().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.DELETED + "= 0 ", null, null);
            Editor editor = sharedpreferences.edit();
            if (mTotalContactCursor != null) {
                editor.putInt("count", mTotalContactCursor.getCount());
                editor.putString("firstBackup", "1");
                editor.putString("mValue", "1");
                editor.commit();
                mTotalContactCursor.close();
            }
        }
        startService(new Intent(this, ContactTrackerService.class));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
