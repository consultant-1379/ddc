/*------------------------------------------------------------------------------
*******************************************************************************
* COPYRIGHT Ericsson 2012
*
* The copyright to the computer program(s) herein is the property of
* Ericsson Inc. The programs may be used and/or copied only with written
* permission from Ericsson Inc. or in accordance with the terms and
* conditions stipulated in the agreement/contract under which the
* program(s) have been supplied.
*******************************************************************************
*----------------------------------------------------------------------------*/

package com.ericsson.cifwk.diagmon.e2e;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.Monitor;



public class JvmMonitor {
    private static final org.apache.logging.log4j.Logger message_log = org.apache.logging.log4j.LogManager.getLogger(JvmMonitor.class);

    public static Map<String, String> getJvms() {
        final Map<String, String> results = new HashMap<String, String>();
        try {
            final MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(new HostIdentifier((String) null));
            for (final Iterator<?> vmIterator = monitoredHost.activeVms().iterator(); vmIterator.hasNext();) {
                final int vmIdInt = (Integer) vmIterator.next();
                message_log.debug("getJvms: id=" + vmIdInt);

                try {
                    final VmIdentifier vmId = new VmIdentifier("//" + vmIdInt + "?mode=r");
                    message_log.debug("vmId: " + vmId);

                    final MonitoredVm monitoredVm = monitoredHost.getMonitoredVm(vmId, 0);
                    results.put(getVmCommand(monitoredVm), getVmAddress(monitoredVm));
                } catch ( MonitorException | URISyntaxException exception) {
                    message_log.error("Failed to process VM id: " + vmIdInt, exception);
                }
            }
        } catch ( MonitorException | URISyntaxException exception) {
            message_log.error("JMX failure", exception);
        }

        return results;
    }

    private static String getVmCommand(final MonitoredVm monitoredVm) throws MonitorException{

        final String jvmArgs = MonitoredVmUtil.jvmArgs(monitoredVm);
        final String cmdLine = MonitoredVmUtil.commandLine(monitoredVm);

        message_log.debug("getMonVM: cmdLine=" + cmdLine);
        message_log.debug("getMonVM: jvmArgs=" + jvmArgs);

        return jvmArgs + " " + cmdLine;
    }

    private static String getVmAddress(final MonitoredVm monVm) {
        String result = "";
        try {
            Monitor monAttr = monVm.findByName("sun.management.JMXConnectorServer.0.remoteAddress");
            if ( monAttr == null ) {
                monAttr= monVm.findByName("sun.management.JMXConnectorServer.address");
            }
            if ( monAttr != null ) {
                result = (String)monAttr.getValue();
                message_log.debug("getVmAddress: monAttr " + monAttr.getName() + "=" + result);
            }
        } catch(MonitorException ignored) {}

        return result;
    }
}
