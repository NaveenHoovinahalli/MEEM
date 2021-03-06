package com.meem.events;

/**
 * Created by arun on 1/3/16.
 */
public enum EventCode {
    INVALID,
    MMP_RESPONSE_FROM_BACKEND,
    ALL_VAULT_STATUS_UPDATED,
    BACKUP_PREPARATION_FAILED,
    BACKUP_PREPARATION_SUCCEEDED,
    RESTORE_PREPARATION_FAILED,
    RESTORE_PREPARATION_SUCCEEDED,
    SESSION_XFR_STARTED,
    SESSION_PROGRESS_UPDATE,
    FILE_RECEIVED_FROM_MEEM,
    FILE_SENT_TO_MEEM,
    WWW_DOWNLOAD_COMPLETED,
    WWW_DOWNLOAD_FAILED,
    CRITICAL_ERROR,
    UPDATE_BACKUP_TIME,
    SMART_CONTACT_STATUS,
    SESSION_ABORT_BACKEND_ACK,
    MEEMCORE_DETECTED_CABLE_DISCONNECT,
    CABLE_PROBE_SUCCEEDED,

    // V2 stuff
    INIT_SEQ1_RECVD,
    INIT_SEQ2_RECVD,
    BACKUP_FINSIHED,
    RESTORE_FINISHED,
    MMP_CTRL_MSG_RECEIVED,
    FILE_SEND_SUCCEEDED,
    FILE_SEND_FAILED,
    FILE_RECEIVE_SUCCEEDED,
    FILE_RECEIVE_FAILED,

    UI_THREAD_EXECUTE_REQ,

    SESSION_PREP_COMMENTARY,
    SESSION_XFR_COMMENTARY,

    MNET_USER_REQ_SEARCH_MASTER,
    MNET_DISCOVERY_BCAST,
    MNET_CONNECT_REQUEST,
    MNET_REMOTE_ACCESSORY_CONNECTED,
    MNET_REMOTE_ACCESSORY_DISCONNECTED,

    MNET_CABLE_ACQUIRE_REQUEST,
    MNET_CABLE_RELEASE_REQUEST,

    MNET_REMOTE_CLIENT_HANDLER_STARTS,
    MNET_REMOTE_CLIENT_HANDLER_QUITS,
    MNET_START_AUTO_COPY,
    MNET_REMOTE_CLIENT_UI_REFRESH,
    MNET_CABLE_ACQUIRE_REQUEST_FROM_DESKTOP,
    MNET_CABLE_RELEASE_REQUEST_FROM_DESKTOP
}
