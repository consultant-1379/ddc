package com.ericsson.cifwk.diagmon.util.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.omg.CORBA.Any;
import org.omg.TimeBase.UtcT;
import org.omg.TimeBase.UtcTHelper;

public class AnyPrinter
{
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(AnyPrinter.class);

    private static DateFormat m_DF;
    private static final long EPOCH_OFFSET = 12219292800000L;

    private AnyPrinter() {}

    public static void init() {
        m_DF = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
        m_DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String toString( final Any any ) //throws Exception
    {
        if ( m_Log.isDebugEnabled() ) { m_Log.debug("toString any.type()==" + any.type() + ", UtcTHelper.type()=" + UtcTHelper.type()); }
        // CORBA Time
        if ( any.type().equivalent(UtcTHelper.type()) ) {
            final UtcT time = UtcTHelper.extract(any);
            if ( time.time == 0 ) {
                return "EPOCH";
            } else {
                final long unixTimeMilli = (time.time / 10000L) - EPOCH_OFFSET;

                if ( m_Log.isDebugEnabled() ) {
                    m_Log.debug("toString UtcT time=" + time.time + " inacclo=" + time.inacclo +
                                " EPOCH_OFFSET=" + EPOCH_OFFSET + " unixTimeMilli=" + unixTimeMilli);
                }
                return m_DF.format(new Date(unixTimeMilli));
            }
        } else {
            return any.toString();
        }
    }
}

