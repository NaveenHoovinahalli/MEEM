package com.meem.cablemodel;

import com.meem.mmp.mml.MMLCategory;
import com.meem.mmp.mml.MMLGenericDataSettings;
import com.meem.mmp.mml.MMLSmartDataSettings;
import com.meem.utils.GenUtils;
import com.meem.utils.XMLDOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * This is the class contains all configuration information of each vault in MEEM cable. It can create and parse vault configuration files
 * specified in MML standard documentation.
 *
 * @author Arun T A
 */

public class VaultConfig {
    private static final String tag = "VaultConfig";

    String mTime = GenUtils.readableDateYYYYMMDDhhmmss();
    String mName = "My Phone";

    boolean mIsMirrorOnly = false;
    boolean mIsAuto = false;
    boolean mIsSound = false;
    boolean mIsSynced = false;

    String mLanguage = "English";
    String mPlatform = "Android";
    String mBrand = "Meem";
    int mLag;

    MMLSmartDataSettings mSmartMirrorSettings;
    MMLSmartDataSettings mSmartMirrorPlusSettings;

    MMLGenericDataSettings mGenericMirrorSettings;
    MMLGenericDataSettings mGenericMirrorPlusSettings;

    VaultUsage mUsage;
    SessionHistory mHistory;
    SyncSettings mSyncInfo;

    public VaultConfig() {
        mSmartMirrorSettings = new MMLSmartDataSettings(true);
        mSmartMirrorPlusSettings = new MMLSmartDataSettings(false);
        mGenericMirrorSettings = new MMLGenericDataSettings(true);
        mGenericMirrorPlusSettings = new MMLGenericDataSettings(false);

        mUsage = new VaultUsage();
        mHistory = new SessionHistory();
        mSyncInfo = null;
    }

    public boolean createVcfg(String path) {
        dbgTrace();
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(path, false));

            buf.append("<?mml version=\"1.0\"?>");

            buf.append("<vcfg>");

            mTime = GenUtils.readableDateYYYYMMDDhhmmss();
            buf.append("<time>" + mTime);
            buf.append("</time>");

            buf.append("<config>");

            buf.append("<name>" + GenUtils.sanitizeXml(mName));

            buf.append("</name>");

            buf.append("<flags>");
            buf.append("<mirror>" + (mIsMirrorOnly ? "yes" : "no"));
            buf.append("</mirror>");
            buf.append("<auto>" + (mIsAuto ? "yes" : "no"));
            buf.append("</auto>");
            buf.append("<sound>" + ((null == mSyncInfo) ? "no" : "yes")); // HACK for FW: 02Feb2015
            buf.append("</sound>");
            buf.append("</flags>");

            buf.append("<language>" + mLanguage);
            buf.append("</language>");

            buf.append("<lag>" + String.valueOf(mLag));
            buf.append("</lag>");

            buf.append("<backup-category>");

            buf.append("<smart-data>");
            buf.append("<mirror>");
            buf.append(mSmartMirrorSettings.toString());
            buf.append("</mirror>");
            buf.append("<mirror-plus>");
            buf.append(mSmartMirrorPlusSettings.toString());
            buf.append("</mirror-plus>");
            buf.append("</smart-data>");

            buf.append("<generic-data>");
            buf.append("<mirror>");
            buf.append(mGenericMirrorSettings.toString());
            buf.append("</mirror>");
            buf.append("<mirror-plus>");
            buf.append(mGenericMirrorPlusSettings.toString());
            buf.append("</mirror-plus>");
            buf.append("</generic-data>");

            buf.append("</backup-category>");

            // my my... here VCFG is using XML attributes! So quoted strings are mandatory.
            if (mSyncInfo != null) {
                buf.append("<sync-info upid=" + "\"" + mSyncInfo.mUpid + "\"");
                if (mSyncInfo.mSmartDataSettings != null) {
                    buf.append(" smart=" + mSyncInfo.mSmartDataSettings.toXMLStringAttribute());
                }

                if (mSyncInfo.mGenDataSettings != null) {
                    buf.append(" generic=" + mSyncInfo.mGenDataSettings.toXMLStringAttribute());
                }
                buf.append("/>");
            }

            buf.append("</config>");
            buf.append("</vcfg>");

            buf.close();
        } catch (Exception ex) {
            dbgTrace("Vault config XML creation failed: " + ex.getMessage());
            return false;
        }

        return true;
    }

    public boolean parseVstat(String path) {
        dbgTrace();

        XMLDOMParser parser = new XMLDOMParser();
        Document doc = parser.getDomElement(path);
        NodeList nl, nl1, nl2;
        Element el, el1, el2;

        if (doc == null) {
            return false;
        }

        try {
            nl = doc.getElementsByTagName("time");
            el = (Element) nl.item(0);
            mTime = parser.getElementValue(el);

            nl = doc.getElementsByTagName("name");
            el = (Element) nl.item(0);
            mName = parser.getElementValue(el);

            nl = doc.getElementsByTagName("mirror");
            el = (Element) nl.item(0);
            String snow = parser.getElementValue(el); // snow... ;)
            mIsMirrorOnly = snow.equalsIgnoreCase("yes");

            nl = doc.getElementsByTagName("auto");
            el = (Element) nl.item(0);
            snow = parser.getElementValue(el);
            mIsAuto = snow.equalsIgnoreCase("yes");

            nl = doc.getElementsByTagName("sound");
            el = (Element) nl.item(0);
            snow = parser.getElementValue(el);
            mIsSound = snow.equalsIgnoreCase("yes");

            nl = doc.getElementsByTagName("language");
            el = (Element) nl.item(0);
            mLanguage = parser.getElementValue(el);

            nl = doc.getElementsByTagName("lag");
            el = (Element) nl.item(0);
            mLag = Integer.valueOf(parser.getElementValue(el));

            nl = doc.getElementsByTagName("platform");
            el = (Element) nl.item(0);
            mPlatform = parser.getElementValue(el);

            nl = doc.getElementsByTagName("brand");
            el = (Element) nl.item(0);
            mBrand = parser.getElementValue(el);

            nl = doc.getElementsByTagName("smart-data");
            el = (Element) nl.item(0);

            nl1 = el.getElementsByTagName("mirror");
            el1 = (Element) nl1.item(0);
            if (null == el1) {
                dbgTrace("Warning: Smartdata mirror category string parsed as null");
                mSmartMirrorSettings.fromString(null);
            } else {
                mSmartMirrorSettings.fromString(parser.getElementValue(el1));
            }
            nl1 = el.getElementsByTagName("mirror-plus");
            el1 = (Element) nl1.item(0);
            if (null == el1) {
                dbgTrace("Warning: Smartdata mirror+ category string parsed as null");
                mSmartMirrorPlusSettings.fromString(null);
            } else {
                mSmartMirrorPlusSettings.fromString(parser.getElementValue(el1));
            }

            nl = doc.getElementsByTagName("generic-data");
            el = (Element) nl.item(0);

            nl1 = el.getElementsByTagName("mirror");
            el1 = (Element) nl1.item(0);
            if (null == el1) {
                dbgTrace("Warning: Genericdata mirror category string parsed as null");
                mGenericMirrorSettings.fromString(null);
            } else {
                mGenericMirrorSettings.fromString(parser.getElementValue(el1));
            }
            nl1 = el.getElementsByTagName("mirror-plus");
            el1 = (Element) nl1.item(0);
            if (null == el1) {
                dbgTrace("Warning: Genericdata mirror+ category string parsed as null");
                mGenericMirrorPlusSettings.fromString(null);
            } else {
                mGenericMirrorPlusSettings.fromString(parser.getElementValue(el1));
            }

            // sync info tag parsing begins
            // my my... here VCFG is using XML attributes! So quoted strings are mandatory.
            if (mIsSound) { // HACK for FW: 02Feb2015
                nl = doc.getElementsByTagName("sync-info");
                if (nl != null && 0 != nl.getLength()) {
                    dbgTrace("Sync is enabled");
                    mIsSynced = true;

                    mSyncInfo = new SyncSettings();
                    el = (Element) nl.item(0);

                    mSyncInfo.mUpid = el.getAttribute("upid");
                    dbgTrace("Sync enabled with: " + mSyncInfo.mUpid);

                    String smartCats = el.getAttribute("smart");
                    if (smartCats != null && !smartCats.isEmpty()) {
                        dbgTrace("Sync enabled smart cats: " + smartCats);
                        mSyncInfo.mSmartDataSettings.fromString(smartCats);
                    }

                    String genCats = el.getAttribute("generic");
                    if (genCats != null && !genCats.isEmpty()) {
                        dbgTrace("Sync enabled gen cats: " + genCats);
                        mSyncInfo.mGenDataSettings.fromString(genCats);
                    }
                }
            }

            // usage statistics tag parsing begins
            int n, i = 0;
            nl = doc.getElementsByTagName("usage");
            el = (Element) nl.item(0);

            // smart data usage
            nl1 = el.getElementsByTagName("smart");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getChildNodes(); // this contains all smart cats tags
            n = nl2.getLength();
            i = 0;
            for (i = 0; i < n; i++) {
                el2 = (Element) nl2.item(i);
                String catName = el2.getNodeName();

                byte catCode = MMLCategory.toSmartCatCode(catName);

                String sizeStr = el2.getAttribute("mSize");
                long mSize = Long.parseLong(sizeStr);

                sizeStr = el2.getAttribute("pSize");
                long pSize = Long.parseLong(sizeStr);

                dbgTrace("Usage: " + catName + " : mSize: " + String.valueOf(mSize) + " : pSize: " + String.valueOf(pSize));
                mUsage.setSmartCatUsage(catCode, mSize, pSize);
            }

            // generic data usage
            nl1 = el.getElementsByTagName("generic");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getChildNodes(); // this contains all smart cats tags
            n = nl2.getLength();
            i = 0;
            for (i = 0; i < n; i++) {
                el2 = (Element) nl2.item(i);
                String catName = el2.getNodeName();

                byte catCode = MMLCategory.toGenericCatCode(catName);

                String sizeStr = el2.getAttribute("mSize");
                long mSize = Long.parseLong(sizeStr);

                sizeStr = el2.getAttribute("pSize");
                long pSize = Long.parseLong(sizeStr);

                dbgTrace("Usage: " + catName + " : mSize: " + String.valueOf(mSize) + " : pSize: " + String.valueOf(pSize));
                mUsage.setGenCatUsage(catCode, mSize, pSize);
            }

            // last session times
            String timeStr;
            long lastTime;
            boolean bStatus;

            nl = doc.getElementsByTagName("previous-sessions");
            el = (Element) nl.item(0);

            // last backup time
            nl1 = el.getElementsByTagName("backup");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getElementsByTagName("time");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            lastTime = Long.parseLong(timeStr);

            nl2 = el1.getElementsByTagName("status");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            bStatus = (null != timeStr && timeStr.equals("completed")) ? true : false;

            mHistory.mLastBackupTime = lastTime;
            mHistory.mLastBackupComplete = bStatus;

            // last restore time
            nl1 = el.getElementsByTagName("restore");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getElementsByTagName("time");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            lastTime = Long.parseLong(timeStr);

            nl2 = el1.getElementsByTagName("status");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            bStatus = (null != timeStr && timeStr.equals("completed")) ? true : false;

            mHistory.mLastRestoreTime = lastTime;
            mHistory.mLastRestoreComplete = bStatus;

            // last copy time
            nl1 = el.getElementsByTagName("cpy");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getElementsByTagName("time");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            lastTime = Long.parseLong(timeStr);

            nl2 = el1.getElementsByTagName("status");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            bStatus = (null != timeStr && timeStr.equals("completed")) ? true : false;

            mHistory.mLastCopyTime = lastTime;
            mHistory.mLastCopyComplete = bStatus;

            // copy UPID (can be null)
            nl2 = el1.getElementsByTagName("cpy-upid");
            el2 = (Element) nl2.item(0);
            mHistory.mCopyUpid = parser.getElementValue(el2);

            // last sync time
            nl1 = el.getElementsByTagName("sync");
            el1 = (Element) nl1.item(0);

            nl2 = el1.getElementsByTagName("time");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            lastTime = Long.parseLong(timeStr);

            nl2 = el1.getElementsByTagName("status");
            el2 = (Element) nl2.item(0);
            timeStr = parser.getElementValue(el2);
            bStatus = (null != timeStr && timeStr.equals("completed")) ? true : false;

            mHistory.mLastSyncTime = lastTime;
            mHistory.mLastSyncComplete = bStatus;

            // sync UPID (can be null)
            nl2 = el1.getElementsByTagName("sync-upid");
            el2 = (Element) nl2.item(0);
            mHistory.mSyncUpid = parser.getElementValue(el2);

            dbgTrace("Last backup time: " + String.valueOf(mHistory.mLastBackupTime));

        } catch (Exception ex) {
            dbgTrace("Parsing VSTAT failed: " + ex.getMessage());
            return false;
        }

        dbgTrace("Parsing VSTAT finished");
        return true;
    }

    public final VaultUsage getUsageInfo() {
        return mUsage;
    }

    public final SessionHistory getHistory() {
        return mHistory;
    }

    public boolean isSyncEnabled() {
        return (mIsSynced || mIsSound); // HACK for FW for the time being: 02Feb2015.
    }

    public SyncSettings getSyncInfo() {
        return (mIsSynced || mIsSound) ? mSyncInfo : null;
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("VaultConfig.log", trace);
    }

    // debug support
    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}
