package com.meem.fwupdate;

/**
 * @author Arun T A
 */

public class UpdateInfo {

    /**
     * TODO: make all these members package private after firmware manager is integrated.
     */
    public String mFwSize = "";
    public String mFwDate = "";
    public String mFwTime = "";

    public String mFwNewVersion = "";
    public String mFwReqMeemVersion = "";
    public String mFwToolVersion = "";

    public String mFwPriority = "";
    public String mFwCSumType = "";
    public String mFwCSumValue = "";
    public String mFwDescLanguage = "";
    public String mFwDescText = "";

    public String mFwUpdateUrl = "";
    public String mFwUpdateLocalFile = "";

    public UpdateInfo() {
        // nothing
    }

    public String getmFwUpdateLocalFile() {
        return mFwUpdateLocalFile;
    }

    public void setmFwUpdateLocalFile(String mFwUpdateLocalFile) {
        this.mFwUpdateLocalFile = mFwUpdateLocalFile;
    }

    public String getUrl() {
        return mFwUpdateUrl;
    }

    public void setUrl(String mFwUpdateUrl) {
        this.mFwUpdateUrl = mFwUpdateUrl;
    }

    public String toString() {
        return mFwNewVersion;
    }

    public boolean equels(UpdateInfo other) {
        // ignore checksum algorithm type. we are not going to be that elaborate.
        return (this.mFwCSumValue.equals(other.mFwCSumValue));
    }

    public String toDescription() {
        StringBuilder sb = new StringBuilder();

        sb.append("Date: ").append(mFwDate);
        sb.append("Applicable to version: ").append(mFwReqMeemVersion).append("\n");
        sb.append("Importance: ").append(mFwPriority).append(" ");
        sb.append("Size: ").append(mFwSize).append("\n");
        sb.append("Description: ").append(mFwDescText).append("\n");

        return sb.toString();
    }
}
