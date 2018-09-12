package com.meem.ui;

/**
 * Created by SCS on 8/11/2016.
 */
public class ThumbnailsModel {

    byte[] images;
    String srcFilePath;
    String secCsum;
    int fwAck;

    public byte[] getImages() {
        return images;
    }

    public void setImages(byte[] images) {
        this.images = images;
    }

    public String getSrcFilePath() {
        return srcFilePath;
    }

    public void setSrcFilePath(String srcFilePath) {
        this.srcFilePath = srcFilePath;
    }

    public String getSecCsum() {
        return secCsum;
    }

    public void setSecCsum(String secCsum) {
        this.secCsum = secCsum;
    }

    public int getFwAck() {
        return fwAck;
    }

    public void setFwAck(int fwAck) {
        this.fwAck = fwAck;
    }
}
