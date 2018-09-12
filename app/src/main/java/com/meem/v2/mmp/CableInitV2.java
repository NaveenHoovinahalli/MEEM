package com.meem.v2.mmp;

import com.meem.androidapp.ProductSpecs;
import com.meem.events.ResponseCallback;
import com.meem.utils.DebugTracer;
import com.meem.utils.GenUtils;
import com.meem.v2.core.MeemCoreV2;

import java.nio.ByteBuffer;

/**
 * Created by arun on 17/5/17.
 */

public class CableInitV2 extends MMPV2CtrlMsg {
    private boolean mIsSlowSpeedMode;

    private DebugTracer mDbg = new DebugTracer("CableInitV2", "CableInitV2.log");

    public CableInitV2(boolean isSlowSpeedMode, ResponseCallback responseCallback) {
        super("CableInitV2", MMPV2Constants.MMP_CODE_INTERNAL_CABLE_INIT, responseCallback);
        mIsSlowSpeedMode = isSlowSpeedMode;
    }

    @Override
    protected boolean kickStart() {
        if (!super.kickStart()) {
            return false;
        }

        MeemCoreV2 core = MeemCoreV2.getInstance();

        MMPV2 mmpv2 = new MMPV2(core.getPayloadSize());
        ByteBuffer initSeq = mmpv2.getInitSequence(mIsSlowSpeedMode);

        return core.sendUsbMessage(initSeq, mName);
    }

    @Override
    protected boolean onMMPMessage(MMPV2 msg) {
        // must not call super as this is not a typical control message handler!

         /*
         [00] 6D 65 65 6D 3E E3 3E E3 <- legacy init seq1
         [08] AA AA 3E E3 <- android plat
         [12] 00 00 00 00 <- high speed
         [16] 00 3F <- remaining len
         [18] 01 02 <- version diff
         [20] 01 00 <- plat code
         [22] 01 00 <- plat spl code
         [24] 07 07 E1 04 10 0A 0A 0A <- date
         [32] 02 80 00 <- buff len
         [35] 06 02 02 00 01 00 01 <- fw version
         [42] 01 10 <- passwd status
         [44] 12 01 01 0F 33 35 34 30 31 30 30 38 32 37 37 31 32 31 34 <- vault info #1
         00 00 00 00 00 0...
        */

        ByteBuffer buf = msg.getMessageBuffer();
        GenUtils.saveByteArray(buf.array(), "InitSequence.log");

        int major = buf.get(MMPV2Constants.INIT_SEQ_FW_VER_OFFSET);
        int minor = buf.get(MMPV2Constants.INIT_SEQ_FW_VER_OFFSET + 1);
        int build = buf.getShort(MMPV2Constants.INIT_SEQ_FW_VER_OFFSET + 2);
        int trial = buf.getShort(MMPV2Constants.INIT_SEQ_FW_VER_OFFSET + 4);

        String fwVersion = String.valueOf(major) + "." +
                String.valueOf(minor) + "." +
                String.valueOf(build) + "." +
                String.valueOf(trial);

        byte pinStatus = buf.get(MMPV2Constants.INIT_SEQ_PIN_STATUS_OFFSET);
        String serialStr = "MEEM";
        int dbStatus = 0;
        String delPendingUpid = "";

        try {
            // get the serial number. essentially, take the remaining length param,
            // add it to its offset to reach the serial number.
            short remainLen = buf.getShort(MMPV2Constants.INIT_SEQ_REMIAN_LEN_OFFSET);
            // mDbg.trace("Remaining length: " + remainLen);

            byte[] serialBytes = new byte[ProductSpecs.INEDA_CABLE_SERIAL_LEN];

            buf.position(MMPV2Constants.INIT_SEQ_REMIAN_LEN_OFFSET + 2 + remainLen);
            buf.get(serialBytes, 0, ProductSpecs.INEDA_CABLE_SERIAL_LEN);

            serialStr = new String(serialBytes);
            mDbg.trace("Serial no: " + serialStr);

            // Arun: 20Aug2018: Cable Disconnect in between DeleteVault may cause orphan entries in DB in cable.
            // Check for it - Added this flagging in FW by Barath - check mail on same date.
            // mDbg.trace("Pos for db status: " + buf.position());
            dbStatus = buf.getInt();
            mDbg.trace("Firmware db status: " + Integer.toHexString(dbStatus));

            if (ProductSpecs.FIRMWARE_FLAG_DB_CORRUPT_ON_DELETEVAULT == dbStatus) {
                byte upidLen = buf.get();
                byte[] upidBytes = new byte[upidLen];

                buf.get(upidBytes, 0, upidLen);
                delPendingUpid = new String(upidBytes);

                mDbg.trace("upidLen: " + upidLen + " Vault delete incomplete for upid: " + delPendingUpid);
            }

        } catch (Exception e) {
            mDbg.trace("Exception while getting serial no: " + e.getMessage());
        }

        InitSeqResponseParams params = new InitSeqResponseParams();
        params.pinStatus = pinStatus;
        params.fwVersion = fwVersion;
        params.serialNo = serialStr;
        params.dbStatus = dbStatus;
        params.delPendingUpid = delPendingUpid;

        postResult(true, 0, params, null);

        return false;
    }

    public class InitSeqResponseParams {
        public byte pinStatus;
        public String fwVersion;
        public String serialNo;
        public int dbStatus;
        public String delPendingUpid;
    }
}
