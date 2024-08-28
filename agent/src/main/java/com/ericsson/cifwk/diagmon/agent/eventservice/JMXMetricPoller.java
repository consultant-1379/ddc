package com.ericsson.cifwk.diagmon.agent.eventservice;

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

import com.ericsson.cifwk.diagmon.agent.common.Logger;

public class JMXMetricPoller extends MetricPoller {
    private String description;
    private String jmxBeanObjectName;
    private String paramName;
    private ObjectName objName;
    
    // MBeanServer to retrieve metrics from
    MBeanServer mbs;

    public String getDescription() {
        return description;
    }

    public String getObjectName() {
        return jmxBeanObjectName;
    }

    public String getValue() {
        Logger.debug("in getValue: " + objName + "; " + paramName);
        if (objName == null || paramName == null || mbs == null) {
            Logger.debug("All values null: returning null");
            return null;
        }
        try {
            String values = "";
            Object attr = mbs.getAttribute(objName, paramName);
            if(attr instanceof CompositeDataSupport) {
                String delim = "";
                CompositeDataSupport cdsObj = (CompositeDataSupport)attr;
                
                for (Object mName : cdsObj.getCompositeType().keySet().toArray()) {
                    values += delim + mName + "=" + cdsObj.get(mName.toString());
                    delim = "&";
                }
            } else if (attr.getClass().isArray()) {
                values = "" + ((Object[])attr).length;
            } else {
                values = attr.toString();
            }
            return paramName + "=" + values;
        } catch (AttributeNotFoundException e) {
            Logger.debug("AttributeNotFoundException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        } catch (InstanceNotFoundException e) {
            Logger.debug("InstanceNotFoundException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        } catch (MBeanException e) {
            Logger.debug("MBeanException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        } catch (ReflectionException e) {
            Logger.debug("ReflectionException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        }
        // TODO:
        // In some cases a runtime exception can be thrown here - for example
        // a coding error! In those cases the handling executor service should
        // silently discard the metric provider. We may want to catch a
        // generic exception, log it and then throw it upwards - that includes
        // for the exceptions above.
        return null;
    }

    public JMXMetricPoller(String jmxBeanObjectName, String paramName,
            String description, int collectionPeriod) {
        Logger.debug("Constructing JMXMetricPoller: " + jmxBeanObjectName + " ; " + paramName);
        this.jmxBeanObjectName = jmxBeanObjectName;
        this.paramName = paramName;
        this.description = description;
        this.setPollIntervalSeconds(collectionPeriod);
        try {
            this.objName = new ObjectName(jmxBeanObjectName);
        } catch (MalformedObjectNameException e) {
            Logger.debug("MalformedObjectNameException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        } catch (NullPointerException e) {
            Logger.debug("NullPointerException resolving " +
                    jmxBeanObjectName + ": " + e.getMessage());
        }
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            String objNameStr = this.jmxBeanObjectName.replace(":type=", ".") + "." + this.paramName;
            ObjectName name = new ObjectName("com.ericsson.cifwk.diagmon.agent.eventservice.MetricPoller:type=jmx."
                    + objNameStr);
            mbs.registerMBean(this, name);
        } catch (MalformedObjectNameException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (NullPointerException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        }
    }
}
