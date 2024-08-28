package com.ericsson.cifwk.diagmon.agent.eclipseagent;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventService;

/**
 * @author EEICJON
 *
 */
public class Listener implements FrameworkListener, BundleListener,
        ServiceListener {

    /* (non-Javadoc)
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(FrameworkEvent evt) {
        String bundleSymbolicName = evt.getBundle().getSymbolicName();
        String evtType = "UNKNOWN";
        switch (evt.getType()) {
        case FrameworkEvent.ERROR:
            evtType = "ERROR"; break;
        case FrameworkEvent.INFO:
            evtType = "INFO"; break;
        case FrameworkEvent.PACKAGES_REFRESHED:
            evtType = "PACKAGES_REFRESHED"; break;
        case FrameworkEvent.STARTED:
            evtType = "STARTED"; break;
        case FrameworkEvent.STARTLEVEL_CHANGED:
            evtType = "STARTLEVEL_CHANGED"; break;
        case FrameworkEvent.WARNING:
            evtType = "WARNING"; break;
        }
        Logger.debug("eclipse.frameworkEvent: " + bundleSymbolicName + ";" + evtType);
        EventService.get().sendEvent(System.currentTimeMillis(),
                "eclipse.frameworkEvent:"  + bundleSymbolicName,
                "event from bundle " + bundleSymbolicName, evtType);
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent evt) {
        String bundleSymbolicName = evt.getBundle().getSymbolicName();
        String evtType = "UNKNOWN";
        switch (evt.getType()) {
        case BundleEvent.INSTALLED:
            evtType = "INSTALLED"; break;
        case BundleEvent.RESOLVED:
            evtType = "RESOLVED"; break;
        case BundleEvent.LAZY_ACTIVATION:
            evtType = "LAZY_ACTIVATION"; break;
        case BundleEvent.STARTING:
            evtType = "STARTING"; break;
        case BundleEvent.STARTED:
            evtType = "STARTED"; break;
        case BundleEvent.STOPPING:
            evtType = "STOPPING"; break;
        case BundleEvent.STOPPED:
            evtType = "STOPPED"; break;
        case BundleEvent.UPDATED:
            evtType = "UPDATED"; break;
        case BundleEvent.UNRESOLVED:
            evtType = "UNRESOLVED"; break;
        case BundleEvent.UNINSTALLED:
            evtType = "UNINSTALLED"; break;
        } 
        Logger.debug("eclipse.bundleChangedEvent: " + bundleSymbolicName + ";" + evtType);
        EventService.get().sendEvent(System.currentTimeMillis(),
                "eclipse.bundleChangedEvent:"  + bundleSymbolicName,
                "", evtType);
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent evt) {
        String bundleSymbolicName = evt.getServiceReference().getBundle().getSymbolicName();

        String evtType = "UNKNOWN";
        switch (evt.getType()) {
        case ServiceEvent.MODIFIED:
            evtType = "UPDATED"; break;
        case ServiceEvent.REGISTERED:
            evtType = "REGISTERED"; break;
        case ServiceEvent.UNREGISTERING:
            evtType = "UNREGISTERING"; break;            
        }
        Logger.debug("eclipse.serviceChangedEvent: " + bundleSymbolicName + ";" + evtType);
        EventService.get().sendEvent(System.currentTimeMillis(),
                "eclipse.serviceChangedEvent:"  + bundleSymbolicName,
                "", evtType);
    }
}
