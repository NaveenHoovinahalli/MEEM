package com.meem.androidapp;

import android.content.Context;

import com.meem.events.EventCode;
import com.meem.events.MeemEvent;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPSessionStatusInfo;
import com.meem.utils.DebugTracer;

/**
 * Single instance of this class is kept by MainActivity (UI thread) and all methods of this class are always called from UI thread context.
 * Note that the event handler functions' return code (boolean) does not have any particular meaning as of now.
 * <p/>
 * IMPORTANT: You must have a good understanding of MEEM message flows as well as application architecture to modify this file. Random
 * changes will have unpleasant side effects.
 *
 * @author Arun T A
 */
public class EventHandler {
    DebugTracer mDbg = new DebugTracer("EventHandler", "EventHandler.log");
    private MainActivity mActivity;

    public EventHandler(MainActivity activity, Context context) {
        mActivity = activity;
    }

    public boolean handleEvent(MeemEvent event) {
        boolean ret = true;

        if (event == null) {
            mDbg.trace("BUG: Received null event");
            return false;
        }

        boolean result = event.getResult();
        Object info = event.getInfo();
        Object extraInfo = event.getExtraInfo();
        ResponseCallback callback = event.getResponseCallback();

        if (callback != null) {
            ret = callback.execute(result, info, extraInfo);
        } else {
            ret = handleLegacyEvent(event);
        }

        return ret;
    }

    private boolean handleLegacyEvent(MeemEvent event) {
        boolean ret = true;

        EventCode eventCode = event.getCode();
        // mDbg.trace("Received legacy event: " + eventCode);

        boolean result = event.getResult();
        Object info = event.getInfo();
        Object extraInfo = event.getExtraInfo();

        switch (eventCode) {
            case BACKUP_PREPARATION_SUCCEEDED:
            case RESTORE_PREPARATION_SUCCEEDED:
                mActivity.onSessionPrepCompletedEvent(true);
                break;
            case BACKUP_PREPARATION_FAILED:
            case RESTORE_PREPARATION_FAILED:
                mActivity.onSessionPrepCompletedEvent(false);
                ret = false;
                break;
            case SESSION_PROGRESS_UPDATE:
                mActivity.onSessionProgressUpdateEvent((MMPSessionStatusInfo) info);
                break;
            case SESSION_XFR_STARTED:
                mActivity.onSessionXfrStartedEvent();
                break;
            case FILE_RECEIVED_FROM_MEEM:
                mActivity.onFileReceivedEvent((String) event.getInfo());
                break;
            case FILE_SENT_TO_MEEM:
                mActivity.onFileSentEvent((String) event.getInfo());
                break;
            case CRITICAL_ERROR:
                mActivity.onCriticalError("Event: " + event.getInfo());
                ret = false;
                break;
            case MEEMCORE_DETECTED_CABLE_DISCONNECT:
                mActivity.onAccessoryDisconnected(null);
                break;
            case MMP_CTRL_MSG_RECEIVED:
                mDbg.trace("Unhandled dummy event (should not see this!): " + eventCode);
                break;
            case SESSION_PREP_COMMENTARY:
                mActivity.onSessionPrepCommentary(event.getArg0(), event.getArg1(), (Byte) info, (Byte) extraInfo);
                break;
            case SESSION_XFR_COMMENTARY:
                mActivity.onSessionXfrCommentary(event.getArg0(), event.getArg1(), (Byte) info, (Byte) extraInfo);
                break;
            case MNET_REMOTE_ACCESSORY_CONNECTED:
                mActivity.onAccessoryConnected((AccessoryInterface) event.getInfo());
                break;
            case MNET_USER_REQ_SEARCH_MASTER:
                mActivity.onMeemNetworkServerSearchRequest();
                break;
            case MNET_CABLE_ACQUIRE_REQUEST:
                mActivity.onCableAcquireRequest((String) event.getInfo());
                break;
            case MNET_CABLE_RELEASE_REQUEST:
                mActivity.onCableReleaseRequest((String) event.getInfo());
                break;
            case MNET_CABLE_ACQUIRE_REQUEST_FROM_DESKTOP:
                mActivity.OnCableAcquireReqFromDesktop();
                break;
            case MNET_CABLE_RELEASE_REQUEST_FROM_DESKTOP:
                mActivity.OnCableReleaseReqFromDesktop();
                break;
            case MNET_REMOTE_CLIENT_HANDLER_STARTS:
                mActivity.onRemoteClientStart((String) event.getInfo());
                break;
            case MNET_REMOTE_CLIENT_HANDLER_QUITS:
                mActivity.onRemoteClientQuit((String) event.getInfo());
                break;
            case MNET_START_AUTO_COPY:
                mActivity.onStartAutoCopyNotification((String) event.getInfo());
                break;
            case MNET_REMOTE_CLIENT_UI_REFRESH:
                mActivity.onUiRefreshNotification((String) event.getInfo());
                break;
            default:
                mDbg.trace("Unhandled legacy event: " + eventCode);
                break;
        }

        return ret;
    }
}
