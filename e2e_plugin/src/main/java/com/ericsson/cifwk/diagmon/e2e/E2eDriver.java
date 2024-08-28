/**
 *
 */
package com.ericsson.cifwk.diagmon.e2e;

import java.io.File;

import org.apache.commons.cli.*;

/**
 * @author eswavin
 *
 */
@SuppressWarnings({"PMD.DoNotCallSystemExit"})
public class E2eDriver {
    private static boolean displayNameRequired = false;

    private final static String OPT_METRICS_DIR = "metricsdir";
    private final static String OPT_LOGLEVEL = "loglevel";
    private static final String OPT_CONFIG = "config";
    private static final String OPT_MBEANDIR = "mbeansdir";
    private static final String OPT_JVMNAME = "jvmname";
    private static final String OPT_JMXURL = "jmxurl";
    private static final String OPT_DISPLAYNAME = "displayname";
    private static final String OPT_PING = "ping";

    private static org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(E2eDriver.class.getName());

    public static void main(final String[] args) {
        initLogging();
        final E2eDriver e2eDriver = new E2eDriver();
        try {
            e2eDriver.runE2eInstr(args);
            e2eDriver.exit(0);
        } catch (Throwable t) {
            System.out.println("ERROR: " + t.getMessage());
            e2eDriver.exit(1);
        }
    }

    protected void exit(final int exitCode) {
        System.exit(exitCode);
    }

    protected void runE2eInstr(final String[] args) throws PluginException, ParseException {
        final Options options = new Options();
        options.addOption(OPT_CONFIG,true,"e2e config file");
        options.addOption(OPT_MBEANDIR,true,"Output directory for mbean list");
        options.addOption(OPT_JVMNAME,true,"Target JVM name");
        options.addOption(OPT_METRICS_DIR,true,"Metrics directory");
        options.addOption(OPT_LOGLEVEL,true,"Logging level");
        options.addOption(OPT_JMXURL,true,"JMX URL of target JVM");
        options.addOption(OPT_DISPLAYNAME, false, "Require Display Name attribute");
        options.addOption(OPT_PING, false, "Ping target JVM");
        final CommandLineParser parser = new GnuParser();
        final CommandLine cmdArgs = parser.parse( options, args );

        if ( cmdArgs.hasOption(OPT_LOGLEVEL)) {
            com.ericsson.cifwk.diagmon.util.common.Logging.setLevel(cmdArgs.getOptionValue(OPT_LOGLEVEL));
        }
        logger.warn("Starting E2E instrumentation collection.");

        final InstrumentedBeanReaderImpl instrumentedBeanReaderImpl = createInstrumentedBeanReader();

        if ( cmdArgs.hasOption(OPT_CONFIG)) {
            instrumentedBeanReaderImpl.loadConfig(cmdArgs.getOptionValue(OPT_CONFIG));
        }

        if ( cmdArgs.hasOption(OPT_MBEANDIR) ) {
            System.setProperty(Constants.MBEAN_OUTPUT_DIRECTORY_SYS_ARG,
                    cmdArgs.getOptionValue(OPT_MBEANDIR));
        }

        if ( cmdArgs.hasOption(OPT_JVMNAME)) {
            System.setProperty(Constants.MONITOR_JVM_NAME, cmdArgs.getOptionValue(OPT_JVMNAME));
        }

        if ( cmdArgs.hasOption(OPT_JMXURL)) {
            System.setProperty(Constants.MONITOR_JMX_SERVICE_URL, cmdArgs.getOptionValue(OPT_JMXURL));
        }

        if ( cmdArgs.hasOption(OPT_DISPLAYNAME) ) {
            displayNameRequired = true;
        }

        if ( cmdArgs.hasOption(OPT_PING) ) {
            exit(instrumentedBeanReaderImpl.ping());
        } else {
            if (cmdArgs.hasOption(OPT_METRICS_DIR) ) {
                setMetricsDirectory(cmdArgs.getOptionValue(OPT_METRICS_DIR));
            } else {
                throw new PluginException("Provide the directory path to put e2e xml files.");
            }

            instrumentedBeanReaderImpl.getInstrumentedBeanProfiles();
            instrumentedBeanReaderImpl.collectJbossInstanceAndMbeanInfo();
        }
    }

    protected InstrumentedBeanReaderImpl createInstrumentedBeanReader() {
        return new InstrumentedBeanReaderImpl();
    }

    protected void setMetricsDirectory(final String args) throws PluginException {
        final String xmlOutputDirectory = args;
        if (xmlOutputDirectory != null && xmlOutputDirectory.length() > 0) {
            final File directory = createNewFile(xmlOutputDirectory);
            if (directory.exists() && directory.isDirectory()) {
                System.setProperty(Constants.XML_OUTPUT_DIRCETORY_SYS_ARG, xmlOutputDirectory);
            } else {
                throw new PluginException("Directory does not exists for putting e2e xml files.");
            }
        } else {
            throw new PluginException("Directory to put e2e xml files has to be given as an argument to the program.");
        }
    }

    protected File createNewFile(final String xmlOutputDirectory) {
        return new File(xmlOutputDirectory);
    }

    protected static boolean isDisplayNameRequired() {
        return displayNameRequired;
    }

    private static void initLogging() {
         // Configuration now done by the log4j2.xml file
    }

}
