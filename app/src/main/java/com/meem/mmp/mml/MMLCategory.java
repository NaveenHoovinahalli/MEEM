package com.meem.mmp.mml;

import com.meem.androidapp.R;
import com.meem.androidapp.UiContext;
import com.meem.mmp.messages.MMPConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is for dealing with category specifications of data used in DATD and SESD files. This is NOT for the use of generic and smart
 * data settings used in MSTAT, MCFG, VSTAT and VCFG.
 *
 * @author Arun T A
 * @note I am not using an enumeration with string-integer conversion here because we need to take care of multiple category masks too.
 */
public class MMLCategory {

    static final String[] mSmartCatStrings = {MMLKeywords.CAT_CONTACT, MMLKeywords.CAT_MESSAGE, MMLKeywords.CAT_CALENDAR, MMLKeywords.CAT_BOOKMARK, MMLKeywords.CAT_NOTE, MMLKeywords.CAT_CALL_LOG, MMLKeywords.CAT_MEMO, MMLKeywords.CAT_APP, MMLKeywords.CAT_SETTINGS, MMLKeywords.CAT_MAIL_ACCOUNT, MMLKeywords.CAT_APP_DATA};
    static final String[] mGenericCatStrings = {MMLKeywords.CAT_PHOTO, MMLKeywords.CAT_VIDEO, MMLKeywords.CAT_MUSIC, MMLKeywords.CAT_MISC, MMLKeywords.CAT_PHOTO_CAM, MMLKeywords.CAT_VIDEO_CAM, MMLKeywords.CAT_DOC_INT, MMLKeywords.CAT_DOC_EXT};
    // === For V2 ===
    // Important: Do not change the order of the array items.
    static final byte[] mGenCatCodes = {MMPConstants.MMP_CATCODE_PHOTO, MMPConstants.MMP_CATCODE_VIDEO, MMPConstants.MMP_CATCODE_MUSIC, MMPConstants.MMP_CATCODE_FILE, MMPConstants.MMP_CATCODE_PHOTO_CAM, MMPConstants.MMP_CATCODE_VIDEO_CAM, MMPConstants.MMP_CATCODE_DOCUMENTS, MMPConstants.MMP_CATCODE_DOCUMENTS_SD};

    // ---------------------------------------------------------------------------
    // SMART DATA related
    // ---------------------------------------------------------------------------
    static final byte[] mSmartCatCodes = {MMPConstants.MMP_CATCODE_CONTACT, MMPConstants.MMP_CATCODE_MESSAGE, MMPConstants.MMP_CATCODE_CALENDER};
    static UiContext mUiCtxt = UiContext.getInstance();

    public static String toSmartCatString(int mmpSmartCatCode) {
        int idx = mmpSmartCatCode - MMPConstants.MMP_CATCODE_SMARTDATA_BASE;
        if (idx < 0 || idx >= mSmartCatStrings.length) {
            throw new IllegalArgumentException("Invalid smart data category code");
        }

        return mSmartCatStrings[idx];
    }

    public static String toSmartCatPrettyString(int mmpSmartCatCode) {
        int idx = mmpSmartCatCode - MMPConstants.MMP_CATCODE_SMARTDATA_BASE;
        if (idx < 0 || idx >= mSmartCatStrings.length) {
            throw new IllegalArgumentException("Invalid smart data category code");
        }

        String[] smartCatPrettyStrings = {mUiCtxt.getAppContext().getResources().getString(R.string.contacts), mUiCtxt.getAppContext().getResources().getString(R.string.messages), mUiCtxt.getAppContext().getResources().getString(R.string.calendar), "Bookmarks", "Notes", "Call logs", "Memos", "Applications", "Settings", "Mail Accounts", "Application data"};

        return smartCatPrettyStrings[idx];
    }

    public static byte toSmartCatCode(String mmpSmartCatString) {
        for (int i = 0; i < mSmartCatStrings.length; i++) {
            if (mSmartCatStrings[i].equals(mmpSmartCatString)) {
                return (byte) (MMPConstants.MMP_CATCODE_SMARTDATA_BASE + i);
            }
        }
        throw new IllegalArgumentException("Invalid smart category string");
    }

    public static int getSmartCatMask(String[] cats) {
        int mask = 0;
        int setAt = 0;
        List<String> smartList = Arrays.asList(mSmartCatStrings);
        for (int i = 0; i < cats.length; i++) {
            setAt = smartList.indexOf(cats[i]);
            if (setAt != -1) {
                mask = mask | ((int) 1 << setAt);
            }
        }
        return mask;
    }

    public static boolean isSmartCategoryEnabled(byte catCode, int catMask) {
        int idx = catCode - MMPConstants.MMP_CATCODE_SMARTDATA_BASE;
        int pos = 1 << idx;
        if (0 != (catMask & pos)) {
            return true;
        }
        return false;
    }

    public static boolean isSmartCategoryString(String catStr) {
        List<String> smartList = Arrays.asList(mSmartCatStrings);
        return smartList.contains(catStr);
    }

    public static boolean isSmartCategoryCode(byte catCode) {
        if (catCode >= MMPConstants.MMP_CATCODE_SMARTDATA_BASE && catCode <= MMPConstants.MMP_CATCODE_SMARTDATA_MAX) {
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------
    // GENERIC DATA related
    // ---------------------------------------------------------------------------

    public static ArrayList<Byte> getSmartCatCodesArray(int smartCatMask) {
        ArrayList<Byte> cats = new ArrayList<Byte>();
        int mask;
        for (int i = 0; i < 32; i++) {
            mask = 1 << i;
            if (0 != (mask & smartCatMask)) {
                byte cat = (byte) (MMPConstants.MMP_CATCODE_SMARTDATA_BASE + i);
                cats.add(Byte.valueOf(cat));
            }
        }

        return cats;
    }

    public static int updateSmartCatMask(int mask, byte catCode, boolean enabled) {
        int idx = catCode - MMPConstants.MMP_CATCODE_SMARTDATA_BASE;
        int pos = 1 << idx;
        if (enabled) {
            mask |= pos;
        } else {
            mask &= ~pos;
        }

        return mask;
    }

    public static String toGenericCatString(int mmpGenericCatCode) {
        int idx = mmpGenericCatCode - MMPConstants.MMP_CATCODE_GENERICDATA_BASE;
        if (idx < 0 || idx >= mGenericCatStrings.length) {
            throw new IllegalArgumentException("Invalid generic data category code");
        }

        return mGenericCatStrings[idx];
    }

    public static String toGenericCatPrettyString(int mmpGenericCatCode) {
        int idx = mmpGenericCatCode - MMPConstants.MMP_CATCODE_GENERICDATA_BASE;
        if (idx < 0 || idx >= mGenericCatStrings.length) {
            throw new IllegalArgumentException("Invalid generic data category code");
        }

        String[] genericCatPrettyStrings = {mUiCtxt.getAppContext().getResources().getString(R.string.photos), mUiCtxt.getAppContext().getResources().getString(R.string.videos), mUiCtxt.getAppContext().getResources().getString(R.string.music), mUiCtxt.getAppContext().getResources().getString(R.string.music) + "(SD)", mUiCtxt.getAppContext().getResources().getString(R.string.photos) + "(SD)", mUiCtxt.getAppContext().getResources().getString(R.string.videos) + "(SD)", mUiCtxt.getAppContext().getResources().getString(R.string.documents), mUiCtxt.getAppContext().getResources().getString(R.string.documents) + "(SD)"};

        return genericCatPrettyStrings[idx];
    }

    public static byte toGenericCatCode(String mmpGenericCatString) {
        for (int i = 0; i < mGenericCatStrings.length; i++) {
            if (mGenericCatStrings[i].equals(mmpGenericCatString)) {
                return (byte) (MMPConstants.MMP_CATCODE_GENERICDATA_BASE + i);
            }
        }
        throw new IllegalArgumentException("Invalid generic category string");
    }

    public static int getGenericCatMask(String[] cats) {
        int mask = 0;
        int setAt = 0;
        List<String> genList = Arrays.asList(mGenericCatStrings);
        for (int i = 0; i < cats.length; i++) {
            setAt = genList.indexOf(cats[i]);
            if (setAt != -1) {
                mask = mask | ((int) 1 << setAt);
            }
        }
        return mask;
    }

    public static boolean isGenericCategoryEnabled(byte catCode, int catMask) {
        int idx = catCode - MMPConstants.MMP_CATCODE_GENERICDATA_BASE;
        int pos = 1 << idx;
        if (0 != (catMask & pos)) {
            return true;
        }
        return false;
    }

    public static boolean isGenericCategoryString(String catStr) {
        List<String> genericList = Arrays.asList(mGenericCatStrings);
        return genericList.contains(catStr);
    }

    public static boolean isGenericCategoryCode(byte catCode) {
        if (catCode >= MMPConstants.MMP_CATCODE_GENERICDATA_BASE && catCode <= MMPConstants.MMP_CATCODE_GENERICDATA_MAX) {
            return true;
        }
        return false;
    }

    public static ArrayList<Byte> getGenericCatCodesArray(int genCatMask) {
        ArrayList<Byte> cats = new ArrayList<Byte>();
        int mask;
        for (int i = 0; i < 32; i++) {
            mask = 1 << i;
            if (0 != (mask & genCatMask)) {
                byte cat = (byte) (MMPConstants.MMP_CATCODE_GENERICDATA_BASE + i);
                cats.add(Byte.valueOf(cat));
            }
        }

        return cats;
    }

    public static int updateGenCatMask(int mask, byte catCode, boolean enabled) {
        int idx = catCode - MMPConstants.MMP_CATCODE_GENERICDATA_BASE;
        int pos = 1 << idx;
        if (enabled) {
            mask |= pos;
        } else {
            mask &= ~pos;
        }

        return mask;
    }

    public static final byte[] getAllGenCatCodes() {
        return mGenCatCodes;
    }

    public static final String[] getAllGenCatStrings() {
        return mGenericCatStrings;
    }

    public static final byte[] getAllSmartCatCodes() {
        return mSmartCatCodes;
    }

    public static final String[] getAllSmartCatStrings() {
        return mSmartCatStrings;
    }
}
