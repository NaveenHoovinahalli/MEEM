package com.meem.androidapp;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;

public class SmsReceiver extends BroadcastReceiver {

    private static final String tag = "SmsReciever";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        Log.d(tag, "Msg recieved ");
        @SuppressWarnings("unused") String recMsgString = "";

        // The sms URI was not made standard before API level 19(kitkat), The
        // standard API is Telephony.Sms.CONTENT_URI
        Uri messageUri = Uri.parse("content://sms");

        ContentResolver cr = context.getContentResolver();

        Cursor c = cr.query(messageUri, null, null, null, null);
        c.moveToFirst();

        ArrayList<String> columnNames = new ArrayList<String>();
        for (String colName : c.getColumnNames()) {
            columnNames.add(colName);
        }
        c.close();

        SmsMessage recMsg = null;
        SmsMsg smsObj = new SmsMsg();

        if (bundle != null) {
            // ---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            ContentValues contentValues = new ContentValues();
            for (int i = 0; i < pdus.length; i++) {
                recMsg = SmsMessage.createFromPdu((byte[]) pdus[i]);

                smsObj.body = recMsg.getDisplayMessageBody();
                // Log.d(tag, "Received SMS : " +smsObj.body);
                if (smsObj.body != null) {
                    if (columnNames.contains("body")) {
                        contentValues.put("body", smsObj.body);
                    }
                }

                smsObj.address = recMsg.getOriginatingAddress();
                // Log.d(tag, "from address " + smsObj.address);
                if (smsObj.address != null) {
                    if (columnNames.contains("address")) {
                        contentValues.put("address", smsObj.address);
                    }
                }

                smsObj.service_center = recMsg.getServiceCenterAddress();
                // Log.d(tag, "\n Service center " + smsObj.service_center);
                if (smsObj.service_center != null) {
                    if (columnNames.contains("service_center")) {
                        contentValues.put("service_center", smsObj.service_center);
                    }
                }

                smsObj.status = Integer.toString(recMsg.getStatus());
                // Log.d(tag, "\n Status " + smsObj.status);
                if (!smsObj.status.equals("0")) {
                    if (columnNames.contains("status")) {
                        contentValues.put("status", smsObj.status);
                    }
                }

                smsObj.protocol = Integer.toString(recMsg.getProtocolIdentifier());
                // Log.d(tag, "\n protocol " + smsObj.protocol);
                if (columnNames.contains("protocol")) {
                    contentValues.put("protocol", smsObj.protocol);
                }

                smsObj.subject = recMsg.getPseudoSubject();
                // Log.d(tag, "\n Subject " + smsObj.subject);
                if (columnNames.contains("subject")) {
                    contentValues.put("subject", smsObj.subject);
                }

                if (recMsg.isReplyPathPresent()) {
                    if (columnNames.contains("reply_path_present")) {
                        smsObj.reply_path_present = "1";
                        contentValues.put("reply_path_present", smsObj.reply_path_present);
                    }
                }

                cr.insert(messageUri, contentValues);
            }
        }
    }

    public static class SmsMsg {

        public String address = "";
        public String body = "";
        public String service_center = "";
        public String protocol = "";
        public String status = "";
        public String reply_path_present = "";
        public String subject = "";

    }

}
