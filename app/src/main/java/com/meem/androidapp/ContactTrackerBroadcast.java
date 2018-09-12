package com.meem.androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ContactTrackerBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.getApplicationContext().startService(new Intent(context.getApplicationContext(), ContactTrackerService.class));
    }
}
