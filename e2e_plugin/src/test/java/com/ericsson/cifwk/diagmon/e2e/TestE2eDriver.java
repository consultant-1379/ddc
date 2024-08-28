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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(System.class)
public class TestE2eDriver {

    private E2eDriver main;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        main = spy(new E2eDriver());
    }

    @After
    public void tearDown() throws Exception {
        mockStatic(System.class);
        main = null;
    }

    @Test
    public void testRunMain() throws Exception {
        final String[] args = { "--metricsdir", "arg1" };

        final InstrumentedBeanReaderImpl instrumentedBeanReaderImpl = mock(InstrumentedBeanReaderImpl.class);

        doReturn(instrumentedBeanReaderImpl).when(main).createInstrumentedBeanReader();
        doNothing().when(main).setMetricsDirectory(args[1]);
        doNothing().when(instrumentedBeanReaderImpl).getInstrumentedBeanProfiles();
        doNothing().when(instrumentedBeanReaderImpl).collectJbossInstanceAndMbeanInfo();
        main.runE2eInstr(args);
    }

    @Test
    public void testPing() throws Exception {
        final String[] args = { "--ping" };

        final InstrumentedBeanReaderImpl instrumentedBeanReaderImpl = mock(InstrumentedBeanReaderImpl.class);
        doReturn(instrumentedBeanReaderImpl).when(main).createInstrumentedBeanReader();

        doReturn(0).when(instrumentedBeanReaderImpl).ping();
        doNothing().when(main).exit(0);

        main.runE2eInstr(args);

        verify(instrumentedBeanReaderImpl, times(1)).ping();
    }

    @Test
    public void testCreateInstrumentedBeanReader() throws Exception {
        assertNotNull(main.createInstrumentedBeanReader());
    }

    @Test
    public void testSetMetricsDirectory() throws Exception {
        final String arg = "arg";
        final File directory = mock(File.class);

        doReturn(directory).when(main).createNewFile(arg);
        doReturn(true).when(directory).exists();
        doReturn(true).when(directory).isDirectory();

        main.setMetricsDirectory(arg);
        assertEquals(System.getProperty(Constants.XML_OUTPUT_DIRCETORY_SYS_ARG), "arg");
    }

    @Test(expected = PluginException.class)
    public void testSetMetricDirectoryDirectoryDoesNotExist() throws Exception {
        final String arg = "arg";
        main.setMetricsDirectory(arg);
    }

    @Test(expected = PluginException.class)
    public void testSetMetricDirectoryNoDirectoryArg() throws Exception {
        main.setMetricsDirectory(null);
    }
}
