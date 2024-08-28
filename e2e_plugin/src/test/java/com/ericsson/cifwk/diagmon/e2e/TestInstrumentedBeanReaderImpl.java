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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Logger.class, JMXConnectorFactory.class, JvmMonitor.class, LogManager.class })
@PowerMockIgnore( {"javax.management.*", "javax.xml.parsers.*", "org.xml.sax.*", "org.w3c.dom.*"})
public class TestInstrumentedBeanReaderImpl {

    private InstrumentedBeanReaderImpl instrumentedBeanReaderImpl;
    private JbossConnectionInfo runnableJbossInstance;
    private JbossConnectionInfo jbossConnectionInfo;
    private Map<ObjectInstance, MBeanInfo> serviceMbeans;
    private Set<ObjectInstance> mbeans;

    private static File testDirectory;
    private Logger logger = LogManager.getLogger(TestInstrumentedBeanReaderImpl.class);

    private static final String RUNNING_JBOSS_INSTANCE = "-Djboss.node.name=ImpExpServ_su_0_jee_cfg ";

    @BeforeClass
    public static void prepareForTests() {
        testDirectory = new File("test_directory");
        testDirectory.mkdir();
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        for(final File file: testDirectory.listFiles()) {
            while(file.exists()) {
                file.delete();
            }
        }
        testDirectory.delete();
    }

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        instrumentedBeanReaderImpl = spy(new InstrumentedBeanReaderImpl());

        PowerMockito.mockStatic(Logger.class);
        PowerMockito.mockStatic(JMXConnectorFactory.class);
        PowerMockito.mockStatic(JvmMonitor.class);
        PowerMockito.mockStatic(LogManager.class);

        runnableJbossInstance = spy(new JbossConnectionInfo());

        serviceMbeans = mock(Map.class);
        mbeans = mock(Set.class);
        logger = mock(Logger.class);
        jbossConnectionInfo = mock(JbossConnectionInfo.class);

        when(LogManager.getLogger(Mockito.any(Class.class))).thenReturn(logger);
    }

    @After
    public void tearDown() throws Exception {
        instrumentedBeanReaderImpl = null;
        runnableJbossInstance = null;
        serviceMbeans = null;
        mbeans = null;
        logger = null;
        jbossConnectionInfo = null;
    }

    @Test
    public void testGetInstrumentedBeanProfiles() throws Exception {
        doReturn(new JbossConnectionInfo()).when(instrumentedBeanReaderImpl).getRunnableInstance();
        instrumentedBeanReaderImpl.getInstrumentedBeanProfiles();
        verify(instrumentedBeanReaderImpl, times(1)).getRunnableInstance();
    }

    @Test
    public void getRunnableJbossInstancesSuccessfully() throws Exception {
        final Map<String, Object> runningJvms = new HashMap<>();
        runningJvms.put(RUNNING_JBOSS_INSTANCE, new Object());
        doReturn(runningJvms).when(JvmMonitor.class, "getJvms");

        runnableJbossInstance = instrumentedBeanReaderImpl.getRunnableInstance();
        assertNotNull(runnableJbossInstance);
    }

    @Test
    public void getRunnableJbossInstancesExitsBecauseNoJbossInstances() throws Exception {
        doNothing().when(instrumentedBeanReaderImpl).exitProcess();

        instrumentedBeanReaderImpl.getRunnableInstance();

        verify(instrumentedBeanReaderImpl, times(1)).exitProcess();
    }

    @Test
    public void testGetProperties() throws IOException {
        File testPropertiesFile = createPropertiesFile("testKey", "testValue");

        final Properties expectedProperties = new Properties();
        expectedProperties.setProperty("testKey", "testValue");

        final Properties properties = instrumentedBeanReaderImpl.getProperties(testPropertiesFile);
        testPropertiesFile = null;
        assertEquals(expectedProperties, properties);
    }

    private File createPropertiesFile(final String key, final String value) throws IOException, FileNotFoundException {
        final File testPropertiesFile = new File(testDirectory.getAbsolutePath() + "/test_properties");
        testPropertiesFile.createNewFile();
        PrintWriter writer = new PrintWriter(testPropertiesFile);
        writer.println(key + "=" + value);
        writer.flush();
        writer.close();
        writer = null;
        return testPropertiesFile;
    }

    @Test
    public void testGetPropertiesReturnsEmptyList() {
        final Properties properties = instrumentedBeanReaderImpl.getProperties(null);
        assertEquals(new Properties(), properties);
    }

    @Test
    public void testGetPropertiesRaisesIOException() throws FileNotFoundException, IOException {
        final File credentialFile = new File("startNameOfServer_jee-tr-1.");

        doThrow(new IOException()).when(instrumentedBeanReaderImpl).loadProperties(credentialFile);
        instrumentedBeanReaderImpl.getProperties(credentialFile);
    }

    @Test
    public void testGetPropertiesRaisesFileNotFoundException() throws IOException {
        final File credentialFile = new File("startNameOfServer_jee-tr-1.");
        instrumentedBeanReaderImpl.getProperties(credentialFile);
    }

    @Test
    public void testCollectJbossInstanceAndMBeanInfo() throws Exception {
        instrumentedBeanReaderImpl.setRunnableJbossInstanceToQuery(jbossConnectionInfo);
        final MBeanServerConnection mbeanServerConnection = mock(MBeanServerConnection.class);

        doReturn(mbeanServerConnection).when(instrumentedBeanReaderImpl).establishConnectionToInstanceMbeanServer(
                jbossConnectionInfo);
        doReturn(mbeans).when(instrumentedBeanReaderImpl).getMbeans(jbossConnectionInfo);
        doReturn(1).when(mbeans).size();
        doNothing().when(instrumentedBeanReaderImpl).createXMLForMbeans(jbossConnectionInfo, mbeans);

        instrumentedBeanReaderImpl.collectJbossInstanceAndMbeanInfo();
        verify(instrumentedBeanReaderImpl).createXMLForMbeans(jbossConnectionInfo, mbeans);
    }

    @Test
    public void testCollectJbossInstancesAndMbeanInfoNoMbeans() throws Exception {
        instrumentedBeanReaderImpl.setRunnableJbossInstanceToQuery(jbossConnectionInfo);
        final MBeanServerConnection mbeanServerConnection = mock(MBeanServerConnection.class);

        doReturn(mbeanServerConnection).when(instrumentedBeanReaderImpl).establishConnectionToInstanceMbeanServer(
                jbossConnectionInfo);
        doReturn(mbeans).when(instrumentedBeanReaderImpl).getMbeans(jbossConnectionInfo);
        doReturn(true).when(mbeans).isEmpty();

        instrumentedBeanReaderImpl.collectJbossInstanceAndMbeanInfo();
        verify(instrumentedBeanReaderImpl, Mockito.never()).createXMLForMbeans(jbossConnectionInfo, mbeans);
    }

    @Test
    public void testCollectJbossInstanceAndMbeanInfoWriteMbeansToFile() throws FileNotFoundException, IOException, PluginException, MalformedObjectNameException {
        instrumentedBeanReaderImpl.setRunnableJbossInstanceToQuery(jbossConnectionInfo);
        File testPropertiesFile = createPropertiesFile("searchlist", "Mbean_testname");
        instrumentedBeanReaderImpl.loadConfig(testPropertiesFile.getAbsolutePath());
        testPropertiesFile = null;

        final MbeanTextFileWriter mBeanTextFileWriter = mock(MbeanTextFileWriter.class);
        doNothing().when(mBeanTextFileWriter).writeMbeansToFile();
        doReturn(mBeanTextFileWriter).when(instrumentedBeanReaderImpl).createMbeanTextFileWriter(Mockito.anyListOf(String.class));
        final MBeanServerConnection mbeanServerConnection = mock(MBeanServerConnection.class);
        doReturn(mbeanServerConnection).when(instrumentedBeanReaderImpl).establishConnectionToInstanceMbeanServer(jbossConnectionInfo);

        final ObjectInstance objectInstance = mock(ObjectInstance.class);
        mbeans = new HashSet<ObjectInstance>();
        mbeans.add(objectInstance);
        doReturn(mbeans).when(instrumentedBeanReaderImpl).getMbeans(jbossConnectionInfo);
        doReturn(serviceMbeans).when(instrumentedBeanReaderImpl).getServiceMbeans(jbossConnectionInfo, mbeans);

        final ObjectName objectName = mock(ObjectName.class);
        doReturn(objectName).when(objectInstance).getObjectName();
        doReturn("Mbean_testname").when(objectName).getCanonicalName();

        instrumentedBeanReaderImpl.collectJbossInstanceAndMbeanInfo();

        verify(mBeanTextFileWriter).writeMbeansToFile();
    }

    @Test
    public void testCreateXMLForMbeans() throws Exception {
        doReturn(serviceMbeans).when(instrumentedBeanReaderImpl).getServiceMbeans(jbossConnectionInfo, mbeans);
        doReturn(1).when(serviceMbeans).size();

        instrumentedBeanReaderImpl.createXMLForMbeans(jbossConnectionInfo, mbeans);

    }

    @Test
    public void testGetServiceMbeans() throws Exception {
        mbeans = new HashSet<ObjectInstance>();
        final ObjectInstance mbean = mock(ObjectInstance.class);
        mbeans.add(mbean);

        final ObjectName objectName = mock(ObjectName.class);
        final String domain = "com.ericsson.this.is.a.domain";
        final MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        final MBeanInfo mBeanInfo = mock(MBeanInfo.class);
        final Descriptor descriptor = mock(Descriptor.class);
        final String[] fieldNames = { "DisplayName" };

        final MBeanAttributeInfo attributeInfo = new MBeanAttributeInfo("", "", "", false, false, false);
        final MBeanAttributeInfo[] mBeanAttributeInfo = new MBeanAttributeInfo[1];
        mBeanAttributeInfo[0] = attributeInfo;

        doReturn(objectName).when(mbean).getObjectName();
        doReturn(domain).when(objectName).getDomain();
        doReturn(mBeanServerConnection).when(jbossConnectionInfo).getMbeanServerConnection();
        doReturn(mBeanInfo).when(mBeanServerConnection).getMBeanInfo(objectName);
        doReturn(descriptor).when(mBeanInfo).getDescriptor();
        doReturn(fieldNames).when(descriptor).getFieldNames();
        doReturn(mBeanAttributeInfo).when(mBeanInfo).getAttributes();

        final Map<ObjectInstance, MBeanInfo>  result = instrumentedBeanReaderImpl.getServiceMbeans(jbossConnectionInfo, mbeans);
        assertTrue(result.size() == 1);
    }

    @Test
    public void testGetMbeans() throws Exception {
        final MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);

        doReturn(mBeanServerConnection).when(jbossConnectionInfo).getMbeanServerConnection();
        doReturn(mbeans).when(mBeanServerConnection).queryMBeans(null, null);

        final Set<ObjectInstance> returnedMBeans = instrumentedBeanReaderImpl.getMbeans(jbossConnectionInfo);
        assertNotNull(returnedMBeans);
    }

    @Test
    public void testEstablishConnectionToInstanceMBeanServer() throws Exception {
        final JMXServiceURL serviceUrl = mock(JMXServiceURL.class);
        final JMXConnector connector = mock(JMXConnector.class);
        final MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);

        doReturn(serviceUrl).when(instrumentedBeanReaderImpl).createJMXServiceURL(Mockito.anyString());
        doReturn(connector).when(JMXConnectorFactory.class, "connect", serviceUrl, new HashMap<String, String[]>());
        doReturn(mBeanServerConnection).when(instrumentedBeanReaderImpl).createMBeanServerConnection(
                Mockito.any(JMXConnector.class));

        final MBeanServerConnection returnedMBeanServerConnection = instrumentedBeanReaderImpl
                .establishConnectionToInstanceMbeanServer(jbossConnectionInfo);

        assertNotNull(returnedMBeanServerConnection);
    }

    @Test
    public void testEstablishConnectionToInstanceMBeanServerNull() throws Exception {
        doThrow(new MalformedURLException()).when(instrumentedBeanReaderImpl).createJMXServiceURL(Mockito.anyString());

        final MBeanServerConnection returnedMBeanServerConnection = instrumentedBeanReaderImpl
                .establishConnectionToInstanceMbeanServer(jbossConnectionInfo);

        assertNull(returnedMBeanServerConnection);
    }

    @Test
    public void testCreateMBeanServerConnection() throws Exception {
        final JMXConnector connector = mock(JMXConnector.class);
        final MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        doReturn(mBeanServerConnection).when(connector).getMBeanServerConnection();

        final MBeanServerConnection returnedMBeanServerConnection = instrumentedBeanReaderImpl
                .createMBeanServerConnection(connector);
        assertNotNull(returnedMBeanServerConnection);
    }

    @Test
    public void testPingOkay() throws Exception {
        final MBeanServerConnection mbeanServerConnection = mock(MBeanServerConnection.class);
        doReturn(new JbossConnectionInfo()).when(instrumentedBeanReaderImpl).getRunnableInstance();
        doReturn(mbeanServerConnection).when(instrumentedBeanReaderImpl).establishConnectionToInstanceMbeanServer(Mockito.any());

        assertEquals(0 , instrumentedBeanReaderImpl.ping());
    }

    @Test
    public void testPingFail() throws Exception {
        doThrow(new MalformedURLException()).when(instrumentedBeanReaderImpl).createJMXServiceURL(Mockito.anyString());
        doReturn(new JbossConnectionInfo()).when(instrumentedBeanReaderImpl).getRunnableInstance();

        assertEquals(1 , instrumentedBeanReaderImpl.ping());
    }

}
