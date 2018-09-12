package com.meem.mmp.messages;

import com.meem.androidapp.UiContext;
import com.meem.core.MeemCore;
import com.meem.utils.GenUtils;

import java.nio.ByteBuffer;

/**
 * One of the most fundamental class in MMP implementation. See comments below for good understanding. Do not modify unless you know what
 * you are doing.
 *
 * @author Arun T A
 */

public class MMPCtrlMsg {
    private ByteBuffer mMsg;
    private MMPCtrlMsgHeader mHeader;

    /**
     * This is to create a control command object. A header will be created and populated for you with default parameters. You must modify
     * necessary parameters of it (mostly, only command code). Other parameters can be set by addXXX type of APIs. The length field in
     * header, checksum in message etc will be automatically handled for you. Remember to do it in proper order as per MMP documentation
     * because byte array internally will be filled up in the order you call those addParam functions.
     */
    MMPCtrlMsg(int code) {
        mMsg = ByteBuffer.allocate(MMPConstants.MMP_PKT_SIZE);
        mHeader = new MMPCtrlMsgHeader();
        mHeader.setType(MMPConstants.MMP_TYPE_CMD);
        mHeader.setMessageCode(code);
        mMsg.position(mHeader.getHeaderLength());
    }

    /**
     * This is to create a response object from a received response from cable. A header will be created and populated for you with received
     * parameters. You must call getHeader() and take necessary parameters of it (mostly, response code). Use getXXX functions in exact
     * order as per MMP specification to retrieve parameters.
     * <p/>
     * This constructor is used only by the MMPHandler object.
     */
    public MMPCtrlMsg(ByteBuffer pkt) {
        mMsg = pkt; // java: reference, not copy.
        mHeader = new MMPCtrlMsgHeader(mMsg);
        mMsg.position(mHeader.getHeaderLength());
    }

    /**
     * This is to create a response object from a received command from cable, after it was converted to a MMPCtrlMsg object. A header will
     * be created and populated for you with received parameters. You must call getHeader() and take necessary parameters of it (mostly,
     * response code). Use getXXX functions in exact order as per MMP specification to retrieve parameters.
     */
    public MMPCtrlMsg(MMPCtrlMsg copy) {
        this.mMsg = copy.mMsg;
        mHeader = copy.mHeader;
    }

    // Internal function that converts a header to byte array.
    private void setHeader(MMPCtrlMsgHeader hdr) {
        mMsg.rewind();
        mMsg.putInt(hdr.getMagicNumber());
        mMsg.putShort(hdr.getMajorRev());
        mMsg.put(hdr.getMinorRev());
        mMsg.put(MMPConstants.MMP_RESERVED);
        mMsg.putInt(hdr.getMessageLength());
        mMsg.putInt(hdr.getSequenceNumber());
        int flagsAndCode;
        int flags = (int) ((hdr.getFlags() & 0xF0) | hdr.getType());
        int code = hdr.getMessageCode();
        flagsAndCode = (flags << 24) | code;
        mMsg.putInt(flagsAndCode);
    }

    // ============================================
    // ============= the header part ==============
    // ============================================

    public int getRevision() {
        return mHeader.getRevision();
    }

    public int getMagicNumber() {
        return mHeader.getMagicNumber();
    }

    public void setMagicNumber(int magic) {
        mHeader.setMagicNumber(magic);
    }

    public short getMajorRev() {
        return mHeader.getMajorRev();
    }

    public void setMajorRev(short major) {
        mHeader.setMajorRev(major);
    }

    public byte getMinorRev() {
        return mHeader.getMinorRev();
    }

    public void setMinorRev(byte minor) {
        mHeader.setMajorRev(minor);
    }

    // Remember: setMessageLength is not needed as it is
    // done transparently while adding parameters.
    public int getMessageLength() {
        return mHeader.getMessageLength();
    }

    public int getHeaderLength() {
        return mHeader.getHeaderLength();
    }

    public int getSequenceNumber() {
        return mHeader.getSequenceNumber();
    }

    public void setSequenceNumber(int seqn) {
        mHeader.setSequenceNumber(seqn);
    }

    public byte getFlags() {
        return mHeader.getFlags();
    }

    public byte getType() {
        return mHeader.getType();
    }

    public void setType(byte type) {
        mHeader.setType(type);
    }

    public int getMessageCode() {
        return mHeader.getMessageCode();
    }

    public void setMessageCode(int code) {
        mHeader.setMessageCode(code);
    }

    public int getPartIndex() {
        return mHeader.getPartIndex();
    }

    public void setPartIndex(int pidx) {
        mHeader.setPartIndex(pidx);
    }

    public short getCheckSum() {
        return mHeader.getCheckSum();
    }

    public void setCheckSum(short checkSum) {
        mHeader.setCheckSum(checkSum);
    }

    // ======================================================
    // ============= the parameter buffer part ==============
    // ======================================================

    public int getErrorCode() {
        return mMsg.getInt(MMPConstants.MMP_CTRL_MSG_HEADER_LEN);
    }

    // ======= all the following are package private ========

    void addParam(byte b) {
        mMsg.put(b);
        int len = mHeader.getMessageLength();
        mHeader.setMessageLength(len + 1);
    }

    void addParam(short s) {
        mMsg.putShort(s);
        int len = mHeader.getMessageLength();
        mHeader.setMessageLength(len + 2);
    }

    void addParam(int i) {
        mMsg.putInt(i);
        int len = mHeader.getMessageLength();
        mHeader.setMessageLength(len + 4);
    }

    void addParam(byte[] ba) {
        mMsg.put(ba);
        int len = mHeader.getMessageLength();
        mHeader.setMessageLength(len + ba.length);
    }

    byte getByteParam() {
        return mMsg.get();
    }

    short getShortParam() {
        return mMsg.getShort();
    }

    int getIntParam() {
        return mMsg.getInt();
    }

    void getByteArrayParam(byte[] ba) {
        mMsg.get(ba);
    }

    private void prepareToSend() {
        addParam((short) 0); // TODO checksum
        setHeader(mHeader);
        mMsg.rewind();
    }

    // shall override to make response
    boolean prepareResponse() {
        return false;
    }

    public boolean sendAck() {
        MMPCtrlMsg ack = new MMPCtrlMsg(mHeader.getMessageCode());
        ack.setType(MMPConstants.MMP_TYPE_ACK);
        ack.prepareToSend();
        return MeemCore.getInstance().sendUsbMessage(ack.mMsg.array());
    }

    public boolean sendNack() {
        MMPCtrlMsg nack = new MMPCtrlMsg(mHeader.getMessageCode());
        nack.setType(MMPConstants.MMP_TYPE_NCK);
        nack.prepareToSend();
        return MeemCore.getInstance().sendUsbMessage(nack.mMsg.array());
    }

    public boolean send() {
        prepareToSend();

		/*
         * if(mHeader.getMessageCode() == MMPConstants.MMP_CODE_GET_SMART_DATA)
		 * { dbgDumpBuffer(100); }
		 */

        return MeemCore.getInstance().sendUsbMessage(mMsg.array());
    }

    public final ByteBuffer getBuffer() {
        addParam((short) 0);
        setHeader(mHeader);
        mMsg.rewind();
        return mMsg;
    }

    // ======================================================
    // ================= utility functions ==================
    // ======================================================

    public boolean isAck() {
        return (MMPConstants.MMP_TYPE_ACK == mHeader.getType());
    }

    public boolean isError() {
        byte type = mHeader.getType();
        return ((MMPConstants.MMP_TYPE_NCK == type) || (MMPConstants.MMP_TYPE_ERR == type));
    }

    public boolean isSuccess() {
        return (MMPConstants.MMP_TYPE_RES == mHeader.getType());
    }

    public void dbgDumpBuffer() {
        String strBuf = GenUtils.getHexString(this.mMsg.array());
        String dump = GenUtils.formatHexString(strBuf);

        int len = mHeader.getMessageLength();
        UiContext.getInstance().log(UiContext.DEBUG, "--- Length: " + String.valueOf(len) + " ---\n" + dump + "\n...\n");
    }
}
