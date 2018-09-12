package com.meem.fwupdate;

import android.util.Log;

import com.meem.utils.GenUtils;
import com.meem.utils.XMLDOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * Always check with the original FW update application in Carbon SVN to understand this derived code. As a quick info note: This class
 * essentially parses parts of MSTAT file to retrieve the meem FW version.
 *
 * @author Arun T A
 */

public class MeemFwStatus {
    // MSTAT tags
    private static String MSTAT_ELMNT_BUILDINFO = "build";
    private static String MSTAT_ATTR_BUILD_DATE = "date";
    private static String MSTAT_ATTR_BUILD_TIME = "time";
    private static String MSTAT_ATTR_BUILD_VERSION = "version";
    private final String mPreHistoricMeemVersion = "0.1.95.0"; // :-)
    public String mMeemFwDate;
    public String mMeemFwTime;
    public String mMeemCurrFwVersion;

    public MeemFwStatus() {

    }

    public boolean processMSTAT(String mstatFile) {
        File fileMSTAT = new File(mstatFile);
        if (!fileMSTAT.exists()) {
            Log.w("MeemFwStatus", "MSTAT file not found. May be, no cable is connected yet.");
            return false;
        }

        XMLDOMParser parser = new XMLDOMParser();
        Document doc = parser.getDomElement(mstatFile);
        int i = 0;
        NodeList nl;

        boolean result = true;

        try {
            nl = doc.getElementsByTagName(MSTAT_ELMNT_BUILDINFO);
            if ((nl == null) || ((nl != null) && (0 == nl.getLength()))) {
                Log.w("MeemFwStatus", "Unsupported old meem version, work around applied");
                mMeemFwDate = "20140101";
                mMeemFwTime = "000000";
                mMeemCurrFwVersion = mPreHistoricMeemVersion;
            } else {
                for (i = 0; i < nl.getLength(); i++) {
                    Element e = (Element) nl.item(i);

                    mMeemFwDate = parser.getAttribute(e, MSTAT_ATTR_BUILD_DATE);
                    mMeemFwTime = parser.getAttribute(e, MSTAT_ATTR_BUILD_TIME);
                    mMeemCurrFwVersion = parser.getAttribute(e, MSTAT_ATTR_BUILD_VERSION);
                }
            }

            mMeemFwDate = GenUtils.readableDateYYYYMMDD(mMeemFwDate);
        } catch (Exception e) {
            result = false;
        }

        return result;
    }
}
