package com.ericsson.cifwk.diagmon.agent.tests;

import javax.management.ObjectName;

import com.ericsson.cifwk.diagmon.agent.eventservice.EventService;

public class TestApp {
    public TestApp(int numMBeans, int pollInterval) {
        EventService e = EventService.get();
        for (int i = 0 ; i < numMBeans ; i++) {
            // Create an MBean
            String jmxBeanObjectName = "com.ericsson.cifwk.diagmon.agent.test.TestMetric" + i +":type=test";
            TestMetric tm = new TestMetric(jmxBeanObjectName, "Test artefact");
            e.registerMetric(jmxBeanObjectName, "Value", "Test Artefact", pollInterval);
        }
        // 
    }
    public static void main(String[] args) {
        int numMBeans = Integer.parseInt(args[0]);
        int pollInterval = Integer.parseInt(args[1]);
        int uptime = Integer.parseInt(args[2]);
        TestApp t = new TestApp(numMBeans, pollInterval);
        try {
            Thread.sleep(uptime * 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
