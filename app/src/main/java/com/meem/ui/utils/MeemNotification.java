package com.meem.ui.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;


public class MeemNotification {


    //    Uri uri;
//    String Title;
//    String Body;
//    int smallIcon;
//    int largeIcon;
    public static final int MY_NOTIFICATION_ID = 1234;
    MainActivity mActivity;


    public MeemNotification(MainActivity mActivity) {
        this.mActivity = mActivity;
    }

    public void Notify(String Title, String Body, Uri uri) {

        if (mActivity != null) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(mActivity);
            builder.setAutoCancel(true).setContentText(Body).setContentTitle(Title).setSmallIcon(R.drawable.imageedit_tran);
            Intent myIntent = new Intent(mActivity, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    mActivity,
                    0,
                    myIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(MainActivity.NOTIFICATION_SERVICE);
            notificationManager.notify(MY_NOTIFICATION_ID, builder.build());

            //    mNotificationManager.cancelAll();
        }

    }

    public void clear() {
        NotificationManager oldNoti = (NotificationManager) mActivity.getSystemService(mActivity.NOTIFICATION_SERVICE);
        oldNoti.cancel(MY_NOTIFICATION_ID);
    }

}
