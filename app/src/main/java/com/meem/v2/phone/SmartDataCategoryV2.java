package com.meem.v2.phone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.meem.androidapp.SessionCommentary;
import com.meem.events.EventCode;
import com.meem.utils.GenUtils;

import java.util.ArrayList;

import meem.org.apache.commons.lang3.ArrayUtils;

import static com.meem.v2.mmp.MMPV2Constants.MMP_CATCODE_CONTACT;


/**
 * This class implements the core logic of smart data processing in meem suite.
 * <p>
 * It is the base class of all smart data categories. The derived classes must implement the smart data processing according to the abstract
 * interface specified by this class. It is most important to note that the smart data is processed one item at a time. So whenever the
 * abstract interfaces are invoked, the derived class should be processing that one item at hand at that time.
 *
 * @author Karthik B S
 */

public abstract class SmartDataCategoryV2 {
    private static final String tag = "SmartDataCategoryV2";
    protected Context mContext;
    protected Uri mUri;
    protected ContentResolver mContentResolver;
    protected ArrayList<String> mColumnNames;
    protected Cursor mCursor;
    protected int mDbItemCount;
    // checksum array for the existing items in the phone DB
    protected int[] maPhoneDbItemChecksum;
    protected boolean mAbortFlag = false;
    protected boolean isArchiveMode = false;
    long checksum = 0;
    private Mode mOpMode = Mode.BACKUP;
    private boolean mFoundDeletedItems = false;
    private boolean inMirrFileExists = true;
    private boolean inMirrPlusFileExists = true;

    private byte mCatCode;


    public SmartDataCategoryV2(byte catCode, Context context, Uri uri, String filter) {
        dbgTrace();

        mContext = context;
        mUri = uri;
        mAbortFlag = false;

        mContentResolver = mContext.getContentResolver();
        mCursor = mContentResolver.query(mUri, null, filter, null, null);
        mColumnNames = new ArrayList<String>();
        // TODO: Revisit this check
        if (mCursor != null) {
            mCursor.moveToFirst();
            for (String colName : mCursor.getColumnNames()) {
                mColumnNames.add(colName);
            }
            mDbItemCount = mCursor.getCount();
        } else {
            mDbItemCount = 0;
        }

        mCatCode = catCode;
    }

    // For selective  contacts and calendar  restore
    public SmartDataCategoryV2() {
        dbgTrace();
    }

    // For selective  Sms  restore
    public SmartDataCategoryV2(Context context, Uri uri) {
        dbgTrace();
        mContext = context;
        mUri = uri;
        mContentResolver = mContext.getContentResolver();
        mCursor = mContentResolver.query(mUri, null, null, null, null);
        mColumnNames = new ArrayList<String>();
        // TODO: Revisit this check
        if (mCursor != null) {
            mCursor.moveToFirst();
            for (String colName : mCursor.getColumnNames()) {
                mColumnNames.add(colName);
            }
            mDbItemCount = mCursor.getCount();
        } else {
            mDbItemCount = 0;
        }
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("SmartDataCategoryV2.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("SmartDataCategoryV2.log");
    }

    /**
     * In this function, we will iterate through the phone db and: 1. Generate and keep hash code and id of each message found in the query
     * result. 2. Most importantly, we will create the mirror file by putting all items in phone to output mirror file. 3. Create an array
     * of the checksums for the entries in mirror plus file. This will be used in ensuring we do not insert duplicate entries into mirror
     * plus file.
     *
     * @return boolean
     */
    public boolean prepare() {
        dbgTrace("Prepare: Total number of items: " + mDbItemCount);

        maPhoneDbItemChecksum = new int[mDbItemCount];
        long checksum;
        for (int rowIndex = 0; rowIndex < mDbItemCount; rowIndex++) {
            if (mAbortFlag) {
                dbgTrace("Aborting preparation on request");
                return false;
            }

            checksum = getNextPhoneDbItem();
            if (-1 == checksum) {
                dbgTrace("Error getting phone db item (i.e. finished scanning all phone db items)");
                return false;
            }
            if (0 == checksum) {
                continue;
            }
            dbgTrace("phone item: " + checksum);
            maPhoneDbItemChecksum[rowIndex] = (int) checksum;
            if (!addPhoneItemToDatabaseAsMirr()) {
                return false;
            }

            SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, rowIndex, (int) mDbItemCount, mCatCode, SessionCommentary.OPMODE_PROCESSING_PHONE_ITEMS);
            commentary.post();
        }

        dbgTrace("prepare: end");
        return true;
    }

    /**
     * Now we will process the mirror file. Derived classes must call onDeserializedMirrorItem with checksum. In that callback, will check
     * whether the item is deleted in the phone. We will use the checksum array of phone items we prepared in prepare() function for this.
     * Based upon the result of the above step, we will update the mirror plus file verifying that there are no duplicates. The duplication
     * check will make use of the checksum array of mirror plus items we prepare in prepare().
     */

    public boolean process() {
        dbgTrace();
        boolean result = true;
        checksum = 0;

        long mirrcount = getMirrTotalItemsCount();
        dbgTrace("Mirr" +
                "ItemCount: " + mirrcount);

        ArrayList<Integer> deletedCsums = new ArrayList<Integer>();


//        try{


        for (int i = 0; i < mirrcount; i++) {
            checksum = getNextMirrorItem(i);
            dbgTrace("Meem item: " + checksum);

            if (mAbortFlag) {
                dbgTrace("Aborting processing on request");
                return false;
            }

            if ((checksum != -1) && result) {
                dbgTrace("Mirr Item from db ,csum: " + checksum);

                long numphoneitems = maPhoneDbItemChecksum.length;
                boolean deleted = true;
                for (int index = 0; index < numphoneitems; index++) {
                    int csumint = maPhoneDbItemChecksum[index];
                    if (csumint == checksum) {
                        dbgTrace("Item is already present in phone, not inserting again");
                        deleted = false;
                        break;
                    }
                }
                dbgTrace("deleted: " + deleted);
                if (deleted) {

                    if (mOpMode == Mode.BACKUP) {
                        dbgTrace("BACKUP");
                        if (isArchiveMode) {
                            dbgTrace("addMirrPlusToDataBase");
                            result = addMirrPlusToDataBase();
                            if (result) {
                                dbgTrace("added successfully to archieve db");
                            } else {
                                dbgTrace("failed adding to archieve db");
                            }
                        }

                        deletedCsums.add(Integer.valueOf((int) checksum));

                    } else {
                        dbgTrace("RESTORE/COPY");
                        dbgTrace("Inserting an object to phone");
                        mFoundDeletedItems = true;
                        result = addMirrorItemToPhone();
                    }
                }
            }

            SessionCommentary commentary = new SessionCommentary(EventCode.SESSION_PREP_COMMENTARY, i, (int) mirrcount, mCatCode, SessionCommentary.OPMODE_PROCESSING_MEEM_ITEMS);
            commentary.post();
        }

//        }
//        catch (Exception e) {
//            dbgTrace("=> Exception during process " + e.getStackTrace().toString());
//        }


//        try{
        dbgTrace("deleted array count: " + deletedCsums.size());
        if (deletedCsums.size() > 0) {
            for (int j = 0; j < deletedCsums.size(); j++) {
                dbgTrace("csum: " + deletedCsums.get(j));

//                int delcsum = deletedCsums.get(j);
//                if (deleteitemForCsum(delcsum)) {
//                    dbgTrace("Deleted successfully in sync db");
//                } else {
//                    dbgTrace("Couldn't Delete in sync db");
//                }

                /////
                // here before calling delete, first check is checksum present in dup_mapp table as maincsum, if present, get all the checksum present in for the main checksum, and check if all those checksum present in mSessionInfo.mPhoneDbItemChecksumArray
                if (mCatCode==MMP_CATCODE_CONTACT) { // For contact only
                    int[] array=getDupArrayForCsum(deletedCsums.get(j));
                    boolean tobeDeleted=true;
                    if (array.length>0) {
                        // check is all those csums are present in phone
                        dbgTrace("phonearray: "+maPhoneDbItemChecksum+" duparray:"+array);

                        for (int csum : array) {
                            dbgTrace("duparraycsum: "+csum);
                            if (ArrayUtils.contains(maPhoneDbItemChecksum, csum)) {
                                dbgTrace("item is present for csum:%@, marked for not to delete :"+csum);
                                tobeDeleted=false;
                            }
                        }
                        if (tobeDeleted) {
                            int delcsum = deletedCsums.get(j);
                            if (deleteitemForCsum(delcsum)) {
                                dbgTrace("Deleted successfully in sync db");
                            } else {
                                dbgTrace("Couldn't Delete in sync db");
                            }
                        }
                    }else{
                        int delcsum = deletedCsums.get(j);
                        if (deleteitemForCsum(delcsum)) {
                            dbgTrace("Deleted successfully in sync db,for contact, no duplicates");
                        } else {
                            dbgTrace("Couldn't Delete in sync db");
                        }
                    }
                }else{
                    int delcsum = deletedCsums.get(j);
                    if (deleteitemForCsum(delcsum)) {
                        dbgTrace("Deleted successfully in sync db for a calendar Event");
                    } else {
                        dbgTrace("Couldn't Delete in sync db");
                    } // For calendar or messages
                }

                //////////
            }
        }
//        }
//        catch (Exception e) {
//            dbgTrace("=> Exception during deleting " + e.getStackTrace().toString());
//        }

        if (mOpMode == Mode.RESTORE && mFoundDeletedItems) {
            dbgTrace("Refreshing Threads");
            refreshPhoneDb();
        } else {
            dbgTrace("Not refreshing: " + mOpMode + " DeletedFlag: " + mFoundDeletedItems);
        }
        return result;
    }

    public boolean backup(boolean inArchive) {
        dbgTrace();
        boolean result = true;

        isArchiveMode = inArchive;
        if (isArchiveMode) {
            dbgTrace("Starting Backup in archieve mode");
        } else {
            dbgTrace("Starting Backup in Sync mode");
        }
        mOpMode = Mode.BACKUP;

        result = process();


        if (mCursor != null) {
            mCursor.close();
        }

        return result;
    }

    public boolean restore() {
        dbgTrace();

        boolean result;
        mOpMode = Mode.RESTORE;
        result = process();

        if (mCursor != null) {
            mCursor.close();
        }

        return result;
    }

    public void abort() {
        dbgTrace();
        mAbortFlag = true;
    }

    abstract long getNextPhoneDbItem();

    abstract long getNextMirrorItem(int row);

    abstract long getNextMirrorPlusItem(int row);

    abstract boolean addMirrorItemToPhone();

    abstract void refreshPhoneDb();

    abstract boolean addMirrToDataBase();

    abstract boolean addMirrPlusToDataBase();

    abstract boolean addPhoneItemToDatabaseAsMirr();

    abstract long getMirrTotalItemsCount();

    abstract long getMirrPlusTotalItemsCount();

    abstract boolean deleteitemForCsum(int csum);

    abstract int[] getDupArrayForCsum(int csum);


    protected enum Mode {
        BACKUP, RESTORE
    }

}
