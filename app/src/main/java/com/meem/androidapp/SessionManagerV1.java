package com.meem.androidapp;

import com.meem.businesslogic.Session;
import com.meem.businesslogic.SessionV1;
import com.meem.utils.DebugTracer;

/**
 * This is exactly same as SessionManager. Forked here as SessionManagerV1 to support the design for V2 app.
 * <p>
 * Created by arun on 25/5/17.
 */

public class SessionManagerV1 extends SessionManager {
    private static final String TAG = "SessionManagerV1";
    protected DebugTracer mDbg = new DebugTracer(TAG, "SessionManagerV1.log");

    public SessionManagerV1(String phoneUpid, SessionManagementHelper helper) {
        super(phoneUpid, helper);
    }

    @Override
    protected Session createSession(int sessionType, Session.SessionStatUpdateHelper statHelper) {
        return new SessionV1(sessionType, statHelper);
    }
}
