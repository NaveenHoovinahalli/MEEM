package com.meem.mmp.messages;

/**
 * @author Arun T A
 */

public class MMPCreateSession extends MMPCtrlMsg {

    /**
     * Backup or restore sessions
     *
     * @param umid
     * @param mmpCode
     */
    public MMPCreateSession(MMPUmid umid, int mmpCode) {
        super(mmpCode);

        if (mmpCode != MMPConstants.MMP_CODE_CREATE_BACKUP_SESSION && mmpCode != MMPConstants.MMP_CODE_CREATE_RESTORE_SESSION) {
            throw new IllegalArgumentException("Invalid session command code");
        }
        addParam(umid.asArray());
    }

    /**
     * Copy session
     *
     * @param upid
     * @param umid
     */
    public MMPCreateSession(MMPUpid upid, MMPUmid umid) {
        super(MMPConstants.MMP_CODE_CREATE_CPY_SESSION);
        addParam(upid.asArray());
        addParam(umid.asArray());
    }

    /**
     * Sync session
     *
     * @param upid
     * @param umid1
     * @param umid2
     */
    public MMPCreateSession(MMPUpid upid, MMPUmid umid1, MMPUmid umid2) {
        super(MMPConstants.MMP_CODE_CREATE_SYNC_SESSION);
        addParam(upid.asArray());
        addParam(umid1.asArray());
        addParam(umid2.asArray());
    }

    public MMPCreateSession(MMPCtrlMsg copy) {
        super(copy);
    }

    public byte getSessionHandle() {
        int msgType = getType();
        if (msgType == MMPConstants.MMP_TYPE_RES) {
            return getByteParam();
        } else {
            throw new IllegalArgumentException("Unexpected response code (not success)");
        }
    }
}
