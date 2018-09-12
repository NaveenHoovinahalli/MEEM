package com.meem.ui.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;

import com.meem.androidapp.AppPreferences;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;

import me.leolin.shortcutbadger.ShortcutBadger;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

public class BackupStatusBroadcastReceiver extends BroadcastReceiver {


    public static final int ONE_DAY = ((((60 * 1000) * 60) * 24));
    String MY_PREFS_NAME = "badge_number";
    Context mycontext;
    int badgeCount;

    @Override
    public void onReceive(Context context, Intent intent) {

        mycontext = context;

        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        badgeCount = prefs.getInt("count", 0);

        badgeCount++;

        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt("count", badgeCount);
        editor.commit();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mycontext);
        if (!sharedPreferences.getBoolean(AppPreferences.BACKUP_NOTIFICATION_STATUS, true)) {
            setAlarm();
            return;
        }

        ShortcutBadger.applyCount(context, badgeCount);
        showNotificatin();

        setAlarm();

    }


    public void setAlarm() {

        Intent intent = new Intent(mycontext, BackupStatusBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mycontext, 234324243, intent, 0);
        AlarmManager alarmManager = (AlarmManager) mycontext.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ONE_DAY, pendingIntent);
    }


    public void showNotificatin() {

        if (badgeCount > 7)
            return;

        NotificationManager notificationManager = (NotificationManager) mycontext.getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(mycontext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mycontext, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mycontext);
        android.app.Notification notification;

        String string;
        if (badgeCount == 1) {
            string = String.format(mycontext.getString(R.string.notification_message), badgeCount, mycontext.getString(R.string.day));
        } else {

            string = String.format(mycontext.getString(R.string.notification_message), badgeCount, mycontext.getString(R.string.days));
        }


        notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(mycontext.getString(R.string.app_name))
                .setContentText(string).build();

        notification.flags = Notification.FLAG_INSISTENT | Notification.FLAG_AUTO_CANCEL;


        notificationManager.notify(234324243, notification);

    }


}
