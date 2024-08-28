/**
 *
 */
package com.ericsson.cifwk.diagmon.e2e;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author eswavin
 *
 */
public class MbeanTextFileWriter {

    private final JbossConnectionInfo jbossConnectionInfo;
    private final List<String> mbeanNames;

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(InstrumentedBeanReaderImpl.class);

    public MbeanTextFileWriter(final JbossConnectionInfo jBossConnectionInfo, final List<String> mbeanNames) {
        this.jbossConnectionInfo = jBossConnectionInfo;
        this.mbeanNames = mbeanNames;
    }

    public void writeMbeansToFile() {
        final String filename = System.getProperty(Constants.MBEAN_OUTPUT_DIRECTORY_SYS_ARG) + File.separator + "e2e_"
                + jbossConnectionInfo.getInstanceName() + ".mbeans";
        try {
            final PrintWriter writer = new PrintWriter(filename, "UTF-8");

            writeCredentialsInFile(writer);
            for (final String mbeanName : mbeanNames) {
                writer.println(mbeanName);
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            LOGGER.error(String.format("Failed while writing %s JBoss connection information to text file, %s",
                    jbossConnectionInfo.getInstanceName(), filename), e);

        }
    }

    private void writeCredentialsInFile(final PrintWriter writer) {
        writer.println("jee.container.instance.name=" + jbossConnectionInfo.getInstanceName());
        writer.println("jee.container.management.ip=" + jbossConnectionInfo.getIpAddress());
        writer.println("jee.container.management.port=" + jbossConnectionInfo.getManagementPort());
        writer.println("jee.container.management.user=" + jbossConnectionInfo.getManagementUser());
        writer.println("jee.container.management.password=" + jbossConnectionInfo.getManagementPassword());
    }

}
