package com.ericsson.cifwk.diagmon.util.notif;


import java.io.PrintStream;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.StructuredEvent;
import org.omg.PortableServer.POAHelper;

import com.ericsson.irp.CosEventComm.Disconnected;
import com.ericsson.irp.CosNotifyComm.InvalidEventType;
import com.ericsson.irp.CosNotifyComm.SequencePushConsumer;
import com.ericsson.irp.CosNotifyComm.SequencePushConsumerHelper;
import com.ericsson.irp.CosNotifyComm.SequencePushConsumerOperations;
import com.ericsson.irp.CosNotifyComm.SequencePushConsumerPOATie;
import com.ericsson.irp.NotificationIRPConstDefs.SubscriptionStateHolder;
import com.ericsson.irp.NotificationIRPSystem.InvalidParameter;
import com.ericsson.irp.NotificationIRPSystem._NotificationIRPOperations;
import com.ericsson.irp.NotificationIRPSystem._NotificationIRPOperationsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreegppConsumer implements SequencePushConsumerOperations, IConsumer {
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(ThreegppConsumer.class);

    static final NameComponent NMSNA_IFACE_NAME[] =
    {
        new NameComponent("com",""),
        new NameComponent("ericsson",""),
        new NameComponent("nms",""),
        new NameComponent("cif",""),
        new NameComponent("service",""),
        new NameComponent("NMSNAConsumer","")
    };

    private static final SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");
    private static final long m_SleepTime = Integer.parseInt(System.getProperty("com.ericsson.nms.umts.ranos.util.notif.sleep", "0")) * 1000;

    protected final String m_Cat[] = new String[1];
    private final String m_Filter;

    private final INotificationHandler m_NotifHandler;
    private final PrintStream m_PrintOut;
    private final SubPinger m_Pinger;
    final int m_PingId;

    private static final int MAX_SUBSCRIBE_ATTEMPTS = 3;
    private final static int TICK_VALUE = 25;

    private String m_SubID;

    private int m_SubscribeAttempts = 0;
    private final ORB orb;

    _NotificationIRPOperations naIRP;
    private SequencePushConsumer m_SPC;

    public ThreegppConsumer( final PrintStream printOut, final INotificationHandler notifHandler, final String cat,
                             final String filter, final SubPinger pinger, final ORB orb )
            throws Exception { //NOPMD
        m_PrintOut = printOut;
        m_NotifHandler = notifHandler;
        m_Cat[0] = cat;
        m_Filter = filter;
        m_Pinger = pinger;
        m_PingId = m_Pinger.add(this);
        this.orb = orb;

        if ( m_SleepTime > 0 ) {
            printMessage("WARN", "SleepTime active for " + m_Cat[0] + ": m_SleepTime = " + m_SleepTime);
        }

        subscribe();
    }

    public void detach()
            throws Exception { //NOPMD
        if ( m_SubID != null ) {
            naIRP.detach(m_SPC, m_SubID);
            m_Pinger.remove(m_PingId);
            m_SubID = null;
        }
    }

    public void ping() {
        if ( m_SubID == null ) {
            subscribe();
        } else {
            try {
                getNaHelper();
                final StringHolder stringHolder = new StringHolder();
                final IntHolder intHolder = new IntHolder();
                final SubscriptionStateHolder stateHolder = new SubscriptionStateHolder();
                naIRP.get_subscription_status( m_SubID, stringHolder, stateHolder, intHolder );
            } catch ( InvalidParameter ip ) {
                printMessage("WARN", "subscription invalid for " + m_Cat[0] + ": subId = " + m_SubID + " " + ip.toString() );
                m_SubID = null;
                subscribe();
            } catch ( Throwable t ) {//NOPMD
                printMessage("ERROR", "get_subscription_status failed for " + m_Cat[0] + ": subId = " + m_SubID + " " + t.toString() );
                naIRP = null;
            }
        }
    }

    protected final _NotificationIRPOperations getNaHelper()
            throws Exception //NOPMD
    {
        if ( naIRP == null ) {
            final org.omg.CORBA.Object namingRootObj = orb.string_to_object("corbaloc::visinamingnb-pub:9951/NameService");
            final NamingContext namingCtx = NamingContextHelper.narrow(namingRootObj);
            final org.omg.CORBA.Object nmsNAObj = namingCtx.resolve(NMSNA_IFACE_NAME);
            naIRP = _NotificationIRPOperationsHelper.narrow(nmsNAObj);
            printMessage("INFO", "connected to NotificationAgent cat:" + m_Cat[0]);
        }

        return naIRP;
    }

    private void subscribe() {
        if ( m_SubscribeAttempts >= MAX_SUBSCRIBE_ATTEMPTS ) {
            return;
        } else {
            m_SubscribeAttempts++;
        }

        try {
            getNaHelper();

            final SequencePushConsumerPOATie tie = new SequencePushConsumerPOATie(this);
            final org.omg.PortableServer.POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();

            rootPOA.activate_object(tie);
            m_SPC = SequencePushConsumerHelper.narrow(rootPOA.servant_to_reference(tie));

            m_SubID = naIRP.attach_push( m_SPC, TICK_VALUE, m_Cat, m_Filter );

            printMessage("INFO", "subscribe successfull for " + m_Cat[0] + ": subId = " + m_SubID);
        } catch ( Exception e ) {
            m_Log.debug("subscribe failed",e);

            naIRP = null;
            printMessage("ERROR", "subscribe failed for " + m_Cat[0] + ": " + e.toString());

            if ( m_SubscribeAttempts == MAX_SUBSCRIBE_ATTEMPTS ) {
                printMessage("ERROR", "MAX_SUBSCRIBE_ATTEMPTS reached for " + m_Cat[0]);
            }
        }
    }

    protected final void printMessage( final String type, final String message ) {
        synchronized(m_PrintOut) {
            m_PrintOut.println(m_DF.format(new Date()) + " " + type + " " + message );
            m_PrintOut.println();
        }
    }

    public String toString()
    {
        return "NA : cat = " + m_Cat[0] + " m_SubID = " + m_SubID;
    }


    @Override
    public void offer_change(final EventType[] arg0, final EventType[] arg1) throws InvalidEventType {}

    @Override
    public void disconnect_sequence_push_consumer() {}

    @Override
    public void push_structured_events(final StructuredEvent[] seArray) throws Disconnected {
        for ( int i = 0; i < seArray.length; i++ ) {
            //StructuredEventPrinter.printEvent( seArray[i] );
            m_NotifHandler.handleStructuredEvent(seArray[i], "ENM");
        }
    }
}

