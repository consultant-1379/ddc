package com.ericsson.cifwk.diagmon.util.notif;

import java.io.PrintStream;

import com.ericsson.cifwk.diagmon.util.common.StructuredEventPrinter;
import com.ericsson.nms.umts.ranos.cms.nead.segmentserver.neaccess.cello_p1.idl.ConfigExtended.ConfigNotification;

public class NotifHandler implements INotificationHandler {
    protected final PrintStream m_PrintOut;

    public NotifHandler( final PrintStream printOut) {
        m_PrintOut = printOut;
    }

    public void handleStructuredEvent( final org.omg.CosNotification.StructuredEvent se, final String source )  {
        final long timeStamp = System.currentTimeMillis();
        StructuredEventPrinter.printEvent( m_PrintOut, se, source, timeStamp );
    }

    @Override
    public void handleConfigNotifications(final ConfigNotification[] notifications, final String source) {
        final long timeStamp = System.currentTimeMillis();
        for ( int i = 0; i < notifications.length; i++ ) {
            ConfigNotifPrinter.printEvent(m_PrintOut, notifications[i], source, timeStamp);
        }

    }

    protected void close() {}
}

