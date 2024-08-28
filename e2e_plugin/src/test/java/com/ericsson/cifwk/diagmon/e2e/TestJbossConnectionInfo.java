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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;

import org.junit.Before;
import org.junit.Test;

public class TestJbossConnectionInfo {

    private JbossConnectionInfo jbossConnectionInfo;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        jbossConnectionInfo = new JbossConnectionInfo();
    }

    @Test
    public void testSetAndGetInstanceName() {
        jbossConnectionInfo.setInstanceName("name");
        assertEquals("name", jbossConnectionInfo.getInstanceName());
    }

    @Test
    public void testSetAndGetIPAddress() {
        jbossConnectionInfo.setIpAddress("0.0.0.0");
        assertEquals("0.0.0.0", jbossConnectionInfo.getIpAddress());
    }

    @Test
    public void testSetAndGetManagementPassword() {
        jbossConnectionInfo.setManagementPassword("password");
        assertEquals("password", jbossConnectionInfo.getManagementPassword());
    }

    @Test
    public void testSetAndGetMbeanServerConnection() {
        final MBeanServerConnection mBeanServerConnection = MBeanServerFactory.createMBeanServer();
        jbossConnectionInfo.setMbeanServerConnection(mBeanServerConnection);
        assertEquals(mBeanServerConnection, jbossConnectionInfo.getMbeanServerConnection());
    }

    @Test
    public void testSetAndGetManagementUser() {
        jbossConnectionInfo.setManagementUser("user");
        assertEquals("user", jbossConnectionInfo.getManagementUser());
    }

    @Test
    public void testHasManagementUser() {
        jbossConnectionInfo.setManagementUser("user");
        assertTrue(jbossConnectionInfo.hasManagementUser());
    }

    @Test
    public void testSetAndGetManagementPort() {
        jbossConnectionInfo.setManagementPort("1234");
        assertEquals("1234", jbossConnectionInfo.getManagementPort());
    }

    @Test
    public void testGetXmlFilenameForDDC() {
        jbossConnectionInfo.setInstanceName("name");
        assertEquals("e2e_name.xml", jbossConnectionInfo.getXmlFilenameForDDC());
    }
}
