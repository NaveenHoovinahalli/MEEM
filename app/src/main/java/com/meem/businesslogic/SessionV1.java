package com.meem.businesslogic;

import com.meem.utils.GenUtils;

/**
 * This is exactly same as original session. Forked as V1 for V2 design.
 * <p>
 * Created by arun on 25/5/17.
 */

public class SessionV1 extends Session {
    private static final String tag = "SessionV1";

    public SessionV1(int mmpSessionType, SessionStatUpdateHelper statHelper) {
        super(mmpSessionType, statHelper);
    }

    private void dbgTrace(String trace) {
        GenUtils.logCat(tag, trace);
        GenUtils.logMessageToFile("SessionV1.log", trace);
    }

    private void dbgTrace() {
        GenUtils.logMethodToFile("SessionV1.log");
    }
}
