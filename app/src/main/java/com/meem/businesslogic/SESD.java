package com.meem.businesslogic;

import android.util.Log;
import android.util.Xml;

import com.meem.androidapp.AppLocalData;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataDesc;
import com.meem.utils.GenUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SESD is the information about items in phone to be uploaded to MEEM cable. So, naturally, its a part of this package.
 * <p/>
 * TODO: Update MMLKeywords with the XMLS tags used in SESD
 *
 * @author Arun T A
 */
public class SESD {
    private final static String tag = "SESD";
    private static String mSesdFilePath = AppLocalData.getInstance().getSesdPath();

    XmlSerializer mSerializer;
    FileWriter mWriter;

    public SESD() {
        try {
            mSerializer = Xml.newSerializer();
            mWriter = new FileWriter(mSesdFilePath);
            mSerializer.setOutput(mWriter);
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            // TODO: send event to UI.
        }
    }

    public boolean begin() {
        boolean result = true;
        try {
            mSerializer.startTag(null, "sesd");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean addSmartDataSection(SessionSmartDataInfo smartInfo) {
        boolean result = true;
        try {
            mSerializer.startTag(null, "smart");

            ArrayList<Byte> catsList = smartInfo.getCatCodes();
            HashMap<Byte, ArrayList<MMPFPath>> catsFilesMap = smartInfo.getCatsFilesMap();
            for (Byte cat : catsList) {
                mSerializer.startTag(null, MMLCategory.toSmartCatString(cat));
                ArrayList<MMPFPath> files = catsFilesMap.get(cat);

                MMPFPath file = files.get(0);
                if (null != file) {
                    String mirrorPath = file.toString();
                    if (null != mirrorPath) {
                        File mirrorFile = new File(mirrorPath);
                        mSerializer.startTag(null, "mirror");
                        mSerializer.attribute(null, "path", mirrorPath);
                        mSerializer.attribute(null, "size", String.valueOf(mirrorFile.length()));
                        mSerializer.attribute(null, "csum", GenUtils.getFileMD5(mirrorPath, null));
                        mSerializer.endTag(null, "mirror");
                    }
                }

                file = files.get(1);
                if (null != file) {
                    String plusPath = file.toString();
                    if (plusPath != null) {
                        File plusFile = new File(plusPath);
                        mSerializer.startTag(null, "deleted");
                        mSerializer.attribute(null, "path", plusPath);
                        mSerializer.attribute(null, "size", String.valueOf(plusFile.length()));
                        mSerializer.attribute(null, "csum", GenUtils.getFileMD5(plusPath, null));
                        mSerializer.endTag(null, "deleted");
                    }
                }
                mSerializer.endTag(null, MMLCategory.toSmartCatString(cat));
            }

            mSerializer.endTag(null, "smart");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create smart data section of SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean beginGenericData() {
        boolean result = true;
        try {
            mSerializer.startTag(null, "generic");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean endGenericData() {
        boolean result = true;
        try {
            mSerializer.endTag(null, "generic");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean beginGenCategory(int catCode) {
        boolean result = true;
        try {
            mSerializer.startTag(null, MMLCategory.toGenericCatString(catCode));
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean beginMirror() {
        boolean result = true;
        try {
            mSerializer.startTag(null, "mirror");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean endMirror() {
        boolean result = true;
        try {
            mSerializer.endTag(null, "mirror");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean beginDeleted() {
        boolean result = true;
        try {
            mSerializer.startTag(null, "deleted");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean endDeleted() {
        boolean result = true;
        try {
            mSerializer.endTag(null, "deleted");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean endGenCategory(int catCode) {
        boolean result = true;
        try {
            mSerializer.endTag(null, MMLCategory.toGenericCatString(catCode));
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean addGenericDataFile(MMLGenericDataDesc genDesc) {
        boolean result = true;
        try {
            mSerializer.startTag(null, "file");
            mSerializer.attribute(null, "name", genDesc.mPath);
            mSerializer.attribute(null, "size", String.valueOf(genDesc.mSize));
            mSerializer.attribute(null, "mtime", String.valueOf(genDesc.mModTime));
            mSerializer.attribute(null, "csum", genDesc.mCSum);
            mSerializer.endTag(null, "file");
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }

    public boolean end() {
        boolean result = true;
        try {
            mSerializer.endTag(null, "sesd");
            mSerializer.endDocument();
        } catch (Exception ex) {
            Log.wtf(tag, "Could not create SESD", ex);
            result = false;
        }
        return result;
    }
}
