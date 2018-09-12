/*
 * ------------------------------------------------------------ Copyright (c)
 * 2014, Silvan Innovation Labs Pvt. Ltd. (http://www.silvanlabs.com). All
 * rights reserved.
 * 
 * Unauthorized distribution, redistribution or usage of this software in source
 * or binary forms are strictly prohibited.
 * ------------------------------------------------------------
 * 
 * Description: Implementation of Messages backup and restore . 19-May-2014
 * 
 * @author KARTHIK.B.S [karthik.bs@silvanlabs.com]
 */
package com.meem.v2.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.meem.androidapp.AppLocalData;
import com.meem.androidapp.UiContext;
import com.meem.ui.SmartDataInfo;
import com.meem.utils.GenUtils;
import com.meem.v2.mmp.MMPV2Constants;

import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Implementation of Messages(SMS) backup and restore methods.
 *
 * @author karthik B S
 */

public class MessagesV2 extends SmartDataCategoryV2 {
    private static final String tag = "MessagesV2";
    SmsMsg mCurrentMirrorItem, mCurrentPlusItem, mCurrentPhoneItem;
    // Sql DB related
    private MessagesDbV2 mMsgMirrDb;
    private MessagesDbV2 mMsgArchiveDb;
    private AppLocalData mAppData = AppLocalData.getInstance();

    public MessagesV2(Context context, String upid) {
        super(MMPV2Constants.MMP_CATCODE_MESSAGE, context, Uri.parse("content://sms"), null);

        mContext = context;
        ArrayList<String> mtableNamesList = new ArrayList<String>();
        mtableNamesList.add(MessagesTableNameV2.CREATE_VAULT_LINKS_COUNT_TABLE);
        mtableNamesList.add(MessagesTableNameV2.CREATE_VAULT_MESSAGE_TABLE);
        mtableNamesList.add(MessagesTableNameV2.CREATE_SMS_MSG_TABLE);

        mMsgMirrDb = new MessagesDbV2(context, mAppData.getMessageV2MirrorDbFullPath(upid), mtableNamesList);
        mMsgMirrDb.getRowCount();

//        dbgTrace("plus path: "+mAppData.getMessageV2PlusDbFullPath(upid));

        mMsgArchiveDb = new MessagesDbV2(context, mAppData.getMessageV2PlusDbFullPath(upid), mtableNamesList);
        dbgTrace("sync count: " + mMsgMirrDb.getRowCount() + ", archieve count: " + mMsgArchiveDb.getRowCount());

        dbgTrace();
    }

    public boolean iterateNaddToPhoneDb(ArrayList<SmartDataInfo> smartDataInfoList, Boolean isMirr) {
        dbgTrace();
        boolean res = true;
        for (SmartDataInfo sdInfo : smartDataInfoList) {
            if (mAbortFlag) {
                return false;
            }
            if (isMirr) {
                mCurrentMirrorItem = mMsgMirrDb.getMessageForChecksum(sdInfo.getChecksum());
            } else {
                mCurrentMirrorItem = mMsgArchiveDb.getMessageForChecksum(sdInfo.getChecksum());
            }

            if (!addMirrorItemToPhone()) {
                res = false;
                break;
            }
        }

        dbgTrace("Finished Restoring Individual Message thread");
        return true;
    }

    @Override
    long getNextPhoneDbItem() {
        dbgTrace();

        mCurrentPhoneItem = new SmsMsg();

        if (mColumnNames.contains("_id")) {
            mCurrentPhoneItem.identi2 = mCursor.getString(mCursor.getColumnIndexOrThrow("_id"));
            if (mCurrentPhoneItem.identi2 == null) {
                mCurrentPhoneItem.identi2 = "";
            }
        }
        if (mColumnNames.contains("address")) {
            mCurrentPhoneItem.address = mCursor.getString(mCursor.getColumnIndexOrThrow("address"));
            if (mCurrentPhoneItem.address == null) {
                mCurrentPhoneItem.address = "";
            }
        }
        if (mColumnNames.contains("thread_id")) {
            mCurrentPhoneItem.thread_id = mCursor.getString(mCursor.getColumnIndexOrThrow("thread_id"));
            if (mCurrentPhoneItem.thread_id == null) {
                mCurrentPhoneItem.thread_id = "";
            }
        }
        if (mColumnNames.contains("person")) {
            mCurrentPhoneItem.person = mCursor.getString(mCursor.getColumnIndexOrThrow("person"));
            if (mCurrentPhoneItem.person == null) {
                mCurrentPhoneItem.person = "";
            }
        }
        if (mColumnNames.contains("date")) {
            mCurrentPhoneItem.date = mCursor.getString(mCursor.getColumnIndexOrThrow("date"));
            if (mCurrentPhoneItem.date == null) {
                mCurrentPhoneItem.date = "";
            }
        }
        if (mColumnNames.contains("date_sent")) {
            mCurrentPhoneItem.date_sent = mCursor.getString(mCursor.getColumnIndexOrThrow("date_sent"));
            if (mCurrentPhoneItem.date_sent == null) {
                mCurrentPhoneItem.date_sent = "";
            }
        }
        if (mColumnNames.contains("protocol")) {
            mCurrentPhoneItem.protocol = mCursor.getString(mCursor.getColumnIndexOrThrow("protocol"));
            if (mCurrentPhoneItem.protocol == null) {
                mCurrentPhoneItem.protocol = "";
            }
        }
        if (mColumnNames.contains("read")) {
            mCurrentPhoneItem.read = mCursor.getString(mCursor.getColumnIndexOrThrow("read"));
            if (mCurrentPhoneItem.read == null) {
                mCurrentPhoneItem.read = "";
            }
        }
        if (mColumnNames.contains("status")) {
            mCurrentPhoneItem.status = mCursor.getString(mCursor.getColumnIndexOrThrow("status"));
            if (mCurrentPhoneItem.status == null) {
                mCurrentPhoneItem.status = "";
            }
        }
        if (mColumnNames.contains("reply_path_present")) {
            mCurrentPhoneItem.reply_path_present = mCursor.getString(mCursor.getColumnIndexOrThrow("reply_path_present"));
            if (mCurrentPhoneItem.reply_path_present == null) {
                mCurrentPhoneItem.reply_path_present = "";
            }
        }
        if (mColumnNames.contains("subject")) {
            mCurrentPhoneItem.subject = mCursor.getString(mCursor.getColumnIndexOrThrow("subject"));
            if (mCurrentPhoneItem.subject == null) {
                mCurrentPhoneItem.subject = "";
            }
        }
        if (mColumnNames.contains("body")) {
            mCurrentPhoneItem.body = mCursor.getString(mCursor.getColumnIndexOrThrow("body"));
            if (mCurrentPhoneItem.body == null) {
                mCurrentPhoneItem.body = "";
            }
        }
        if (mColumnNames.contains("service_center")) {
            mCurrentPhoneItem.service_center = mCursor.getString(mCursor.getColumnIndexOrThrow("service_center"));
            if (mCurrentPhoneItem.service_center == null) {
                mCurrentPhoneItem.service_center = "";
            }
        }
        if (mColumnNames.contains("locked")) {
            mCurrentPhoneItem.locked = mCursor.getString(mCursor.getColumnIndexOrThrow("locked"));
            if (mCurrentPhoneItem.locked == null) {
                mCurrentPhoneItem.locked = "";
            }
        }
        if (mColumnNames.contains("error_code")) {
            mCurrentPhoneItem.error_code = mCursor.getString(mCursor.getColumnIndexOrThrow("error_code"));
            if (mCurrentPhoneItem.error_code == null) {
                mCurrentPhoneItem.error_code = "";
            }
        }
        if (mColumnNames.contains("seen")) {
            mCurrentPhoneItem.seen = mCursor.getString(mCursor.getColumnIndexOrThrow("seen"));
            if (mCurrentPhoneItem.seen == null) {
                mCurrentPhoneItem.seen = "";
            }
        }
        if (mColumnNames.contains("type")) {
            mCurrentPhoneItem.type = mCursor.getString(mCursor.getColumnIndexOrThrow("type"));
            if (mCurrentPhoneItem.type == null) {
                mCurrentPhoneItem.type = "";
            }
        }
        if (mCurrentPhoneItem.type.equalsIgnoreCase(Integer.toString(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT))) {
            String phoneNumber = "";
            try {
                Cursor draftCursor = mContentResolver.query(Uri.parse("content://mms-sms/conversations?simple=true"), null, "_id = " + mCurrentPhoneItem.thread_id, null, null);
                //getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = " + recipientId, null, null);
                if (draftCursor != null) {
                    if (draftCursor.moveToNext()) {
                        phoneNumber = draftCursor.getString(3);
                        if (phoneNumber != null) {
                            //if(phoneNumber.contains("-"))
                            Log.v(" id=>" + mCurrentPhoneItem.identi2 + " thid=>" + mCurrentPhoneItem.thread_id + " first = > ", draftCursor.getString(3));
                        }
                    }
                    draftCursor.close();
                    draftCursor = mContentResolver.query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = " + phoneNumber, null, null);
                    if (draftCursor != null) {
                        if (draftCursor.moveToNext()) {
                            Log.v(mCurrentPhoneItem.thread_id + " second = > ", draftCursor.getString(1));
                            phoneNumber = draftCursor.getString(1);
                            phoneNumber = phoneNumber.replaceAll("[-() ]", "");
                            mCurrentPhoneItem.address = phoneNumber;
                        }
                        draftCursor.close();
                    }
                }
            } catch (Exception e) {
                dbgTrace("draftCursor query Failed");
                dbgTrace("Exception: " + e.getMessage());
            }

        }

        if (mColumnNames.contains("sub_id")) {
            mCurrentPhoneItem.sub_id = mCursor.getString(mCursor.getColumnIndexOrThrow("sub_id"));
            if (mCurrentPhoneItem.sub_id == null) {
                mCurrentPhoneItem.sub_id = "";
            }
        }
        if (mColumnNames.contains("semc_message_priority")) {
            mCurrentPhoneItem.semc_message_priority = mCursor.getString(mCursor.getColumnIndexOrThrow("semc_message_priority"));
            if (mCurrentPhoneItem.semc_message_priority == null) {
                mCurrentPhoneItem.semc_message_priority = "";
            }
        }
        if (mColumnNames.contains("parent_id")) {
            mCurrentPhoneItem.parent_id = mCursor.getString(mCursor.getColumnIndexOrThrow("parent_id"));
            if (mCurrentPhoneItem.parent_id == null) {
                mCurrentPhoneItem.parent_id = "";
            }
        }
        if (mColumnNames.contains("delivery_status")) {
            mCurrentPhoneItem.delivery_status = mCursor.getString(mCursor.getColumnIndexOrThrow("delivery_status"));
            if (mCurrentPhoneItem.delivery_status == null) {
                mCurrentPhoneItem.delivery_status = "";
            }
        }
        if (mColumnNames.contains("star_status")) {
            mCurrentPhoneItem.star_status = mCursor.getString(mCursor.getColumnIndexOrThrow("star_status"));
            if (mCurrentPhoneItem.star_status == null) {
                mCurrentPhoneItem.star_status = "";
            }
        }
        if (mColumnNames.contains("delivery_date")) {
            mCurrentPhoneItem.delivery_date = mCursor.getString(mCursor.getColumnIndexOrThrow("delivery_date"));
            if (mCurrentPhoneItem.delivery_date == null) {
                mCurrentPhoneItem.delivery_date = "";
            }
        }

        if (mColumnNames.contains("message_type_all")) {
            mCurrentPhoneItem.message_type_all = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_all"));

        }
        if (mColumnNames.contains("message_type_draft")) {
            mCurrentPhoneItem.message_type_draft = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_draft"));
        }
        if (mColumnNames.contains("message_type_failed")) {
            mCurrentPhoneItem.message_type_failed = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_failed"));
        }
        if (mColumnNames.contains("message_type_inbox")) {
            mCurrentPhoneItem.message_type_inbox = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_inbox"));
        }
        if (mColumnNames.contains("message_type_outbox")) {
            mCurrentPhoneItem.message_type_outbox = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_outbox"));
        }
        if (mColumnNames.contains("message_type_queued")) {
            mCurrentPhoneItem.message_type_queued = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_queued"));
        }
        if (mColumnNames.contains("message_type_sent")) {
            mCurrentPhoneItem.message_type_sent = mCursor.getInt(mCursor.getColumnIndexOrThrow("message_type_sent"));
        }

        long checksum = mCurrentPhoneItem.crc32();
        mCurrentPhoneItem.checksum = (int) checksum;
        dbgTrace("Checksum: " + checksum);

        mCursor.moveToNext();

        return checksum;
    }

    @Override
    boolean addMirrorItemToPhone() {
        dbgTrace();

        ContentValues cv = new ContentValues();

        if (mColumnNames.contains("address")) {
            cv.put("address", mCurrentMirrorItem.address);
        }
        if (mColumnNames.contains("person")) {
            cv.put("person", mCurrentMirrorItem.person);
        }
        if (mColumnNames.contains("date")) {
            cv.put("date", mCurrentMirrorItem.date);
        }
        if (mColumnNames.contains("date_sent")) {
            cv.put("date_sent", mCurrentMirrorItem.date_sent);
        }
        if (mColumnNames.contains("protocol")) {
            cv.put("protocol", mCurrentMirrorItem.protocol);
        }
        if (mColumnNames.contains("read")) {
            cv.put("read", mCurrentMirrorItem.read);
        }
        if (mColumnNames.contains("status")) {
            cv.put("status", mCurrentMirrorItem.status);
        }
        if (mColumnNames.contains("reply_path_present")) {
            cv.put("reply_path_present", mCurrentMirrorItem.reply_path_present);
        }
        if (mColumnNames.contains("subject")) {
            cv.put("subject", mCurrentMirrorItem.subject);
        }
        if (mColumnNames.contains("body")) {
            cv.put("body", mCurrentMirrorItem.body);
        }
        if (mColumnNames.contains("service_center")) {
            cv.put("service_center", mCurrentMirrorItem.service_center);
        }
        if (mColumnNames.contains("locked")) {
            cv.put("locked", mCurrentMirrorItem.locked);
        }
        if (mColumnNames.contains("error_code")) {
            cv.put("error_code", mCurrentMirrorItem.error_code);
        }
        if (mColumnNames.contains("seen")) {
            cv.put("seen", mCurrentMirrorItem.seen);
        }
        if (mColumnNames.contains("type")) {
            cv.put("type", mCurrentMirrorItem.type);
        }

        if (mColumnNames.contains("semc_message_priority")) {
            cv.put("semc_message_priority", mCurrentMirrorItem.semc_message_priority);
        }

        if (mColumnNames.contains("message_type_all")) {
            cv.put("message_type_all", mCurrentMirrorItem.message_type_all);
        }
        if (mColumnNames.contains("message_type_draft")) {
            if (mCurrentMirrorItem.message_type_draft != 0) {
                cv.put("message_type_draft", mCurrentMirrorItem.message_type_draft);
            }
        }
        if (mColumnNames.contains("message_type_failed")) {
            if (mCurrentMirrorItem.message_type_failed != 0) {
                cv.put("message_type_failed", mCurrentMirrorItem.message_type_failed);
            }
        }
        if (mColumnNames.contains("message_type_inbox")) {
            if (mCurrentMirrorItem.message_type_inbox != 0) {
                cv.put("message_type_inbox", mCurrentMirrorItem.message_type_inbox);
            }
        }
        if (mColumnNames.contains("message_type_outbox")) {
            if (mCurrentMirrorItem.message_type_outbox != 0) {
                cv.put("message_type_outbox", mCurrentMirrorItem.message_type_outbox);
            }
        }
        if (mColumnNames.contains("message_type_queued")) {
            if (mCurrentMirrorItem.message_type_queued != 0) {
                cv.put("message_type_queued", mCurrentMirrorItem.message_type_queued);
            }
        }
        if (mColumnNames.contains("message_type_sent")) {
            if (mCurrentMirrorItem.message_type_sent != 0) {
                cv.put("message_type_sent", mCurrentMirrorItem.message_type_sent);
            }
        }

        if (mCurrentMirrorItem.date.isEmpty()) {
            dbgTrace("date is empty: Invalid Message");
//            return true;

        }

        Uri result = mContentResolver.insert(mUri, cv);
        if (result != null) {
            dbgTrace("Inserted: " + result);
        } else {
            dbgTrace("Insert failed");
        }

        return true;
    }


    @Override
    long getNextMirrorItem(int row) {
        mCurrentMirrorItem = this.getMirrSmsforRow(row);
        dbgTrace("Mirr item checksum: " + mCurrentMirrorItem.checksum + "For row: " + row);
        return mCurrentMirrorItem.checksum;
    }

    @Override
    long getNextMirrorPlusItem(int row) {
        mCurrentPlusItem = this.getArchiveSmsforRow(row);
        dbgTrace("Archive item checksum: " + mCurrentPlusItem.checksum);
        return mCurrentPlusItem.checksum;
    }

    @Override
    int[] getDupArrayForCsum(int csum){
        return new int[0];
    }

    private SmsMsg getMirrSmsforRow(int row) {
        dbgTrace("getMirrSmsforRow");
        SmsMsg tempsms = mMsgMirrDb.getMessageForRow(row);
        return tempsms;
    }

    private SmsMsg getArchiveSmsforRow(int row) {
        dbgTrace("getArchiveSmsforRow");
        SmsMsg tempsms = mMsgArchiveDb.getMessageForRow(row);
        return tempsms;
    }

    @Override
    public void refreshPhoneDb() {
        dbgTrace();
        mContentResolver.delete(Uri.parse("content://sms/conversations/-1"), null, null);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("MessagesV2.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MessagesV2.log", trace);
    }

    @Override
    boolean addMirrToDataBase() {
        dbgTrace();
        return mMsgMirrDb.addToSqlDataBase(mCurrentMirrorItem);
    }

    @Override
    boolean addMirrPlusToDataBase() {
        dbgTrace();
        UiContext.getInstance().log(UiContext.DEBUG, "addMirrPlusToDataBase");
        return mMsgArchiveDb.addToSqlDataBase(mCurrentMirrorItem);
    }

    @Override
    boolean addPhoneItemToDatabaseAsMirr() {
        dbgTrace();
        return mMsgMirrDb.addToSqlDataBase(mCurrentPhoneItem);
    }

    @Override
    boolean deleteitemForCsum(int csum) {
        return mMsgMirrDb.deleteMessageForChecksum(csum);
    }

    @Override
    public long getMirrTotalItemsCount() {
        dbgTrace();
        return mMsgMirrDb.getRowCount();
    }


    @Override
    long getMirrPlusTotalItemsCount() {
        dbgTrace();
        return mMsgArchiveDb.getRowCount();
    }

    public static class SmsMsg {
        public String identi2 = "";
        public String address = "";
        public String thread_id = "";
        public String person = "";
        public String date = "";
        public String date_sent = "";
        public String protocol = "";
        public String read = "";
        public String status = "";
        public String reply_path_present = "";
        public String subject = "";
        public String body = "";
        public String service_center = "";
        public String locked = "";
        public String error_code = "";
        public String seen = "";
        public String type = "";

        public String sub_id = "";
        public String semc_message_priority = "";
        public String parent_id = "";
        public String delivery_status = "";
        public String star_status = "";
        public String delivery_date = "";

        public int message_type_all = 0;
        public int message_type_draft = 0;
        public int message_type_failed = 0;
        public int message_type_inbox = 0;
        public int message_type_outbox = 0;
        public int message_type_queued = 0;
        public int message_type_sent = 0;

        // this is crucial for logimCursor. see the documentation.
        public int checksum;

        public SmsMsg() {

        }

        public long crc32() {
            if (this.identi2 == null) {
                return 0;
            }
            Checksum checksum = new CRC32();

            checksum.update(this.address.getBytes(), 0, this.address.length());
            checksum.update(this.body.getBytes(), 0, this.body.length());
            checksum.update(this.date.getBytes(), 0, this.date.length());
            checksum.update(this.date_sent.getBytes(), 0, this.date_sent.length());
            checksum.update(this.service_center.getBytes(), 0, this.service_center.length());

            return checksum.getValue();
        }
    }


}
