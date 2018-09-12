package com.meem.cablemodel;

import com.meem.androidapp.ProductSpecs;
import com.meem.utils.GenUtils;
import com.meem.utils.XMLDOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Essentially contains all status info that is retrieved from the cable using GET_MSTAT command. All class members are kept package private
 * for obvious reasons.
 *
 * @author Arun T A
 */
public class MeemConfig {
    private static final String tag = "MeemConfig";

    String mTime;
    String mName;
    String mOldName;

    boolean mIsMultiPhone;
    String mIdentifier;
    boolean mIsCrypted;

    int mNumVaults;
    Map<String, MeemVault> mVaults;

    MeemUsage mUsage;

    String mFwVersion;
    String mFwDate;

    /**
     * Only deal with absolutely necessary members.
     */
    public MeemConfig() {
        mName = "My MEEM";
        mOldName = "My MEEM";
        mNumVaults = 0;
        mVaults = new HashMap<String, MeemVault>();
        mUsage = new MeemUsage();
    }

    public boolean createMcfg(String path) {
        dbgTrace();

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(path, false));

            buf.append("<?mml version=\"1.0\"?>");

            buf.append("<mcfg>");

            mTime = GenUtils.readableDateYYYYMMDDhhmmss();
            buf.append("<time>" + mTime);
            buf.append("</time>");

            buf.append("<name>" + mName);
            buf.append("</name>");

            buf.append("<flags>");
            buf.append("<multiphone>" + (mIsMultiPhone ? "yes" : "no"));
            buf.append("</multiphone>");
            buf.append("</flags>");
            buf.append("</mcfg>");

            buf.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Update the status using given MSTAT file.
     */
    public boolean update(String mstatFile) {
        dbgTrace();

        // Arun: Fix for breaking MVC, 02July2013
        // mNumVaults = 0;
        // mVaults.clear();
        return parseMstatFile(mstatFile);
    }

    private boolean parseMstatFile(String mstatFile) {
        dbgTrace();

        XMLDOMParser parser = new XMLDOMParser();
        Document doc = parser.getDomElement(mstatFile);
        NodeList nl;
        Element el;

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

            nl = doc.getElementsByTagName("multiphone");
            el = (Element) nl.item(0);
            String snow = parser.getElementValue(el); // snow... ;)
            mIsMultiPhone = snow.equalsIgnoreCase("yes");

            nl = doc.getElementsByTagName("device-id");
            el = (Element) nl.item(0);
            mIdentifier = parser.getElementValue(el);

            nl = doc.getElementsByTagName("crypto");
            el = (Element) nl.item(0);
            snow = parser.getElementValue(el);
            mIsCrypted = snow.equalsIgnoreCase("yes");

            nl = doc.getElementsByTagName("vault");
            mNumVaults = nl.getLength();
            if (mNumVaults > ProductSpecs.LIMIT_MAX_VAULTS) {
                return false;
            }
            // parse all vaults
            for (int i = 0; i < mNumVaults; i++) {
                MeemVault vault = new MeemVault();
                el = (Element) nl.item(i);

                NodeList nl1 = el.getElementsByTagName("name");
                Element el1 = (Element) nl1.item(0);
                vault.setName(parser.getElementValue(el1));

                nl1 = el.getElementsByTagName("upid");
                el1 = (Element) nl1.item(0);
                String upid = parser.getElementValue(el1);
                vault.setUpid(upid);

                if (mVaults.containsKey(upid)) {
                    // This vault will be updated on getVSTAT and rest of it
                    dbgTrace("Vault is already present in model, should be updated soon by VSTAT. upid: " + upid);
                } else {
                    dbgTrace("Adding new vault to list, should be updated soon by VSTAT. upid: " + upid);
                    mVaults.put(vault.getUpid(), vault);
                }
            }

            nl = doc.getElementsByTagName("capacity");
            el = (Element) nl.item(0);
            String strLong = parser.getElementValue(el);
            mUsage.mTotalStorage = Long.parseLong(strLong);

            dbgTrace("Total storage: " + mUsage.mTotalStorage);

            nl = doc.getElementsByTagName("used");
            el = (Element) nl.item(0);
            strLong = parser.getElementValue(el);
            mUsage.mUsedStorage = Long.parseLong(strLong);

            nl = doc.getElementsByTagName("free");
            el = (Element) nl.item(0);
            strLong = parser.getElementValue(el);
            mUsage.mFreeStorage = Long.parseLong(strLong);

            nl = doc.getElementsByTagName("reserved");
            el = (Element) nl.item(0);
            strLong = parser.getElementValue(el);
            mUsage.mRsvdStorage = Long.parseLong(strLong);

            nl = doc.getElementsByTagName("build");
            el = (Element) nl.item(0);
            mFwVersion = el.getAttribute("version");
            mFwDate = el.getAttribute("date");

        } catch (Exception ex) {
            dbgTrace("Error parsing mstat: " + ex.getMessage());
            return false;
        }

        return true;
    }

    public MeemVault getVault(String upid) {
        MeemVault vault = mVaults.get(upid);

        if (null == vault) {
            dbgTrace("No vault found with given upid: " + upid);
        }

        return vault;
    }

    public final MeemUsage getUsageInfo() {
        return mUsage;
    }

    public final String getFwVersion() {
        return mFwVersion;
    }

    public final String getFwDate() {
        return mFwDate;
    }

    // debug support
    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("MeemConfig.log", trace);
    }

    // debug support
    private void dbgTrace() {
        dbgTrace(Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    // Arun: This shall be called only if the view is not using this vault model
    // object anymore!
    public void deleteVault(String upid) {
        if (!mVaults.containsKey(upid)) {
            dbgTrace("Possible bug: Upid is not present in vault map: " + upid);
        } else {
            dbgTrace("Removing upid from vault map: " + upid);
            mVaults.remove(upid);
        }
    }
}
