package com.ericsson.cifwk.diagmon.e2e;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class MbeanTextFileWriterTest {
    private MbeanTextFileWriter mbeanTextFileWriter;
    private static File testDirectory;

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


    @Before
    public void setUp() throws Exception {
        final List<String> mbeanList = new ArrayList<String>();
        mbeanList.add("testMbean");

        final JbossConnectionInfo connectionInfo = new JbossConnectionInfo();
        connectionInfo.setInstanceName("testInstance");
        this.mbeanTextFileWriter = new MbeanTextFileWriter(connectionInfo, mbeanList);
    }


    @Test
    public void testWriteMbeansToFile() {
        System.setProperty(Constants.MBEAN_OUTPUT_DIRECTORY_SYS_ARG, testDirectory.getName());
        this.mbeanTextFileWriter.writeMbeansToFile();
        final File outputFile = new File(testDirectory.getAbsolutePath() + "/e2e_testInstance.mbeans");
        assertTrue(outputFile.length() != 0);
    }

    @Test
    public void testWriteMbeansToFileCatchesException() {
        System.setProperty(Constants.MBEAN_OUTPUT_DIRECTORY_SYS_ARG, "non_existant_directory");
        this.mbeanTextFileWriter.writeMbeansToFile();
    }
}
