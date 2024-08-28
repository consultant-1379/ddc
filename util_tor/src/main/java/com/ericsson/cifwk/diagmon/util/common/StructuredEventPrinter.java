package com.ericsson.cifwk.diagmon.util.common;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

public class StructuredEventPrinter
{
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(StructuredEventPrinter.class);
    private static SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");

    private StructuredEventPrinter() {}

    public static void init() {
        AnyPrinter.init();
    }

    public static void printEvent( final PrintStream output,
                                   final org.omg.CosNotification.StructuredEvent se, final String source, final long timeStamp ) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream printOut = new PrintStream(baos);
        try
            {
                if ( timeStamp != 0 ) {
                    String timeStampStr = null;
                    synchronized ( m_DF ) {
                        timeStampStr = m_DF.format(new Date(timeStamp));
                    }
                    printOut.print(timeStampStr + " ");
                }

                final String channel  = se.header.fixed_header.event_type.domain_name;
                final String typeName = se.header.fixed_header.event_type.type_name;

                printOut.println( source + " " +
                                  channel + " " +
                                  typeName + " " +
                                  se.header.fixed_header.event_name);

                for ( int i = 0; i < se.filterable_data.length; i++ ) {
                    if ( m_Log.isDebugEnabled() ) {
                        m_Log.debug("printEvent: name=" + se.filterable_data[i].name +
                                    ", value=" + se.filterable_data[i].value);
                    }
                    printOut.println("\t" + se.filterable_data[i].name + "=" +
                                     AnyPrinter.toString(se.filterable_data[i].value));
                }


                printOut.println();
                output.print(baos.toString());
            }
        catch (Throwable t) { //NOPMD
            output.println(t);
            t.printStackTrace(output);
        }
    }
}
