package com.meem.mmp.messages;

import android.util.Log;

import com.meem.mmp.mml.MMLCategory;

import java.util.ArrayList;

/**
 * Parameters for DELETE_CETEGORY command. Since at present (27June2014), this is not finalized, I am using this class to hold all
 * parameters needed.
 *
 * @author Arun T A
 * @date 27Jun2014
 */
public class MMPDeleteCategorySpec {
    MMPUpid mUpid;

    byte mCatCode;
    /**
     * 10Dec2015: Arun: Added to support deletion of SDCARD items (a very old nasty hack in MMP which maps SDCARD items which are not
     * displayed in GUI but need to be specified for data operations.)
     */
    ArrayList<Byte> mMappedCats;

    boolean mIsMirror;

    public MMPDeleteCategorySpec(MMPUpid upid, byte catCode, boolean isMirror) {
        mUpid = upid;
        mCatCode = catCode;
        mIsMirror = isMirror;

        /**
         * 10Dec2015: Arun: See comment above.
         */
        mMappedCats = new ArrayList<Byte>();
        switch (mCatCode) {
            case MMPConstants.MMP_CATCODE_PHOTO:
                mMappedCats.add(MMPConstants.MMP_CATCODE_PHOTO_CAM);
                break;
            case MMPConstants.MMP_CATCODE_VIDEO:
                mMappedCats.add(MMPConstants.MMP_CATCODE_VIDEO_CAM);
                break;
            case MMPConstants.MMP_CATCODE_MUSIC:
                mMappedCats.add(MMPConstants.MMP_CATCODE_FILE);
                break;
            default:
                // May be smart data category. Else it is a bug.
                if (!MMLCategory.isSmartCategoryCode(mCatCode)) {
                    Log.w("MMPDeleteCategorySpec", "GUI BUG: Invalid category code to delete: " + mCatCode);
                }
        }
    }

    public MMPUpid getUpid() {
        return mUpid;
    }

    public byte getCatCode() {
        return mCatCode;
    }

    /**
     * A very old nasty hack in MMP which maps SDCARD items which are not displayed in GUI but need to be specified for data operations.
     *
     * @return
     */
    public ArrayList<Byte> getMappedCatCodes() {
        return mMappedCats;
    }

    public boolean isMirror() {
        return mIsMirror;
    }
}
