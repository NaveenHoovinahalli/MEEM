package com.meem.mmp.messages;

import java.nio.ByteBuffer;

/**
 * @author Arun T A
 */

class MMPCtrlMsgHeader {

    private int mMagic;
    private short mMajor;
    private byte mMinor;
    private int mMsgLen;
    private int mSeqNum;
    private byte mFlags;
    private byte mMsgType;
    private int mMMPCode;
    private int mPartIndex;
    private short mCheckSum;

    private int mHeaderLen;

    // for sending, creating a new header
    MMPCtrlMsgHeader() {
        mMagic = MMPConstants.MMP_MAGIC;
        mMajor = MMPConstants.MMP_MAJOR_REV;
        mMinor = MMPConstants.MMP_MINOR_REV;
        mFlags = 0;
        mPartIndex = 0;
        mCheckSum = 0;
        mSeqNum = 1;
        mMsgLen = MMPConstants.MMP_CTRL_MSG_HEADER_LEN;
        mHeaderLen = MMPConstants.MMP_CTRL_MSG_HEADER_LEN;
    }

    // for received packet processing
    MMPCtrlMsgHeader(ByteBuffer pkt) {
        pkt.rewind();

        mMagic = pkt.getInt();
        mMajor = pkt.getShort();
        mMinor = pkt.get();
        pkt.get(); // rsvd
        mMsgLen = pkt.getInt();
        mSeqNum = pkt.getInt();

        int flagAndCode = pkt.getInt();
        mFlags = (byte) (flagAndCode >> 24);
        mMMPCode = flagAndCode & 0x00FFFFFF;
        mMsgType = (byte) (mFlags & 0x0F);
        mHeaderLen = MMPConstants.MMP_CTRL_MSG_HEADER_LEN;
        mCheckSum = pkt.getShort(mMsgLen - 2);
    }

    public int getRevision() {
        int rev = 0;
        rev = (mMajor << 16) | (mMinor << 8);
        return rev;
    }

    public int getMagicNumber() {
        return mMagic;
    }

    public void setMagicNumber(int magic) {
        this.mMagic = magic;
    }

    public short getMajorRev() {
        return mMajor;
    }

    public void setMajorRev(short major) {
        this.mMajor = major;
    }

    public byte getMinorRev() {
        return mMinor;
    }

    public void setMinorRev(byte minor) {
        this.mMinor = minor;
    }

    public int getMessageLength() {
        return mMsgLen;
    }

    // careful! there is a reason why this is made package private
    void setMessageLength(int length) {
        this.mMsgLen = length;
    }

    public int getHeaderLength() {
        return mHeaderLen;
    }

    public int getSequenceNumber() {
        return mSeqNum;
    }

    public void setSequenceNumber(int seqNum) {
        this.mSeqNum = seqNum;
    }

    public byte getFlags() {
        return mFlags;
    }

    public byte getType() {
        return mMsgType;
    }

    public void setType(byte msgType) {
        mMsgType = msgType;
    }

    public int getMessageCode() {
        return mMMPCode;
    }

    public void setMessageCode(int mmpCode) {
        mMMPCode = mmpCode;
    }

    public int getPartIndex() {
        return mPartIndex;
    }

    public void setPartIndex(int partIndex) {
        mPartIndex = partIndex;
    }

    public short getCheckSum() {
        return mCheckSum;
    }

    public void setCheckSum(short checkSum) {
        mCheckSum = checkSum;
    }
}
