package com.meem.phone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.meem.utils.GenUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class implements the core logic of smart data processing in meem suite.
 * <p>
 * It is the base class of all smart data categories. The derived classes must implement the smart data processing according to the abstract
 * interface specified by this class. It is most important to note that the smart data is processed one item at a time. So whenever the
 * abstract interfaces are invoked, the derived class should be processing that one item at hand at that time.
 *
 * @author Arun T A
 */

public abstract class SmartDataCategory {
    private static final String tag = "SmartDataCategory";
    protected Context mContext;
    protected Uri mUri;
    protected ContentResolver mContentResolver;
    protected ArrayList<String> mColumnNames;
    protected Cursor mCursor;
    protected int mDbItemCount;
    // checksum array for the existing items in the phone DB
    protected int[] maPhoneDbItemChecksum;
    protected FileInputStream inMirrFileStream;

    ;
    protected FileInputStream inPlusFileStream;
    protected FileOutputStream outMirrFileStream;
    protected FileOutputStream outPlusFileStream;
    protected ObjectMapper mReaderMapper, mWriterMapper;
    protected ObjectReader mObjectReader;
    protected ObjectWriter mObjectWriter;
    protected JsonParser mMirrorParser, mPlusParser;
    // FindBugs fixes (static items)
    protected boolean mAbortFlag = false;
    long checksum = 0;
    // checksum array for the existing items in the mirror plus content (deleted
    // items, but still retained in meem.
    ArrayList<Integer> maPlusItemChecksum = new ArrayList<Integer>();
    private Mode mOpMode = Mode.BACKUP;
    private boolean mFoundDeletedItems = false;
    private boolean inMirrFileExists = true;
    private boolean inMirrPlusFileExists = true;

    public SmartDataCategory(Context context, String inMirr, String inPlus) {

        mContext = context;

        try {

            inMirrFileStream = new FileInputStream(inMirr);

        } catch (FileNotFoundException e) {
            dbgTrace("FileNotFoundException: " + e.getMessage());
            inMirrFileExists = false;
            //throw new IllegalArgumentException("Some of the smart data files are not existing");
        } catch (NullPointerException e) {
            dbgTrace("FileNotFoundException: " + e.getMessage());
            inMirrFileExists = false;
        }


        try {
            inPlusFileStream = new FileInputStream(inPlus);

        } catch (FileNotFoundException e) {
            dbgTrace("FileNotFoundException: " + e.getMessage());
            inMirrPlusFileExists = false;
            //throw new IllegalArgumentException("Some of the smart data files are not existing");
        } catch (NullPointerException e) {
            dbgTrace("FileNotFoundException: " + e.getMessage());
            inMirrPlusFileExists = false;
        }


        mReaderMapper = new ObjectMapper();
        mReaderMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mReaderMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        try {
            if (inMirrFileExists) mMirrorParser = mReaderMapper.getFactory().createParser(inMirrFileStream);
            if (inMirrPlusFileExists) mPlusParser = mReaderMapper.getFactory().createParser(inPlusFileStream);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inMirrFileExists || inMirrPlusFileExists) {

            mObjectReader = mReaderMapper.reader();

            if (mObjectReader == null) {
                dbgTrace("ObjectReader creation failed");
                throw new RuntimeException("ObjectReader/Writer creation failed");
            }

        }


    }

    public SmartDataCategory(Context context, Uri uri, String filter, String inMirr, String inPlus, String outMirr, String outPlus) {
        dbgTrace();
        mContext = context;
        mUri = uri;

        try {
            // IMPORTANT:
            // Both plus files need to be the same for the logic to work. it's
            // not opened for reading and writing simultaneously. so, it's
            // technically possible to open them just like this, mutually
            // exclusively. We will verify it here:
            if (!inPlus.equals(outPlus)) {
                throw new IllegalArgumentException("Input and output PLUS files must be the same");
            }
            inMirrFileStream = new FileInputStream(inMirr);
            inPlusFileStream = new FileInputStream(inPlus);
            outMirrFileStream = new FileOutputStream(outMirr);

            // We will open plus file in append mode
            outPlusFileStream = new FileOutputStream(outPlus, true);
            mAbortFlag = false;

        } catch (FileNotFoundException e) {
            dbgTrace("FileNotFoundException: " + e.getMessage());
            throw new IllegalArgumentException("Some of the smart data files are not existing");
        }
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
        mWriterMapper = new ObjectMapper();
        mWriterMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mWriterMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mReaderMapper = new ObjectMapper();
        mReaderMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mReaderMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        try {
            mMirrorParser = mReaderMapper.getFactory().createParser(inMirrFileStream);
            mPlusParser = mReaderMapper.getFactory().createParser(inPlusFileStream);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mObjectReader = mReaderMapper.reader();
        mObjectWriter = mWriterMapper.writer();

        if (mObjectReader == null || mObjectWriter == null) {
            dbgTrace("ObjectReader/Writer creation failed");
            throw new RuntimeException("ObjectReader/Writer creation failed");
        }
    }

    // For selective  contacts and calendar  restore
    public SmartDataCategory() {
        dbgTrace();
    }

    // For selective  Sms  restore
    public SmartDataCategory(Context context, Uri uri) {
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
        GenUtils.logMessageToFile("SmartDataCategory.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("SmartDataCategory.log");
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
            maPhoneDbItemChecksum[rowIndex] = (int) checksum;
            if (!addPhoneItemToMirror()) {
                return false;
            }
        }

        /**
         * No prepare a checksum array of input mirror plus file items. The
         * derived classes must implement this function and when they
         * de-serialize each item, they must call onDeserializedPlusItem()
         * passing the checksum.
         */
        while (-1 != (checksum = getNextMirrorPlusItem())) {
            maPlusItemChecksum.add(Integer.valueOf((int) checksum));
        }

        dbgTrace("prepare: end");
        return true;
    }

    public boolean parseNAddToDb() {
        dbgTrace();
        boolean result = true;
        int cheksum = 0;
        if (inMirrFileExists) {
            while ((-1 != (checksum = getNextMirrorItem())) && result) {
                if (mAbortFlag) {
                    return false;
                }
                addMirrToDataBase();
            }
        }
        if (inMirrPlusFileExists) {
            while ((-1 != (checksum = getNextMirrorPlusItem())) && result) {
                if (mAbortFlag) {
                    return false;
                }
                addMirrPlusToDataBase();
            }
        }


        dbgTrace("Finshed Processing DB");
        return result;
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


        while ((-1 != (checksum = getNextMirrorItem())) && result) {
            if (mAbortFlag) {
                dbgTrace("Aborting processing on request");
                return false;
            }

            try {
            } catch (Exception e) {
                dbgTrace(e.getMessage());
            }


            if (mOpMode == Mode.BACKUP) {
                if (isUsingTrackingMethod()) {
                    dbgTrace("Using tracker to add mirror plus items");
                    result = useTrackerToAddMirrorPlusItems();
                } else {
                    dbgTrace("Using legacy method to add mirror plus items");

                    int numMsgs = maPhoneDbItemChecksum.length, i = 0;
                    boolean deleted = true;
                    for (i = 0; i < numMsgs; i++) {
                        if (maPhoneDbItemChecksum[i] == (int) checksum) {
                            deleted = false;
                            break;
                        }
                    }

                    dbgTrace("deleted: " + deleted);
                    if (deleted) {
                        dbgTrace("A deleted item found");
                        boolean entryExists = false;
                        for (i = 0; i < maPlusItemChecksum.size(); i++) {
                            if (checksum == maPlusItemChecksum.get(i)) {
                                entryExists = true;
                                break;
                            }
                        }
                        if (!entryExists) {
                            result = addMirrorItemToPlus();
                        }
                    }
                }
            } else {
                // Restore operation
                // insert this item to the phone DB
                int numMsgs = maPhoneDbItemChecksum.length, i = 0;
                boolean deleted = true;

                for (i = 0; i < numMsgs; i++) {
                    if (maPhoneDbItemChecksum[i] == (int) checksum) {
                        deleted = false;
                        dbgTrace("Item already present");
                        break;
                    }
                }

                if (deleted) {
                    dbgTrace("Restoring item to phone");
                    mFoundDeletedItems = true;
                    result = addMirrorItemToPhone();
                }
            }
        }

        if (mOpMode == Mode.RESTORE && mFoundDeletedItems) {
            dbgTrace("Refreshing Threads");
            refreshPhoneDb();
        } else {
            dbgTrace("Not refreshing: " + mOpMode + " DeletedFlag: " + mFoundDeletedItems);
        }
        return result;
    }

    public boolean backup() {
        dbgTrace();

        boolean result;
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

    abstract boolean isUsingTrackingMethod();

    abstract boolean useTrackerToAddMirrorPlusItems();

    abstract long getNextPhoneDbItem();

    abstract long getNextPhoneDbNewItem(int id);

    abstract long getNextMirrorItem();

    abstract String getNextMirrorId();

    abstract long getNextMirrorPlusItem();

    abstract boolean addMirrorItemToPhone();

    abstract boolean addPhoneItemToMirror();

    abstract boolean addNewPhoneItemToMirror();

    abstract boolean addMirrorItemToPlus();

    abstract void refreshPhoneDb();

    abstract void addMirrToDataBase();

    abstract void addMirrPlusToDataBase();

    protected enum Mode {
        BACKUP, RESTORE
    }

}
