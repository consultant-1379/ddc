package com.ericsson.cifwk.diagmon.e2e;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JvmMonitorTest {

    @Test
    public void testGetJvms() {
        assertTrue(JvmMonitor.getJvms().size() > 0);
    }
}
