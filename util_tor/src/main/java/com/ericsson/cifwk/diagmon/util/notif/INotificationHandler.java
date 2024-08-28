package com.ericsson.cifwk.diagmon.util.notif;

import com.ericsson.nms.umts.ranos.cms.nead.segmentserver.neaccess.cello_p1.idl.ConfigExtended.ConfigNotification;

public interface INotificationHandler {
    void handleStructuredEvent( final org.omg.CosNotification.StructuredEvent se, final String source );
    void handleConfigNotifications( ConfigNotification[] notifications, String source );
}
