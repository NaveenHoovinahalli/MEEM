package com.meem.mmp.mml;

import android.util.Log;

import com.meem.mmp.messages.MMPConstants;
import com.meem.utils.GenUtils;

/**
 * This is for the use of configuration/status settings of MEEM vaults. So, you must use these wherever you are dealing with MSTAT, MCFG.
 * VSTAT and VCFG files. Also designed to be usable with GUI.
 *
 * @author Arun T A
 */

public class MMLSmartDataSettings {
    private static final String tag = "MMLSmartDataSettings";
    static String[] mDefCats = {MMLKeywords.CAT_CONTACT, MMLKeywords.CAT_MESSAGE, MMLKeywords.CAT_CALENDAR};
    int mCategoryMask = 0;

    public MMLSmartDataSettings(boolean isEnabled) {
        if (isEnabled) {
            // set defaults
            mCategoryMask = MMLCategory.getSmartCatMask(mDefCats);
        } else {
            mCategoryMask = 0;
        }
    }

    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CONTACT, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CONTACT + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MESSAGE, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_MESSAGE + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CALENDER, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CALENDAR + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_BOOKMARK, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_BOOKMARK + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_NOTE, mCategoryMask)) strBuf.append(MMLKeywords.CAT_NOTE + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CALL_LOG, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CALL_LOG + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MEMO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MEMO + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_APP, mCategoryMask)) strBuf.append(MMLKeywords.CAT_APP + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_SETTING, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_SETTINGS + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MAIL_ACCOUNT, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_MAIL_ACCOUNT + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_APP_DATA, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_APP_DATA + ",");

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
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CONTACT, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CONTACT + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MESSAGE, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_MESSAGE + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CALENDER, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CALENDAR + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_BOOKMARK, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_BOOKMARK + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_NOTE, mCategoryMask)) strBuf.append(MMLKeywords.CAT_NOTE + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_CALL_LOG, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_CALL_LOG + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MEMO, mCategoryMask)) strBuf.append(MMLKeywords.CAT_MEMO + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_APP, mCategoryMask)) strBuf.append(MMLKeywords.CAT_APP + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_SETTING, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_SETTINGS + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_MAIL_ACCOUNT, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_MAIL_ACCOUNT + ",");
        if (MMLCategory.isSmartCategoryEnabled(MMPConstants.MMP_CATCODE_APP_DATA, mCategoryMask))
            strBuf.append(MMLKeywords.CAT_APP_DATA + ",");

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
            Log.i("MMLSmartDataSettings", "settings line in MML is null (no smart cats enabled)");
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

        mCategoryMask = MMLCategory.getSmartCatMask(cats);

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
        GenUtils.logMessageToFile("MMLSmartDataSettings.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("MMLSmartDataSettings.log");
    }
}
