package com.meem.mmp.mml;

import com.meem.mmp.messages.MMPConstants;
import com.meem.utils.GenUtils;

/**
 * This is for the use of configuration/status settings of MEEM vaults. So, you must use these wherever you are dealing with MSTAT, MCFG.
 * VSTAT and VCFG files. Also designed to be usable with GUI.
 *
 * @author Arun T A
 */

public class MMLGenericDataSettings {
    private static final String tag = "MMLGenericDataSettings";
    static String[] mDefCats = {MMLKeywords.CAT_PHOTO, MMLKeywords.CAT_VIDEO, MMLKeywords.CAT_MUSIC, MMLKeywords.CAT_DOC_INT, MMLKeywords.CAT_PHOTO_CAM, MMLKeywords.CAT_VIDEO_CAM, MMLKeywords.CAT_MISC, MMLKeywords.CAT_DOC_EXT};
    int mCategoryMask = 0;

    public MMLGenericDataSettings(boolean isEnabled) {
        if (isEnabled) {
            mCategoryMask = MMLCategory.getGenericCatMask(mDefCats);
        } else {
            mCategoryMask = 0;
        }
    }

    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_PHOTO + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_VIDEO + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MUSIC + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_DOC_INT + ",");

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MISC + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_PHOTO_CAM + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_VIDEO_CAM + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS_SD, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_DOC_EXT + ",");

        String retString = "";

        int len = strBuf.length();
        if (len > 0) {
            strBuf.deleteCharAt(len - 1); // delete last comma.
            retString = "[" + strBuf.toString() + "]";
        }

        dbgTrace(retString);
        return retString;
    }

    public String toXMLStringAttribute() {
        StringBuffer strBuf = new StringBuffer();
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_PHOTO + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_VIDEO + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_MUSIC, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MUSIC + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_DOC_INT + ",");

        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_FILE, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MISC + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_PHOTO_CAM, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_PHOTO_CAM + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_VIDEO_CAM, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_VIDEO_CAM + ",");
        if (MMLCategory.isGenericCategoryEnabled(MMPConstants.MMP_CATCODE_DOCUMENTS_SD, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_DOC_EXT + ",");

        String retString = "";

        int len = strBuf.length();
        if (len > 0) {
            strBuf.deleteCharAt(len - 1); // delete last comma.
            retString = "\"" + strBuf.toString() + "\"";
        } else {
            retString = "\"\"";
        }

        dbgTrace(retString);
        return retString;
    }

    public boolean fromString(String line) {
        if (null == line) {
            dbgTrace("Settings line in MML is null (no generic cats enabled)");
            mCategoryMask = 0;
            return true;
        }

        // cleanup the line from [ and ]
        String regx = "[]";
        char[] ca = regx.toCharArray();
        for (char c : ca) {
            line = line.replace("" + c, "");
        }

        String[] cats = line.split("\\s*,\\s*");

        mCategoryMask = MMLCategory.getGenericCatMask(cats);

        return true;
    }

    public int getCategoryMask() {
        return mCategoryMask;
    }

    public void setCategoryMask(int mask) {
        dbgTrace();
        mCategoryMask = mask;
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MMLGenericDataSettings.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("MMLGenericDataSettings.log");
    }
}
