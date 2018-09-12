package com.meem.phone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.meem.utils.GenUtils;

import java.util.ArrayList;

/**
 * Created by hercules on 26/9/16.
 */
public class MessagesDb extends MeemSqlDBManager {

    private String tag = "MessagesDb";
    private Messages.SmsMsg mCurrentItem;
    private String mUpid;

    public MessagesDb(Context mContext, String uipd, String dbFilepath, ArrayList<String> tbNamesArrList) {
        super(mContext, dbFilepath, tbNamesArrList);
        mUpid = uipd;
        dbgTrace();
    }

    // individual restore related contructor

    public MessagesDb(Context context, String dbPath) {
        super(context, dbPath);
        dbgTrace();
    }


    // To delete All Entries
    public MessagesDb(Context context, String dbPath, String upid) {
        super(context, dbPath);
        mUpid = upid;
        dbgTrace();
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("MessagesDb.log");
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MessagesDb.log", trace);
    }

    public void addToSqlDataBase(Messages.SmsMsg currItem, int isMirr) {
        dbgTrace();
        mCurrentItem = currItem;

        ContentValues values = new ContentValues();
        values.put("upid", mUpid);
        values.put("checksum", (int) mCurrentItem.checksum);
        values.put("ismirr", (int) isMirr);

        if (!add(values, MessagesTableName.VAULT_MESSAGE_TABLE)) {
            dbgTrace("Inserting to vault_messsage_table Failed");
            return;
        } else {
            int linkcount;
            linkcount = getLinkCountforCsum(mCurrentItem.checksum);
            dbgTrace("link count " + linkcount);
            if (linkcount == 0) {
                if (addnewLink(mCurrentItem.checksum)) {


                    if (!add_sms_msg()) dbgTrace(" Adding add_sms_msg failed  ");


                } else {
                    dbgTrace("adding new link to Links table failed ");
                    return;
                }
            } else if (linkcount > 0) {
                        /* if links count is less more than  */
                if (!addLinkscount(mCurrentItem.checksum)) dbgTrace(" Adding addLinkscount failed  ");
            }


        }
    }


    private int getLinkCountforCsum(int checkSum) {
        dbgTrace();

        String querry = "select * from " + MessagesTableName.LINKS_COUNT_TABLE + " where checksum = " + checkSum;
        return rawQuerryGetInt(querry, 1);
    }


    private boolean decrementLinkscountForChecksum(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + MessagesTableName.LINKS_COUNT_TABLE +
                " set linkscount = linkscount - 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean addLinkscount(int checkSum) {
        dbgTrace();
        String sqlStmt = "update " + MessagesTableName.LINKS_COUNT_TABLE +
                " set linkscount = linkscount + 1" +
                " where checksum = " + checkSum;
        return executeSqlStmt(sqlStmt);
    }

    private boolean addnewLink(int checkSum) {
        dbgTrace();
        ContentValues values = new ContentValues();
        values.put("checksum", (int) checkSum);
        values.put("linkscount", (int) 1);

        return add(values, MessagesTableName.LINKS_COUNT_TABLE);
    }

    public boolean deleteAll() {
        boolean result = true;


        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + MessagesTableName.LINKS_COUNT_TABLE);
            db.execSQL("delete from " + MessagesTableName.VAULT_MESSAGE_TABLE);
            db.execSQL("delete from " + MessagesTableName.SMS_MSG_TABLE);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }
        return result;

    }

    private boolean add_sms_msg() {
        dbgTrace();

        ContentValues values = new ContentValues();
        values.put("checksum", (int) mCurrentItem.checksum);

        if (0 != mCurrentItem.identi2.length()) {
            values.put("identi2", mCurrentItem.identi2);
        }
        if (0 != mCurrentItem.address.length()) {
            values.put("address", mCurrentItem.address);
        }
        if (0 != mCurrentItem.thread_id.length()) {
            values.put("thread_id", mCurrentItem.thread_id);
        }
        if (0 != mCurrentItem.person.length()) {
            values.put("person", mCurrentItem.person);
        }
        if (0 != mCurrentItem.date.length()) {
            values.put("date", mCurrentItem.date);
        }
        if (0 != mCurrentItem.date_sent.length()) {
            values.put("date_sent", mCurrentItem.date_sent);
        }
        if (0 != mCurrentItem.protocol.length()) {
            values.put("protocol", mCurrentItem.protocol);
        }
        if (0 != mCurrentItem.read.length()) {
            values.put("read", mCurrentItem.read);
        }
        if (0 != mCurrentItem.status.length()) {
            values.put("status", mCurrentItem.status);
        }
        if (0 != mCurrentItem.reply_path_present.length()) {
            values.put("reply_path_present", mCurrentItem.reply_path_present);
        }
        if (0 != mCurrentItem.subject.length()) {
            values.put("subject", mCurrentItem.subject);
        }
        if (0 != mCurrentItem.body.length()) {
            values.put("body", mCurrentItem.body);
        }
        if (0 != mCurrentItem.service_center.length()) {
            values.put("service_center", mCurrentItem.service_center);
        }
        if (0 != mCurrentItem.locked.length()) {
            values.put("locked", mCurrentItem.locked);
        }
        if (0 != mCurrentItem.error_code.length()) {
            values.put("error_code", mCurrentItem.error_code);
        }
        if (0 != mCurrentItem.seen.length()) {
            values.put("seen", mCurrentItem.seen);
        }
        if (0 != mCurrentItem.type.length()) {
            values.put("type", mCurrentItem.type);
        }
        if (0 != mCurrentItem.sub_id.length()) {
            values.put("sub_id", mCurrentItem.sub_id);
        }
        if (0 != mCurrentItem.semc_message_priority.length()) {
            values.put("semc_message_priority", mCurrentItem.semc_message_priority);
        }
        if (0 != mCurrentItem.parent_id.length()) {
            values.put("parent_id", mCurrentItem.parent_id);
        }
        if (0 != mCurrentItem.delivery_status.length()) {
            values.put("delivery_status", mCurrentItem.delivery_status);
        }
        if (0 != mCurrentItem.star_status.length()) {
            values.put("star_status", mCurrentItem.star_status);
        }
        if (0 != mCurrentItem.delivery_date.length()) {
            values.put("delivery_date", mCurrentItem.delivery_date);
        }


        values.put("message_type_all", (int) mCurrentItem.message_type_all);
        values.put("message_type_draft", (int) mCurrentItem.message_type_draft);
        values.put("message_type_failed", (int) mCurrentItem.message_type_failed);
        values.put("message_type_inbox", (int) mCurrentItem.message_type_inbox);
        values.put("message_type_outbox", (int) mCurrentItem.message_type_outbox);
        values.put("message_type_queued", (int) mCurrentItem.message_type_queued);
        values.put("message_type_sent", (int) mCurrentItem.message_type_sent);


        return add(values, MessagesTableName.SMS_MSG_TABLE);

    }

    /**
     * Get DB Methods
     */
    public Messages.SmsMsg getMessageForChecksum(int cSum) {
        Messages.SmsMsg currDbItem = new Messages.SmsMsg();
        currDbItem.checksum = cSum;

        if (!getSmsMsgTableForChecksum(currDbItem)) {
            dbgTrace("getSmsMsgTableForChecksum Failed for checksum" + cSum);
        }


        return currDbItem;

    }


    private boolean getSmsMsgTableForChecksum(Messages.SmsMsg currItem) {
        boolean result = true;
        String sqlStmt = "select * from " + MessagesTableName.SMS_MSG_TABLE + " where checksum = " + currItem.checksum;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);
            if (null == cursor) {
                return false;
            }

            cursor.moveToNext();

            currItem.identi2 = cursor.getString(1);
            if (currItem.identi2 == null) {
                currItem.identi2 = "";
            }
            currItem.address = cursor.getString(2);
            if (currItem.address == null) {
                currItem.address = "";
            }
            currItem.thread_id = cursor.getString(3);
            if (currItem.thread_id == null) {
                currItem.thread_id = "";
            }
            currItem.person = cursor.getString(4);
            if (currItem.person == null) {
                currItem.person = "";
            }
            currItem.date = cursor.getString(5);
            if (currItem.date == null) {
                currItem.date = "";
            }
            currItem.date_sent = cursor.getString(6);
            if (currItem.date_sent == null) {
                currItem.date_sent = "";
            }
            currItem.protocol = cursor.getString(7);
            if (currItem.protocol == null) {
                currItem.protocol = "";
            }
            currItem.read = cursor.getString(8);
            if (currItem.read == null) {
                currItem.read = "";
            }
            currItem.status = cursor.getString(9);
            if (currItem.status == null) {
                currItem.status = "";
            }
            currItem.reply_path_present = cursor.getString(10);
            if (currItem.reply_path_present == null) {
                currItem.reply_path_present = "";
            }
            currItem.subject = cursor.getString(11);
            if (currItem.subject == null) {
                currItem.subject = "";
            }
            currItem.body = cursor.getString(12);
            if (currItem.body == null) {
                currItem.body = "";
            }
            currItem.service_center = cursor.getString(13);
            if (currItem.service_center == null) {
                currItem.service_center = "";
            }
            currItem.locked = cursor.getString(14);
            if (currItem.locked == null) {
                currItem.locked = "";
            }
            currItem.error_code = cursor.getString(15);
            if (currItem.error_code == null) {
                currItem.error_code = "";
            }
            currItem.seen = cursor.getString(16);
            if (currItem.seen == null) {
                currItem.seen = "";
            }
            currItem.type = cursor.getString(17);
            if (currItem.type == null) {
                currItem.type = "";
            }

            currItem.sub_id = cursor.getString(18);
            if (currItem.sub_id == null) {
                currItem.sub_id = "";
            }
            currItem.semc_message_priority = cursor.getString(19);
            if (currItem.semc_message_priority == null) {
                currItem.semc_message_priority = "";
            }
            currItem.parent_id = cursor.getString(20);
            if (currItem.parent_id == null) {
                currItem.parent_id = "";
            }
            currItem.delivery_status = cursor.getString(21);
            if (currItem.delivery_status == null) {
                currItem.delivery_status = "";
            }
            currItem.star_status = cursor.getString(22);
            if (currItem.star_status == null) {
                currItem.star_status = "";
            }
            currItem.delivery_date = cursor.getString(23);
            if (currItem.delivery_date == null) {
                currItem.delivery_date = "";
            }

            currItem.message_type_all = cursor.getInt(24);
            currItem.message_type_draft = cursor.getInt(25);
            currItem.message_type_failed = cursor.getInt(26);
            currItem.message_type_inbox = cursor.getInt(27);
            currItem.message_type_outbox = cursor.getInt(28);
            currItem.message_type_queued = cursor.getInt(29);
            currItem.message_type_sent = cursor.getInt(30);


            if (null != cursor) {
                cursor.close();
            }

            db.close();
        } catch (Exception ex) {
            result = false;
            dbgTrace("Exception: " + ex.getMessage());
        }

        return result;

    }


    public boolean deleteRowsForChecksum(int cSum) {

        if (getLinkCountforCsum(cSum) > 1) {
            // Decrement links count
            if (!decrementLinkscountForChecksum(cSum)) {


                dbgTrace(" getLinkCountforCsum Failed for csum " + cSum);
                return false;
            }
        } else {
            // Delete in All Tables
            if (!deleteRowInAllOtherTablesForChecksum(cSum)) {
                dbgTrace(" deleteRowInAllOtherTablesForChecksum Failed for csum " + cSum);
                return false;
            }
        }

        return true;
    }


    public boolean deleteRowInAllOtherTablesForChecksum(int cSum) {
        boolean result = true;

        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + MessagesTableName.LINKS_COUNT_TABLE + " where checksum = " + cSum);
            db.execSQL("delete from " + MessagesTableName.SMS_MSG_TABLE + " where checksum = " + cSum);


            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public boolean deleteRowsInVaultsTableForUpid(String upid, int isMirr) {
        boolean result = true;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            db.execSQL("delete from " + MessagesTableName.VAULT_MESSAGE_TABLE + " where upid = '" + upid + "' and ismirr = " + isMirr);
            db.close();
        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }


    public boolean deleteMessagesForUpid(String upid, int isMirr) {
        boolean result = true;
        String sqlStmt = "select * from " + MessagesTableName.VAULT_MESSAGE_TABLE + " where upid = '" + upid + "' and ismirr = " + isMirr;
        try {
            ArrayList<Integer> cSumArr = new ArrayList<Integer>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlStmt, null);

            if (null == cursor) {
                return false;
            }

            while (cursor.moveToNext()) {
                cSumArr.add(cursor.getInt(1));
            }
            if (null != cursor) {
                cursor.close();
            }
            db.close();

            for (int cSum : cSumArr) {
                deleteRowsForChecksum(cSum);
            }
            deleteRowsInVaultsTableForUpid(upid, isMirr);

        } catch (SQLException ex) {
            dbgTrace("SQLException: " + ex.getMessage());
            result = false;
        }


        return result;
    }

    public boolean delteAllEntriesOfUpidInDb() {
        boolean res = true;

        if (!deleteMessagesForUpid(mUpid, 0)) {
            dbgTrace("Failed! Delete Messages of Upid " + mUpid + "Mirror Items");
            res = false;
        } else dbgTrace("Deleted All Messages of Upid " + mUpid + "Mirror Items");
        if (!deleteMessagesForUpid(mUpid, 1)) {
            dbgTrace("Failed! Delete Messages of Upid " + mUpid + "MirrorPlus Items");
            res = false;
        } else dbgTrace("Deleted All Messages of Upid " + mUpid + "MirrorPlus Items");

        return res;
    }

}
