package com.meem.v2.businesslogic;

import android.annotation.SuppressLint;

import com.meem.androidapp.AppLocalData;
import com.meem.businesslogic.Session;
import com.meem.businesslogic.SessionGenericDataInfo;
import com.meem.businesslogic.SessionSmartDataInfo;
import com.meem.mmp.messages.MMPConstants;
import com.meem.mmp.messages.MMPFPath;
import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLSmartDataDesc;
import com.meem.utils.GenUtils;
import com.meem.v2.mmp.MMPV2Constants;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by arun on 25/5/17.
 */

public class SessionV2 extends Session {
    private static final String tag = "SessionV2";
    protected AppLocalData mAppData = AppLocalData.getInstance();

    public SessionV2(int mmpSessionType, Session.SessionStatUpdateHelper statHelper) {
        super(mmpSessionType, statHelper);
    }

    @Override
    public boolean prepareGenericDataInfoForBackup() {
        dbgTrace();

        int genCatMask = mVaultInfo.getGenericDataCategoryMask();
        int genPlusCatMask = mVaultInfo.getGenericPlusDataCategoryMask();

        ArrayList<Byte> genCats = MMLCategory.getGenericCatCodesArray(genCatMask);
        ArrayList<Byte> genPlusCats = MMLCategory.getGenericCatCodesArray(genPlusCatMask);

        dbgTrace("genCats: " + genCats);
        dbgTrace("genPlusCats: " + genPlusCats);

        if (null != mCatFilter) {
            dbgTrace("Applying individual generic category filters");
            mCatFilter.applyGenCatFilter(genCats);
            mCatFilter.applyGenPlusCatFilter(genPlusCats);

            genCatMask = 0;
            genPlusCatMask = 0;

            for (Byte cat : genCats) {
                genCatMask = MMLCategory.updateGenCatMask(genCatMask, cat, true);
            }

            for (Byte cat : genPlusCats) {
                genPlusCatMask = MMLCategory.updateGenCatMask(genPlusCatMask, cat, true);
            }

            // for all plus enabled categories, mirror is also enabled.
            genCatMask |= genPlusCatMask;

            dbgTrace("After filtering generic category mask: " + genCatMask);
        }

        mGenericDataInfo = new SessionGenericDataInfo(genCatMask, genPlusCatMask);
        return true;
    }

    /**
     * For restore and copy/sync operations, the category masks can not be taken from vault as the data need be put into phone can be
     * modified by user - e.g. avoiding sdcard items.
     * <p/>
     * Important: Filter applying and all is done already before calling this method
     * <p/>
     * TODO: The filter application and sdcard items stuff are now in main activity which is not that good - but it is put there only
     * because there were some user interaction as per old GUI spec. Now that there is no user interaction, we can move that part into here,
     * which is its proper place.
     *
     * @param genCatMask     - gen category mask
     * @param genPlusCatMask - always zero. (we do not consider plus data for restore/copy/sync)
     *
     * @return boolean
     */
    @Override
    public boolean prepareGenericDataInfoForRestoreOrCopy(int genCatMask, int genPlusCatMask) {
        mGenericDataInfo = new SessionGenericDataInfo(genCatMask, genPlusCatMask);
        return true;
    }

    @Override
    @SuppressLint("UseSparseArrays")
    public boolean prepareSmartDataInfo() {
        dbgTrace();

        mSmartDataInfo = new SessionSmartDataInfo();

        int smartCatMask = mVaultInfo.getSmartDataCategoryMask();
        int smartPlusCatMask = mVaultInfo.getSmartPlusDataCategoryMask();

        ArrayList<Byte> smartCats = MMLCategory.getSmartCatCodesArray(smartCatMask);
        ArrayList<Byte> smartPlusCats = MMLCategory.getSmartCatCodesArray(smartPlusCatMask);

        dbgTrace("Smart mirror cats: " + smartCats);
        dbgTrace("Smart plus cats: " + smartPlusCats);

        // apply filter (for any session)
        if (null != mCatFilter) {
            dbgTrace("Applying individual smart category filters");
            mCatFilter.applySmartCatFilter(smartCats);
            mCatFilter.applySmartPlusCatFilter(smartPlusCats);

            // for backup only: if mirror plus is enabled, mirror must be enabled.
            if (mType == MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION) {
                for (Byte cat : smartPlusCats) {
                    if (!smartCats.contains(cat)) {
                        smartCats.add(cat);
                    }
                }
            }

            dbgTrace("After filtering, smart mirror cats: " + smartCats);
            dbgTrace("After filtering, smart plus cats: " + smartPlusCats);
        } else {
            dbgTrace("No filter to apply.");
        }

		/* for a whole vault restore/copy session, consider only those categories that has data. */
        if (mCatFilter == null && (mType == MMPConstants.MMP_CODE_CREATE_RESTORE_SESSION || mType == MMPConstants.MMP_CODE_CREATE_CPY_SESSION)) {
            smartCatMask = 0x07;

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_CONTACT)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_CONTACT, false);
            }

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_MESSAGE)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_MESSAGE, false);
            }

            if (0 == mVaultInfo.getTotalSmartCatDataUsage(MMPConstants.MMP_CATCODE_CALENDER)) {
                smartCatMask = MMLCategory.updateSmartCatMask(smartCatMask, MMPConstants.MMP_CATCODE_CALENDER, false);
            }

            smartCats = MMLCategory.getSmartCatCodesArray(smartCatMask);

            // Arun: 11-11-2016: This is needed to make sure that the complicated FPath processing logic below is proper.
            // Anyway, tyhere is ultimately no effect as we are ignoring plus items for restore/copy.
            smartPlusCatMask = smartCatMask;
            smartPlusCats = MMLCategory.getSmartCatCodesArray(smartPlusCatMask);
        }

        dbgTrace("Finalised smart data category codes for session: mirror: " + smartCats + "; plus (ignore for restore/copy): " + smartPlusCats);

        AppLocalData appData = AppLocalData.getInstance();
        HashMap<Byte, ArrayList<MMPFPath>> allSmartCatsFilesMap = new HashMap<>();

        // merge filtered cat masks : TODO can be removed.
        ArrayList<Byte> allSmartCats = new ArrayList<>();
        allSmartCats.addAll(smartCats);
        for (Byte cat : smartPlusCats) {
            if (!allSmartCats.contains(cat)) {
                allSmartCats.add(cat);
            }
        }

        dbgTrace("After preparing smart data category mask: " + smartCats);

        mSmartDataInfo.setCatCodes(allSmartCats);
        mSmartDataInfo.setCatsFilesMap(allSmartCatsFilesMap);

        // === Added For V2 ===
        String upid = mVaultInfo.getUpid();

        mSmartDataInfo.mDbFilesMap = new HashMap<>();
        for (Byte cat : allSmartCats) {
            MMLSmartDataDesc smartDesc = new MMLSmartDataDesc();

            smartDesc.mCatCode = cat;
            smartDesc.mPaths = new String[2];

            String mirrInPath, plusInPath;

            // in files
            mirrInPath = appData.getSmartDataV2DatabasePath(upid, cat, true);

            // TODO: We will always go for plus mode for smart data.
            /*if(smartPlusCats.contains(cat)) { */
            plusInPath = appData.getSmartDataV2DatabasePath(upid, cat, false);
            /*}*/

            smartDesc.mPaths[0] = mirrInPath;
            smartDesc.mPaths[1] = plusInPath;

            smartDesc.mCSums[0] = GenUtils.getFileMD5(smartDesc.mPaths[0], null);
            smartDesc.mCSums[1] = GenUtils.getFileMD5(smartDesc.mPaths[1], null);

            smartDesc.mMeemPaths[0] = GenUtils.genFixedUniqueString(mirrInPath, MMPV2Constants.MMP_FILE_XFR_FNVHASH_LEN); // Note: constant value.
            smartDesc.mMeemPaths[1] = GenUtils.genFixedUniqueString(plusInPath, MMPV2Constants.MMP_FILE_XFR_FNVHASH_LEN); // Note: constant value.

            // Note: Other params will be filled by V2 backup logic.
            mSmartDataInfo.mDbFilesMap.put(cat, smartDesc);
        }

        return true;
    }

    // ===================================================
    // ------------------ Debug support ------------------
    // ===================================================
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("SessionV2.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("SessionV2.log");
    }
}
