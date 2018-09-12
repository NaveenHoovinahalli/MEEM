package com.meem.businesslogic;

import android.util.Log;
import android.util.SparseArray;

import com.meem.androidapp.AppLocalData;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.mmp.mml.MMLKeywords;
import com.meem.mmp.mml.MMLSmartDataDesc;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * DATD is the latest information about items in MEEM cable. So, naturally, its a part of this package.
 * <p/>
 * It essentially parses DATD a file and creates smart data and generic data information for further use by other application modules. It
 * uses XMLPullParser instead of DOM parser (which is used for smaller MML files like MSTAT, VSTAT etc) because DATD file can be really
 * large.
 * <p/>
 * TODO: Must remove DOM parser from the application package once this is one perfectly.
 *
 * @author Arun T A
 */
public class DATD {
    private final static String tag = "DATD";

    private static String mDatdFilePath = AppLocalData.getInstance().getDatdPath();
    DatdProcessingListener mListener;

    SparseArray<MMLSmartDataDesc> mSmartDataList;

    public DATD(DatdProcessingListener listener) {
        mListener = listener;
        mSmartDataList = new SparseArray<MMLSmartDataDesc>();
    }

    public boolean process() {
        boolean result = true;

        MMLSmartDataDesc smartDesc = null;
        MMLGenericDataDesc genericDesc = null;

        FileInputStream fis = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();

            fis = new FileInputStream(new File(mDatdFilePath));
            xpp.setInput(fis, null);

            // kick start
            xpp.nextTag();
            xpp.require(XmlPullParser.START_TAG, null, MMLKeywords.DATD_TAG_START);
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = null;

                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        Log.d(tag, "Start document");
                        break;

                    case XmlPullParser.START_TAG:
                        name = xpp.getName();
                        if (MMLCategory.isSmartCategoryString(name)) {
                            smartDesc = new MMLSmartDataDesc();
                            smartDesc.mCatCode = MMLCategory.toSmartCatCode(name);
                        } else if ((name.equals(MMLKeywords.TYPE_MIRROR))) {
                            smartDesc.mPaths[0] = ""; // no path in DATD for
                            // smart data
                            smartDesc.mSizes[0] = Long.parseLong(xpp.getAttributeValue(0));
                            smartDesc.mCSums[0] = xpp.getAttributeValue(1);
                        } else if ((name.equals(MMLKeywords.TYPE_DELETED))) {
                            smartDesc.mPaths[1] = ""; // no path in DATD for
                            // smart plus data
                            smartDesc.mSizes[1] = Long.parseLong(xpp.getAttributeValue(0));
                            smartDesc.mCSums[1] = xpp.getAttributeValue(1);
                        } else if (MMLCategory.isGenericCategoryString(name)) {
                            genericDesc = new MMLGenericDataDesc();
                            genericDesc.mCatCode = MMLCategory.toGenericCatCode(name);
                        } else if (name.equals(MMLKeywords.DATD_TAG_FILE)) {
                            genericDesc.mPath = xpp.getAttributeValue(0);
                            genericDesc.mSize = Long.parseLong(xpp.getAttributeValue(1));
                            genericDesc.mModTime = Long.parseLong(xpp.getAttributeValue(2));
                            genericDesc.mCSum = xpp.getAttributeValue(3);
                            genericDesc.mStatus = 0;
                            // genericDesc.mStatus will be processed later in
                            // backup and restore logic
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        name = xpp.getName();
                        if (MMLCategory.isSmartCategoryString(name)) {
                            Log.d(tag, smartDesc.toString());
                            mSmartDataList.append(smartDesc.mCatCode, smartDesc);
                        } else if (name.equals(MMLKeywords.DATD_TAG_FILE)) {
                            Log.d(tag, genericDesc.toString());
                            if (false == mListener.onNewGenericDataDesc(genericDesc)) {
                                Log.w("DATD", "onNewGenericDataDesc returns error. Aborting further processing");

                                result = false;

                                mListener.onDatdProcessingCompletion(result);

                                fis.close();
                                return result;
                            }
                        }
                        break;
                } // switch on event type

                eventType = xpp.next();
            } // while document is not fully processed

        } catch (Exception ex) {
            Log.e("DATD", "DATD parsing failed: " + ex.getMessage());
            result = false;
        }

        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ex) {
                Log.e("DATD", "DATD file stream close failed: " + ex.getMessage());
            }
        }

        mListener.onDatdProcessingCompletion(result);
        return result;
    }
}
