package com.ericsson.cifwk.diagmon.agent.common;

import java.text.SimpleDateFormat;

public class Time {
    private static SimpleDateFormat m_DF =
        new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");

    public static String getTimestamp() {
        return getTimestamp(System.currentTimeMillis());
    }

    public static String getTimestamp(long ts) {
        return m_DF.format(new java.util.Date(ts));
    }
}
