package com.meem.v2.mmp;

import android.util.Log;

import com.meem.androidapp.ProductSpecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static com.meem.v2.mmp.MMPV2Constants.BLACKLISTED_PHONE_SLOWSPEED;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS;
import static com.meem.v2.mmp.MMPV2Constants.MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM;
import static com.meem.v2.mmp.MMPV2Constants.MMP_FULL_SPEED_DEVICE_CODE;

public class MMPV2 {
    public int mBufSize;
    public boolean mIsVirginCable;
    public String mFwVersion = "0.0.0.0";
    ByteBuffer mBuf;
    boolean mResult;
    int mMsgCode;

    /**
     * To be used to create a message to send to cable
     */
    public MMPV2(int bufSize) {
        mBufSize = bufSize;
    }

    /**
     * To be used to parse a response from cable
     *
     * @param msg
     */

    public MMPV2(ByteBuffer msg) {
        mBuf = msg;
        parseMsgHeader();
    }


    public ByteBuffer getInitSequence(boolean isSlowSpeed) {
        ByteBuffer initSeq = ByteBuffer.allocate(mBufSize);
        initSeq.order(ByteOrder.BIG_ENDIAN);

        initSeq.put(MMPV2Constants.MMP_INIT_SEQ_MAGIC_BYTES); // 8
        initSeq.putInt(MMPV2Constants.MMP_PLATFORM_CODE_ANDROID); // 12

        if (isSlowSpeed) {
            initSeq.putInt(BLACKLISTED_PHONE_SLOWSPEED);
        } else {
            initSeq.putInt(MMP_FULL_SPEED_DEVICE_CODE);
        }

        int lenOffset = initSeq.position();

        initSeq.putShort((short) 0); // total param len, filled later //

        initSeq.put((byte) 1); // hw version len, filled by fw
        initSeq.put((byte) 0); // hw version, filled by fw

        initSeq.put((byte) 1); // platform code len = 1
        initSeq.put((byte) 0); // platform code, reserved

        initSeq.put((byte) 1); // platform specific code len = 1
        initSeq.put((byte) 0); // platform specific code, reserved

        initSeq.put((byte) 7); // data-time len, always 7
        initSeq.putShort((short) 2017); // dummy year 2017
        initSeq.put((byte) 4); // dummy month
        initSeq.put((byte) 16); // dummy day
        initSeq.put((byte) 10); // dummy hour
        initSeq.put((byte) 10); // dummy minute
        initSeq.put((byte) 10); // dummy second

        initSeq.put((byte) 2); // buffer length param length
        initSeq.putShort((short) mBufSize);

        initSeq.put((byte) 6); // fw version length
        initSeq.put(MMPV2Constants.MMP_FW_VERSION_PLACEHOLDER_BYTES); // filled
        // by fw

        initSeq.put((byte) 1); // password status field length
        initSeq.put((byte) 0); // password status, filled by fw

        // fill the param length
        int curOffset = initSeq.position();
        initSeq.putShort(lenOffset, (short) (curOffset - lenOffset));

        return initSeq;
    }

    public ByteBuffer getMessageBuffer() {
        mBuf.rewind();
        return mBuf;
    }

    /**
     * USed while creating a message to send
     *
     * @param cmd
     * @param cmdCode
     */
    private void initMsgHeader(ByteBuffer cmd, int cmdCode) {
        cmd.rewind();

        cmd.putInt(MMPV2Constants.MMP_HEADER_MAGIC);
        cmd.putInt(0);
        cmd.put(MMPV2Constants.MMP_HDR_TYPE_CODE_COMMAND);
        cmd.putInt(cmdCode);
    }

    private void parseMsgHeader() {
        mBuf.rewind();

        int magic = mBuf.getInt();

        if ((MMPV2Constants.MMP_HEADER_MAGIC) != magic && (MMPV2Constants.MMP_MAGIC_2 != magic)) {
            throw new IllegalArgumentException("MMPV2 message header does not have magic signature");
        }

        int val = mBuf.getInt(); // reserved
        if (MMPV2Constants.MMP_INIT_SEQ == val || MMPV2Constants.MMP_INIT_SEQ_2 == val) {
            // This is a special case of init sequence.
            return;
        }

        byte type = mBuf.get();
        if (type == MMPV2Constants.MMP_HDR_TYPE_CODE_RESPONSE_SUCCESS) {
            mResult = true;
        } else if (type == MMPV2Constants.MMP_HDR_TYPE_CODE_RESPONSE_FAILED) {
            mResult = false;
        } else {
            // Arun: 18July2017: This happens for mode switch to desktop
            Log.e("MMPV2", "FW BUG: MMPV2 control message (response) header type is unknown: " + type);
            mResult = false;
        }

        mMsgCode = mBuf.getInt();
    }

    public boolean getResult() {
        return mResult;
    }

    public int getMsgCode() {
        return mMsgCode;
    }

    public ByteBuffer getSetPINMsg(String pin, LinkedHashMap<Integer, String> recoveryAnswers) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_SET_PIN);

        cmd.putShort((short) pin.length());
        cmd.put(pin.getBytes());

        // Arun: 05July2018: Added pin recovery mechanism
        cmd.put(ProductSpecs.PIN_RECOVERY_NUM_QUESTIONS);

        byte[] ansSrc;
        byte[] ansDst;

        // Answers
        for (byte idx = 1; idx <= 3; idx++) {
            cmd.put(idx);
            ansSrc = recoveryAnswers.get(Integer.valueOf(idx)).getBytes(); // Note: boxing is required.
            ansDst = Arrays.copyOf(ansSrc, 256);
            cmd.put(ansDst);
        }

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getAuthPhoneMsg(String pin) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_PIN_AUTH);

        cmd.putShort((short) pin.length());
        cmd.put(pin.getBytes());

        cmd.rewind();
        return cmd;
    }

    /*public ByteBuffer getCreateVaultMsg(String upid, String name, String memSize) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_CREATE_VAULT);

        int msgLenIdx = cmd.position();
        cmd.position(msgLenIdx + 1);

        cmd.put((byte) upid.length());
        cmd.put(upid.getBytes());

        cmd.put((byte) name.length());
        cmd.put(name.getBytes());

        // oper
        cmd.put((byte) "Idea".length());
        cmd.put("Idea".getBytes());

        // lang
        cmd.put((byte) "Eng".length());
        cmd.put("Eng".getBytes());

        // plat
        cmd.put((byte) "Android".length());
        cmd.put("Android".getBytes());

        // ver
        cmd.put((byte) Build.VERSION.RELEASE.length());
        cmd.put(Build.VERSION.RELEASE.getBytes());

        // brand
        cmd.put((byte) "dummy".length());
        cmd.put("dummy".getBytes());

        // model
        cmd.put((byte) "dummy".length());
        cmd.put("dummy".getBytes());

        // mem
        cmd.put((byte) memSize.length());
        cmd.put(memSize.getBytes());

        // update whole param length. useless.
        cmd.put(msgLenIdx, (byte) cmd.position());

        cmd.rewind();
        return cmd;
    }*/

   /* public ByteBuffer getSetVCFGMsg(String upid, String name) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_SET_VCFG);

        int msgLenIdx = cmd.position();
        cmd.position(msgLenIdx + 1);

        cmd.put((byte) upid.length());
        cmd.put(upid.getBytes());

        cmd.put((byte) name.length());
        cmd.put(name.getBytes());

        // migration?
        cmd.put((byte) 1);
        cmd.put((byte) 0);

        // backup mode?
        cmd.put((byte) 1);
        cmd.put((byte) 0);

        // auto backup enabled
        cmd.put((byte) 1);
        cmd.put((byte) 1);

        // sound - useless
        cmd.put((byte) 1);
        cmd.put((byte) 0);

        // num smart cats for sync
        cmd.put((byte) 3);
        cmd.put(MMPV2Constants.MMP_CATCODE_CONTACT);
        cmd.put(MMPV2Constants.MMP_CATCODE_MESSAGE);
        cmd.put(MMPV2Constants.MMP_CATCODE_CALENDER);

        // num gen cats for sync
        cmd.put((byte) 8);
        cmd.put(MMPV2Constants.MMP_CATCODE_PHOTO);
        cmd.put(MMPV2Constants.MMP_CATCODE_PHOTO_CAM);
        cmd.put(MMPV2Constants.MMP_CATCODE_VIDEO);
        cmd.put(MMPV2Constants.MMP_CATCODE_VIDEO_CAM);
        cmd.put(MMPV2Constants.MMP_CATCODE_MUSIC);
        cmd.put(MMPV2Constants.MMP_CATCODE_FILE);
        cmd.put(MMPV2Constants.MMP_CATCODE_DOCUMENTS);
        cmd.put(MMPV2Constants.MMP_CATCODE_DOCUMENTS_SD);

        // num smart cats for arch
        cmd.put((byte) 0);

        // num gen cats for arch
        cmd.put((byte) 0);

        // update whole param length. useless.
        cmd.put(msgLenIdx, (byte) cmd.position());

        cmd.rewind();
        return cmd;
    }*/

    public ByteBuffer getPCMeemModeMsg() {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_CHANGE_MODE);
        cmd.put(MMP_CODE_CHANGE_MODE_PARAM_PC_MEEM);

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getPCBypassModeMsg() {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_CHANGE_MODE);
        cmd.put(MMP_CODE_CHANGE_MODE_PARAM_PC_BYPASS);

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getCableResetMsg() {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_DELETE);

        short totLen = (short) (1 /* func type */);
        cmd.putShort(totLen);

        cmd.put(MMPV2Constants.MMP_CODE_DELETE_PARAM_TYPE_RESET);

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getDeleteVaultMsg(String upid) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_DELETE);

        short totLen = (short) (1 /* func type */ + 1 /* upid len */ + upid.getBytes().length);

        cmd.putShort(totLen);

        cmd.put(MMPV2Constants.MMP_CODE_DELETE_PARAM_TYPE_VAULT);
        cmd.put((byte) (upid.getBytes().length));
        cmd.put(upid.getBytes());

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getDeleteFileMsg(String upid, String meemPath, byte catCode, boolean isMirror) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_DELETE);

        short totLen = (short) (1 /* func type */ +
                1 /* upid len */ +
                upid.getBytes().length +
                1 /* meem path len */ +
                meemPath.getBytes().length +
                1 /* catCode */ +
                1 /* mirr or plus */);

        cmd.putShort(totLen);

        cmd.put(MMPV2Constants.MMP_CODE_DELETE_PARAM_TYPE_FILE);
        cmd.put((byte) (upid.getBytes().length));
        cmd.put(upid.getBytes());
        cmd.put((byte) meemPath.getBytes().length);
        cmd.put(meemPath.getBytes());
        cmd.put(catCode);
        cmd.put((byte) (isMirror ? 0 : 1));

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getCableCleanupMsg(String upid, byte catCode) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_CABLE_CLEANUP);

        cmd.put((byte) (upid.getBytes().length));
        cmd.put(upid.getBytes());
        cmd.put(catCode);

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getAppQuitMsg() {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_APP_QUIT); // 0x003ECCCD

        cmd.rewind();
        return cmd;
    }

    public ByteBuffer getRebootMsg() {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_REBOOT_CABLE);

        cmd.rewind();
        return cmd;
    }

    // Arun: 05July2018: This is same as phone auth message, but with dummy pin and answer params extra.
    public ByteBuffer getPinRecoveryMsg(LinkedHashMap<Integer, String> recoveryAnswers) {
        ByteBuffer cmd = ByteBuffer.allocate(mBufSize);
        cmd.order(ByteOrder.BIG_ENDIAN);

        initMsgHeader(cmd, MMPV2Constants.MMP_CODE_PIN_AUTH);

        // no pin, length is set as 0
        cmd.putShort((short) 0);

        cmd.put(ProductSpecs.PIN_RECOVERY_NUM_QUESTIONS);

        byte[] ansSrc;
        byte[] ansDst;

        // Answers
        for (byte idx = 1; idx <= 3; idx++) {
            cmd.put(idx);
            ansSrc = recoveryAnswers.get(Integer.valueOf(idx)).getBytes(); // Note: boxing is required.
            ansDst = Arrays.copyOf(ansSrc, 256);
            cmd.put(ansDst);
        }

        cmd.rewind();
        return cmd;
    }
}
