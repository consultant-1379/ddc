package com.ericsson.cifwk.diagmon.util.notif;


import com.ericsson.cifwk.diagmon.util.common.IorReader;
import com.ericsson.nms.umts.ranos.cms.nead.segmentserver.neaccess.cello_p1.idl.ConfigExtended.*;
import com.ericsson.nms.umts.ranos.cms.nead.segmentserver.neaccess.cello_p1.idl.ConfigExtended.NotificationProducerPackage.NotificationFilter;
import com.inprise.vbroker.IIOP.ProfileBodyValue;
import com.inprise.vbroker.IOP.ComponentValue;
import com.borland.security.csiv2.SSLComponentValue;
import com.inprise.vbroker.IOP.ProfileValue;
import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.Messaging.NO_RECONNECT;
import org.omg.Messaging.REBIND_POLICY_TYPE;
import org.omg.Messaging.RebindModeHelper;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.SSLIOP.TAG_SSL_SEC_TRANS;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class CelloConfigConsumer implements IConsumer, NotificationConsumerOperations {
    private final org.apache.logging.log4j.Logger m_Log;
    private static final int TICK_TIME = 3 * 60; // Allow the subscription to survive 3mins
    private static final int NO_SUB = -1;

    private final String m_CelloHost;
    private final INotificationHandler m_NotifHandler;
    private final PrintStream m_PrintOut;
    private final SubPinger m_Pinger;
    private final String m_MoFilter[];

    private NotificationProducer m_CelloNotif;
    private int m_SubId = NO_SUB;

    private final int m_PingId;
    private final ORB orb;

    private static POA cpp_poa;

    private final SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");
    private NotificationConsumer consumerRef;

    private static final Integer TAG_SECIOP_SEC_TRANS = 35;
    private static final List<Integer> SECURITY_TAGS = Arrays.asList(TAG_SSL_SEC_TRANS.value, TAG_TLS_SEC_TRANS.value, TAG_SECIOP_SEC_TRANS);

    public CelloConfigConsumer(final PrintStream printOut, final INotificationHandler notifHandler, final String celloHost,
                               final SubPinger pinger, final String moFilter[],
                               final ORB orb) {
        m_Log = org.apache.logging.log4j.LogManager.getLogger(CelloConfigConsumer.class.getName() + "[" + celloHost + "]");
        m_PrintOut = printOut;
        m_NotifHandler = notifHandler;

        m_CelloHost = celloHost;
        m_Pinger = pinger;

        m_MoFilter = moFilter;
        this.orb = orb;

        ping();

        m_PingId = m_Pinger.add(this);
    }

    public void push(final ConfigNotification[] notifications) {
        m_NotifHandler.handleConfigNotifications(notifications, m_CelloHost);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void detach() throws Exception {
        m_Pinger.remove(m_PingId);

        if (m_CelloNotif != null && m_SubId != NO_SUB) {
            m_PrintOut.println(m_DF.format(new Date()) + " INFO: Unsubscribing subscription id = " + m_SubId);

            m_CelloNotif.unsubscribe(m_SubId);
            m_CelloNotif = null;
            m_SubId = NO_SUB;
        }
    }

    final public void ping() {
        m_Log.debug("ping(): m_CelloNotif valid = " + (m_CelloNotif != null) + ", m_SubId = " + m_SubId);

        final boolean reconnecting = (m_CelloNotif == null) && (m_SubId != NO_SUB);

        if (m_CelloNotif == null) {
            try {
                final String celloNamingIor = getNeNamingIOR();
                getNeNotif(celloNamingIor);
                m_PrintOut.println(m_DF.format(new Date()) + " INFO: Resolved Notification Producer");
            } catch (Exception e) {
                m_PrintOut.println(m_DF.format(new Date()) + " ERROR: Ping Failed, Cannot resolve Notification Producer: " + e);
                return;
            }
        }


        if (m_SubId == NO_SUB) {
            try {
                subscribe();
            } catch (Exception e) {
                m_PrintOut.println(m_DF.format(new Date()) + " ERROR: Ping Failed, Cannot establish subscription : " + e);
                m_Log.debug("ping()", e);
                return;
            }
        } else {
            boolean subActive = false;
            try {
                subActive = m_CelloNotif.get_subscription_status(m_SubId);

                if ( ! subActive ) {
                    m_PrintOut.println(m_DF.format(new Date()) + " ERROR: Ping Failed for subscription id " + m_SubId + " not active");
                    m_SubId = NO_SUB;
                } else if (reconnecting) {
                    m_PrintOut.println(m_DF.format(new Date()) + " INFO: Reconnected, subscription still active, id  = " + m_SubId);
                }
            } catch (Exception e) {
                m_PrintOut.println(m_DF.format(new Date()) + " ERROR: Ping Failed, Cannot get_subscription_status: " + e);
                m_CelloNotif = null;
            }
        }

    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private String getNeNamingIOR() throws Exception {
        return IorReader.getInstance().getIOR(m_CelloHost, 5000);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void getNeNotif(final String celloNamingIor) throws Exception {
        m_Log.debug("getNeNotif()");

        final org.omg.CORBA.Object obj = this.orb.string_to_object(celloNamingIor);
        if ( m_Log.isDebugEnabled() ) {
            final com.inprise.vbroker.CORBA.Object vobj = (com.inprise.vbroker.CORBA.Object)obj;
            m_Log.debug("getNeNotif() ns location=" + getLocation(vobj) + ", isSecure=" + isSecure(vobj));
        }

        final NamingContext celloNaming = NamingContextHelper.narrow(obj);

        final NameComponent celloCSname[] = {new NameComponent("CelloConfigurationService", "")};
        final ConfigurationExtended celloCS =
                ConfigurationExtendedHelper.narrow(celloNaming.resolve(celloCSname));
        if ( m_Log.isDebugEnabled() ) { m_Log.debug("getNeNotif celloCS location=" + getLocation(celloCS) + ", isSecure=" + isSecure(celloCS)); }
        final Any policyValue = this.orb.create_any();
        RebindModeHelper.insert(policyValue, NO_RECONNECT.value);
        final Policy myRebindPolicy = this.orb.create_policy(REBIND_POLICY_TYPE.value, policyValue);
        final org.omg.CORBA.Object newObjRef =
                celloCS.get_notification_producer()._set_policy_override(new Policy[]{myRebindPolicy},
                        SetOverrideType.SET_OVERRIDE);

        m_CelloNotif = NotificationProducerHelper.narrow(newObjRef);
        if ( m_Log.isDebugEnabled() ) { m_Log.debug("getNeNotif m_CelloNotif isSecure=" + isSecure(m_CelloNotif)); }
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void subscribe() throws Exception {
        m_Log.debug("subscribe():");

        if ( consumerRef == null ) {
            final POA poa = getCppPoa(this.orb);
            m_Log.debug("Creating NotificationConsumerPOATie");
            final NotificationConsumerPOATie tie = new NotificationConsumerPOATie(this);
            m_Log.debug("Activating NotificationConsumerPOATie");
            poa.activate_object(tie);
            m_Log.debug("Getting reference");
            consumerRef = NotificationConsumerHelper.narrow(poa.servant_to_reference(tie));

            if (m_Log.isDebugEnabled()) {
                m_Log.debug("subscribe: consumerRef IOR " + this.orb.object_to_string(consumerRef));
                m_Log.debug("subscribe: consumerRef location " + getLocation(consumerRef));
            }
        }


        final IntHolder id = new IntHolder(0);
        final NotificationFilter filter =
                new NotificationFilter(m_MoFilter,
                        true, // TopologyNotifications
                        true, // AVCNotifications
                        "",   // rootMO acts as baseMO
                        -1);  // complete subtree
        m_Log.debug("subscribe: calling subscribe");
        m_CelloNotif.subscribe(consumerRef, filter, TICK_TIME, id);
        m_SubId = id.value;

        m_PrintOut.println(m_DF.format(new Date()) + " INFO: Subscription Successfull, subscription id = " + m_SubId);
    }

    private static synchronized POA getCppPoa(final ORB orb) throws AdapterInactive, InvalidName,
            AdapterAlreadyExists, InvalidPolicy, PolicyError {
        if ( cpp_poa == null ) {
            final org.omg.PortableServer.POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();

            final Any seAny = orb.create_any();
            StringSequenceHelper.insert(seAny, new String[]{"cpp_se"});
            final Policy[] policies = {
                    rootPOA.create_lifespan_policy(LifespanPolicyValue.PERSISTENT),
                    orb.create_policy(com.inprise.vbroker.PortableServerExt.SERVER_ENGINE_POLICY_TYPE.value, seAny)
            };
            // Create myPOA with the right policies
            cpp_poa = rootPOA.create_POA("cpp_poa", rootPOA.the_POAManager(), policies);
        }
        return cpp_poa;
    }

    private static String getLocation(final com.inprise.vbroker.CORBA.Object vObj) {
      String iorInfo = "";

        for (ProfileValue profile : vObj._ior_value().profiles) {
            if (profile instanceof com.inprise.vbroker.IIOP.ProfileBodyValue) {
                 final com.inprise.vbroker.IIOP.ProfileBodyValue iiopPro =
                            (com.inprise.vbroker.IIOP.ProfileBodyValue)profile;
                iorInfo = iiopPro.host + ":" + iiopPro.port;

                for (ComponentValue component : ((ProfileBodyValue) profile).components) {
                    System.out.println(component);
                    if ( component instanceof SSLComponentValue ) {
                        iorInfo = iiopPro.host + ":" + ((SSLComponentValue) component).port();
                    }
                }
            }
        }
      return iorInfo;
   }

   private static boolean isSecure(final com.inprise.vbroker.CORBA.Object vObj) {
       for (ProfileValue profile : vObj._ior_value().profiles) {
           if (profile instanceof com.inprise.vbroker.IIOP.ProfileBodyValue) {
               final ComponentValue[] components = ((ProfileBodyValue) profile).components;
               for (ComponentValue component : components) {
                   if (SECURITY_TAGS.contains(component.tag())) {
                       return true;
                   }
               }
           }
       }
       return false;
   }
}

							 

