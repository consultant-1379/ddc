package com.ericsson.cifwk.diagmon.e2e;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MbeanXmlWriterTest{

    private MbeanXmlWriter mbeanXmlWriter;
    private static File testDirectory;

    @Mock
    JbossConnectionInfo mockJbossConnectionInfo;
    @Mock
    Map<ObjectInstance, MBeanInfo> mockServiceMbeans;
    @Mock
    Logger mockLogger;

    @BeforeClass
    public static void prepareForTests() {
        testDirectory = new File("test_directory");
        testDirectory.mkdir();
    }

    @AfterClass
    public static void cleanUp() {
        for(final File file: testDirectory.listFiles()) {
            file.delete();
        }
        testDirectory.delete();
    }

    @Test
    public void testWriteMbeanXmlForNullMap() {
        mbeanXmlWriter = new MbeanXmlWriter(mockJbossConnectionInfo, null);
        mbeanXmlWriter.writeMbeanXml();
    }

    
    
    @Test
    public void testWriteMbeanXmlForEmptyMap() {
        mbeanXmlWriter = new MbeanXmlWriter(mockJbossConnectionInfo, new HashMap<ObjectInstance, MBeanInfo>());
        mbeanXmlWriter.writeMbeanXml();
    }

    @Test
    public void testWriteMbeanXml() throws TransformerConfigurationException, ParserConfigurationException,
            TransformerException, MalformedObjectNameException {
        doReturn(false).when(mockServiceMbeans).isEmpty();
        mbeanXmlWriter = new MbeanXmlWriter(mockJbossConnectionInfo, mockServiceMbeans) {
            @Override
            protected File writeXmlFile() throws ParserConfigurationException, TransformerConfigurationException,
                    TransformerException {
                return null;
            }
        };
        mbeanXmlWriter.writeMbeanXml();
    }

    @Test
    public void testWriteMbeanXmlthrowsException() throws TransformerConfigurationException,
            ParserConfigurationException, TransformerException, MalformedObjectNameException {
        doReturn(false).when(mockServiceMbeans).isEmpty();
        mbeanXmlWriter = new MbeanXmlWriter(mockJbossConnectionInfo, mockServiceMbeans) {
            @Override
            protected File writeXmlFile() throws ParserConfigurationException, TransformerConfigurationException,
                    TransformerException {
                throw new ParserConfigurationException();
            }
        };
        mbeanXmlWriter.writeMbeanXml();
    }

    @Test
    public void testWriteMbeanXmlRealCall() throws TransformerConfigurationException, ParserConfigurationException,
            TransformerException, MalformedObjectNameException {
        doReturn(false).when(mockServiceMbeans).isEmpty();
        final File testFile = new File(testDirectory.getAbsolutePath() + "/temp.xml");

        mbeanXmlWriter = new MbeanXmlWriter(mockJbossConnectionInfo, mockServiceMbeans) {
            @Override
            protected File getFile() {
                return testFile;
            }
        };
        mbeanXmlWriter.writeMbeanXml();
        assertTrue(testFile.exists());
    }

}
