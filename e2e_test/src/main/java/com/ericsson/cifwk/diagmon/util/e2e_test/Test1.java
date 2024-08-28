/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.cifwk.diagmon.util.e2e_test;

import java.lang.management.ManagementFactory;
import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Singleton
@Startup
public class Test1 implements Test1MBean, DynamicMBean {
    int metric1 = 0;
    private MBeanServer platformMBeanServer;
    private ObjectName objectName;

    @PostConstruct
    public void start() {
        try {
            objectName = new ObjectName("com.ericsson.cifwk.diagmon.util.test:type=Test1");
            platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(this, objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register " + objectName + " into JMX:" + e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            platformMBeanServer.unregisterMBean(objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unregister " + objectName + " from JMX:" + e);
        }
    }

    public void setMetric1(final int metric1) {
        this.metric1 = metric1;
    }

    public int getMetric1() {
        return metric1;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        final MBeanAttributeInfo[] mBeanAttributeInfo = new MBeanAttributeInfo[] {
                new MBeanAttributeInfo(
                        "attr1",
                        Integer.TYPE.getName(),
                        "attr1 description",
                        true,
                        false,
                        false)
        };
        final Descriptor mbeanDescriptor = new DescriptorSupport();
        mbeanDescriptor.setField("DisplayName", "Test1 Metrics");
        return new MBeanInfo(
                this.getClass().getName(),
                "Collects metrics and profiling data of the Test1 MBean",
                mBeanAttributeInfo,
                null,
                null,
                null,
                mbeanDescriptor);
    }

    @Override
    public Object getAttribute(final String attribute) {
        return Integer.valueOf(0);
    }

    @Override
    public AttributeList getAttributes(final String[] attributes) {
        final AttributeList resultList = new AttributeList();
        for (final String attribute : attributes) {
            try {
                final Object value = getAttribute(attribute);
                if (value != null) {
                    resultList.add(new Attribute(attribute, value));
                }
            } catch (final Exception exception) {
                throw new RuntimeException("getAttributes failed", exception); // NOSONAR
            }
        }
        return resultList;
    }

    @Override
    public void setAttribute(final Attribute attribute) {
        throw new UnsupportedOperationException("Setting attribute not allowed");
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        throw new UnsupportedOperationException("Setting attributes not allowed");
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature) {
        throw new UnsupportedOperationException("Invoke not allowed");
    }

    public static void main(final String args[]) {
        final Test1 obj = new Test1();
        obj.start();
        try {
            Thread.sleep(600 * 1000);
        } catch (Exception ignored) {
        }
    }
}
