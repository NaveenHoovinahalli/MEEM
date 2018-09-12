package com.meem.fwupdate;

import com.meem.androidapp.ProductSpecs;
import com.meem.androidapp.UiContext;
import com.meem.utils.GenUtils;
import com.meem.utils.XMLDOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

/**
 * Parser for update manifest file.
 *
 * @author Arun T A
 */

public class UpdateManifest {
    public static final String ELMT_UPDATE = "update";
    public static final String ELMT_FIRMWARE = "firmware";
    private static final String tag = "UpdateManifest";
    private static final String ELMT_INFO = "info";
    private static final String ATTR_SIZE = "size";
    private static final String ATTR_DATE = "date";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_MEEMVER = "meemVer";
    private static final String ATTR_TOOLVER = "toolVer";
    private static final String ATTR_PRIO = "priority";
    private static final String ELMT_CHECKSUM = "checksum";
    private static final String ATTR_CSUM_TYPE = "type";
    private static final String ATTR_CSUM_VALUE = "value";
    private static final String ELMT_DESCRIPTION = "description";
    private static final String ATTR_DESC_LANG = "language";
    private static final String ATTR_DESC_TEXT = "text";
    @SuppressWarnings("unused")
    private static final String ELMT_FILE = "file";
    private static final String ELMT_MIRROR = "mirror";
    private static final String ATTR_MIRROR_SERVER = "url";
    private static final String ATTR_MIRROR_FWFILE = "name";
    UiContext mUiCtxt = UiContext.getInstance();
    ArrayList<UpdateInfo> mFwUpdateList = new ArrayList<UpdateInfo>();
    private String mManifestFileLocalPath = "";

    public UpdateManifest(String filePath) {
        mManifestFileLocalPath = filePath;
    }

    public ArrayList<UpdateInfo> process() {
        XMLDOMParser parser = new XMLDOMParser();
        Document doc = parser.getDomElement(mManifestFileLocalPath);

        int i = 0, j = 0, k = 0;
        NodeList fwNodeList, infoNodeList, nl;

        if (doc == null) {
            throw new IllegalStateException("Invalid or corrupted manifest file");
        }

        fwNodeList = doc.getElementsByTagName(ELMT_FIRMWARE);
        dbgTrace("Number of firmware listed: " + String.valueOf(fwNodeList.getLength()));

        for (i = 0; i < fwNodeList.getLength(); i++) {
            Element fwElement = (Element) fwNodeList.item(i);

            infoNodeList = fwElement.getElementsByTagName(ELMT_INFO);
            if (0 == infoNodeList.getLength()) {
                dbgTrace("Firmware tag has no information!");
                break;
            }

            UpdateInfo updInfo = new UpdateInfo();

            for (j = 0; j < infoNodeList.getLength(); j++) {
                Element ie = (Element) infoNodeList.item(j);
                updInfo.mFwSize = parser.getAttribute(ie, ATTR_SIZE);
                updInfo.mFwSize = GenUtils.readableFileSize((Long.valueOf(updInfo.mFwSize) / 1024));

                updInfo.mFwDate = parser.getAttribute(ie, ATTR_DATE);
                updInfo.mFwDate = GenUtils.readableDateYYYYMMDD(updInfo.mFwDate);

                updInfo.mFwTime = parser.getAttribute(ie, ATTR_TIME);
                updInfo.mFwNewVersion = parser.getAttribute(ie, ATTR_VERSION);
                updInfo.mFwReqMeemVersion = parser.getAttribute(ie, ATTR_MEEMVER);
                updInfo.mFwToolVersion = parser.getAttribute(ie, ATTR_TOOLVER);
                updInfo.mFwPriority = parser.getAttribute(ie, ATTR_PRIO);

                nl = ie.getElementsByTagName(ELMT_CHECKSUM);

                for (k = 0; k < nl.getLength(); k++) {
                    Element e = (Element) nl.item(k);

                    updInfo.mFwCSumType = parser.getAttribute(e, ATTR_CSUM_TYPE);
                    updInfo.mFwCSumValue = parser.getAttribute(e, ATTR_CSUM_VALUE);
                }

                nl = ie.getElementsByTagName(ELMT_DESCRIPTION);

                for (k = 0; k < nl.getLength(); k++) {
                    Element e = (Element) nl.item(k);

                    updInfo.mFwDescLanguage = parser.getAttribute(e, ATTR_DESC_LANG);
                    updInfo.mFwDescText = parser.getAttribute(e, ATTR_DESC_TEXT);
                }
            }

            nl = fwElement.getElementsByTagName(ELMT_MIRROR);

            for (k = 0; k < nl.getLength(); k++) {
                Element e = (Element) nl.item(k);

                String url = parser.getAttribute(e, ATTR_MIRROR_SERVER);
                String remoteFile = parser.getAttribute(e, ATTR_MIRROR_FWFILE);

                // Deal with FTP username password stuff
                String canonUrl = getCanonicalFwDownloadUrl(url + "/" + remoteFile);
                updInfo.setUrl(canonUrl);
            }

            mFwUpdateList.add(updInfo);
            dbgTrace("One firmware tag parsed.");
        } // for all firmware tags

        return mFwUpdateList;
    }

    /**
     * This function is mainly for urls inside manifest file - if it is FTP and if we have username and password, manifest file will not
     * contain the username and password (ftp://hostname/path/to/fimrware.dat). In this case, we need to make a FTP url with embedded
     * username and password - a format that is acceptable for URLCOnnection, for example (ftp://username:passworrd@hostname/path/to/firmware.dat).
     *
     * @param parsedUrl
     *
     * @return see description above
     */

    private String getCanonicalFwDownloadUrl(String parsedUrl) {
        dbgTrace();

        StringBuffer canonUrl = new StringBuffer(parsedUrl);

        String ftpPrefix = "ftp://";
        String credentials;

        if (ProductSpecs.FW_UPDATE_USING_FTP) {
            if ((null != ProductSpecs.FW_UPDATE_SERVER_USERNAME) && (null != ProductSpecs.FW_UPDATE_SERVER_PASSWORD)) {
                credentials = ProductSpecs.FW_UPDATE_SERVER_USERNAME + ":" + ProductSpecs.FW_UPDATE_SERVER_PASSWORD + "@";

                if (parsedUrl.startsWith(ftpPrefix)) {
                    canonUrl.insert(ftpPrefix.length(), credentials);
                } else {
                    dbgTrace("Malformed firmware download url: " + parsedUrl);
                }
            }
        } else {
            dbgTrace("HTTP url: " + parsedUrl);
        }

        return canonUrl.toString();
    }

    // ===================================================================
    // Debug support - Logging to file
    // ===================================================================

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("UpdateManifest.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("UpdateManifest.log");
    }
}
