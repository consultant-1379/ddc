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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * This class attempts to read all the JBoss instance configuration details and
 * convert them to XML files for the JBoss instance.
 *
 * DDC calls the public method getInstrumentedBeanProfiles(), which kicks off
 * the daemon thread. This operates asynchronously; as each JBoss instance is
 * contacted, we write a file into the pollDir. Therefore, this method must be
 * called <b>after</b> the InstrDirectoryPoller is instantiated.
 *
 * It may be that some JBoss instances are not running when this is called.
 * Therefore, we keep trying to connect to each configured instance on a timer.
 * In addition, if we can make a connection, we keep the mbeanConnection.
 *
 * However, if the JBoss instance is not available when the collection is first
 * called, we try to make a connection to all the instances for up to one
 * minute. If we can't make a connection, then we continue and collect the other
 * statistics.
 *
 * If a JBoss instance doesn't come up at start of day, we poll for it in a
 * delay loop in the daemon thread.
 */
public class InstrumentedBeanReaderImpl implements InstrumentedBeanReader {

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(InstrumentedBeanReaderImpl.class);
    private final Map<String, List<String>> blacklist = new HashMap<>();

    private final List<String> searchList = new LinkedList<String>();

    // Custom poll intervals
    private final Map<String,Integer> pollIntervals = new HashMap<String, Integer>();

    private JbossConnectionInfo runnableJbossInstanceToQuery = null;

    /**
     * Check if we can connect to the target JVM
     *
     * @return 0 if we can connect, 1 otherwise
     */
    public int ping() {
        int result = 1;
        try {
            final MBeanServerConnection mbeanServerConnection = establishConnectionToInstanceMbeanServer(getRunnableInstance());
            if (mbeanServerConnection != null) {
                result = 0;
            }
        } catch ( Throwable ignored ) {}

        return result;
    }

    @Override
    public void collectJbossInstanceAndMbeanInfo() throws PluginException {
        MBeanServerConnection mbeanServerConnection = null;

        mbeanServerConnection = establishConnectionToInstanceMbeanServer(runnableJbossInstanceToQuery);
        if (mbeanServerConnection == null) {
            throw new PluginException("Failed to connect to the JBoss instance :: " + runnableJbossInstanceToQuery.getInstanceName()
                         + ", Check the configuration.");
        }
        runnableJbossInstanceToQuery.setMbeanServerConnection(mbeanServerConnection);
        final Set<ObjectInstance> mbeans = getMbeans(runnableJbossInstanceToQuery);
        if ( mbeans == null ) {
            throw new PluginException("Failed to get MBeans from JBoss");
        } else if (!mbeans.isEmpty()) {
            createXMLForMbeans(runnableJbossInstanceToQuery, mbeans);

            // Output list of Mbeans matching the searchList
            if (!searchList.isEmpty()) {
                LOGGER.debug("Searching for MBeans matching " + searchList.toString());

                final List<String> matches = new LinkedList<String>();
                for (final ObjectInstance mbean : mbeans) {
                    final String mbeanName = mbean.getObjectName().getCanonicalName();
                    LOGGER.debug("Checking " + mbeanName);
                    for (final String match : searchList) {
                        if (mbeanName.matches(match)) {
                            matches.add(mbeanName);
                        }
                    }
                }
                LOGGER.debug("Matched " + matches.size());
                if (!matches.isEmpty()) {
                    createMbeanTextFileWriter(matches).writeMbeansToFile();
                }
            }
        } else {
            LOGGER.debug("No mbeans found for the instance " + runnableJbossInstanceToQuery.getInstanceName());
        }

        mbeanServerConnection = null;
    }

    @Override
    public MBeanServerConnection establishConnectionToInstanceMbeanServer(final JbossConnectionInfo jbossConnectionInfo) {
        try {
            final JMXServiceURL serviceUrl = createJMXServiceURL(jbossConnectionInfo.getURL());
            LOGGER.debug("establishConn():serviceUrl=" + serviceUrl);

            final Map<String, String[]> env = new HashMap<String, String[]>();
            if (jbossConnectionInfo.hasManagementUser()) {
                final String[] creds = new String[] { jbossConnectionInfo.getManagementUser(),
                                                      jbossConnectionInfo.getManagementPassword() };
                LOGGER.debug("establishConn():jbossConnectionInfo.managementUser=" + jbossConnectionInfo.getManagementUser());
                env.put("jmx.remote.credentials", creds);
            }

            final JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, env);
            LOGGER.debug("establishConn():connector=" + connector);
            final MBeanServerConnection mbeanServerConnection = createMBeanServerConnection(connector);
            LOGGER.debug("establishConn():mbeanServerConnection=" + mbeanServerConnection);

            return mbeanServerConnection;
        } catch (Throwable e) {
            LOGGER.warn("establishConnection():Couldn't contact JMX Server: " + jbossConnectionInfo, e);
            return null;
        }
    }

    /**
     * This is the main entry point. It kicks off the daemon thread and returns
     * quickly
     */
    @Override
    public synchronized void getInstrumentedBeanProfiles() {
        runnableJbossInstanceToQuery = getRunnableInstance(); // from LITP
        LOGGER.debug("The instances collected from LITP :: " + runnableJbossInstanceToQuery);
    }

    @Override
    public JbossConnectionInfo getRunnableInstance() {

        final String monitorJvmName = System.getProperty(Constants.MONITOR_JVM_NAME);
        final String monitorJmxServiceURL = System.getProperty(Constants.MONITOR_JMX_SERVICE_URL);
        if ( monitorJvmName == null && monitorJmxServiceURL == null) {
            final Map<String, String> runningJvms = JvmMonitor.getJvms();
            final Pattern pattern = Pattern.compile("-Djboss.node.name=(\\S+) ");
            String jbossInstance = null;

            for (final String jbossNodeReference : runningJvms.keySet()) {
                final Matcher matcher = pattern.matcher(jbossNodeReference);
                if (matcher.find()) {
                    jbossInstance = matcher.group(1);
                    LOGGER.debug("getRunnableInstances: matched " + jbossInstance);
                    break;
                }
            }
            if (jbossInstance == null) {
                LOGGER.debug("No JBoss instance found");
                exitProcess();
            }

            final JbossConnectionInfo result = createJbossConnectionInfo(jbossInstance);
            setJbossIPandPort(result);
            final String jmxUrl = "service:jmx:remoting-jmx://" + result.getIpAddress() + ":"
                    + result.getManagementPort();
            result.setURL(jmxUrl);
            return result;
        } else if ( monitorJmxServiceURL != null && monitorJvmName != null ) {
            final JbossConnectionInfo result = createJbossConnectionInfo(monitorJvmName);
            String jmxServiceURL = monitorJmxServiceURL;
            // If the url has a userId:password then get it
            String userIdPassword = null;
            if ( jmxServiceURL.indexOf("@") != -1 ) {
                final String[] parts = jmxServiceURL.split("@");
                userIdPassword = parts[0];
                jmxServiceURL = parts[1];
            }
            result.setURL(jmxServiceURL);
            if ( userIdPassword != null ) {
                final String[] userIdPasswordParts = userIdPassword.split(":");
                byte[] decryptUser = Base64.getDecoder().decode(userIdPasswordParts[0]);
                byte[] decryptPasswd = Base64.getDecoder().decode(userIdPasswordParts[1]);
                result.setManagementUser(new String(decryptUser, StandardCharsets.UTF_8));
                result.setManagementPassword(new String(decryptPasswd, StandardCharsets.UTF_8));
            }
            return result;
        } else {
            final Map<String, String> runningJvms = JvmMonitor.getJvms();
            final Pattern pattern = Pattern.compile("-Ds=" + monitorJvmName + " ");
            String url = null;
            for (final Map.Entry<String, String> entry : runningJvms.entrySet() ) {
                final Matcher matcher = pattern.matcher(entry.getKey());
                if (matcher.find()) {
                    url = entry.getValue();
                }
            }
            if ( url == null ) {
                LOGGER.error("Could not find " + monitorJvmName);
                exitProcess();
            }
            final JbossConnectionInfo result = createJbossConnectionInfo(monitorJvmName);
            result.setURL(url);
            return result;
        }

    }

    public void loadConfig(final String e2eConfigFile) throws PluginException {
        final Properties configurationProperties = new Properties();
        try {
            configurationProperties.load(new FileReader(e2eConfigFile));
        } catch (IOException e) {
            LOGGER.debug("Failed to load props file " + e2eConfigFile, e);
            return;
        }
        for (final Enumeration<?> iterator = configurationProperties.propertyNames(); iterator.hasMoreElements();) {
            final String propertyName = (String) iterator.nextElement();
            LOGGER.debug("loadConfig: propertyName=" + propertyName);
            if (propertyName.startsWith("blacklist")) {
                final String blacklistEntryStr = configurationProperties.getProperty(propertyName);
                final String blacklistEntries[] = blacklistEntryStr.split(";");
                if (blacklistEntries.length != 2) {
                    throw new PluginException("Invalid blacklist entry: " + blacklistEntryStr);
                }
                List<String> blacklistForMbean = blacklist.get(blacklistEntries[0]);
                if (blacklistForMbean == null) {
                    blacklistForMbean = new LinkedList<>();
                    blacklist.put(blacklistEntries[0], blacklistForMbean);
                }
                blacklistForMbean.add(blacklistEntries[1]);
            } else if (propertyName.startsWith("searchlist")) {
                searchList.add(configurationProperties.getProperty(propertyName));
            } else if (propertyName.startsWith("pollinterval")) {
                final String pollintervalStr = configurationProperties.getProperty(propertyName);
                LOGGER.debug("loadConfig: processing pollinterval value=" + pollintervalStr);
                final String intervalBeanList[] = pollintervalStr.split("@");
                if (intervalBeanList.length != 2) {
                    throw new PluginException("Invalid pollinterval entry: " + intervalBeanList);

                }
                final Integer interval = Integer.parseInt(intervalBeanList[0]);
                for ( final String mbean : intervalBeanList[1].split(";") ) {
                        pollIntervals.put(mbean, interval);
                }
            }
        }
    }

    // Needed for unit tests
    public void setRunnableJbossInstanceToQuery(final JbossConnectionInfo runnableJbossInstanceToQuery) {
        this.runnableJbossInstanceToQuery = runnableJbossInstanceToQuery;
    }


    private JbossConnectionInfo createJbossConnectionInfo(final String jbossInstance) {
        final JbossConnectionInfo jbossConnectionInfo = new JbossConnectionInfo();
        jbossConnectionInfo.setInstanceName(jbossInstance);
        jbossConnectionInfo.setManagementUser("");
        jbossConnectionInfo.setManagementPassword("");
        return jbossConnectionInfo;
    }

    private void setJbossIPandPort(final JbossConnectionInfo jbossConnectionInfo) {
        String logDirPath=System.getProperty(Constants.LOGGING_DIRECTORY_PATH_SYS_ARGS);
        File configFilePath = new File(logDirPath+File.separator+"server"+File.separator+"jbossConfig");
        boolean propertySet=false;

        if(configFilePath.exists()){
            String ip="";
            String port="";
            try {
                Properties prop= loadProperties(configFilePath);
                ip=prop.getProperty("JB_MANAGEMENT");
                port=prop.getProperty("JB_MGT_NATIVE_PORT");
            } catch (IOException e) {
                LOGGER.error("jboss config file not loaded :"+configFilePath.toString());
            }
            if(!(ip.isEmpty() || port.isEmpty())){
                jbossConnectionInfo.setIpAddress(ip);
                jbossConnectionInfo.setManagementPort(port);
                propertySet=true;
            }
        }

        if (!propertySet){
            jbossConnectionInfo.setIpAddress(Constants.DEFAULT_JBOSS_MANAGEMENT_IP);
            jbossConnectionInfo.setManagementPort(Constants.DEFAULT_JBOSS_MANAGEMENT_NATIVE_PORT);
            LOGGER.warn("setting default values of JB_MANAGEMENT=" + Constants.DEFAULT_JBOSS_MANAGEMENT_IP + " and JB_MGT_NATIVE_PORT=" + Constants.DEFAULT_JBOSS_MANAGEMENT_NATIVE_PORT);
        }
    }

    protected JMXServiceURL createJMXServiceURL(final String jmxUrl) throws MalformedURLException {
        final JMXServiceURL serviceUrl = new JMXServiceURL(jmxUrl);
        return serviceUrl;
    }

    protected MBeanServerConnection createMBeanServerConnection(final JMXConnector connector) throws IOException {
        return connector.getMBeanServerConnection();
    }

    protected void createXMLForMbeans(final JbossConnectionInfo jbossConnectionInfo, final Set<ObjectInstance> mbeans) {
        final Map<ObjectInstance, MBeanInfo> defaultServiceMbeans =
                getServiceMbeans(jbossConnectionInfo, mbeans);
        if (!defaultServiceMbeans.isEmpty()) {
            final MbeanXmlWriter mbeanXmlWriter = new MbeanXmlWriter(jbossConnectionInfo, defaultServiceMbeans, blacklist, pollIntervals);
            mbeanXmlWriter.writeMbeanXml();
        }
    }

    protected void exitProcess() {
        System.exit(1);
    }

    protected Set<ObjectInstance> getMbeans(final JbossConnectionInfo jbossConnectionInfo) {
        Set<ObjectInstance> mbeans = null;
        try {
            mbeans = jbossConnectionInfo.getMbeanServerConnection().queryMBeans(null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mbeans;
    }

    protected Properties getProperties(final File credentialFile) {

        Properties jbossProperties = null;

        try {
            jbossProperties = loadProperties(credentialFile);
        } catch (FileNotFoundException e) {
            LOGGER.error(
                         "Failed while loading the credentials from the environment file for " + credentialFile.getName()
                         + ", Hopefully file not exists in system. Continue with other files..", e);
        } catch (IOException e) {
            LOGGER.error(
                         "Failed while creating the properties file from the credential file " + credentialFile.getName()
                         + ". Check the file format", e);
        }
        return jbossProperties;

    }

    protected Map<ObjectInstance, MBeanInfo> getServiceMbeans(final JbossConnectionInfo jbossConnectionInfo,
                                    final Set<ObjectInstance> mbeans) {
        final Map<ObjectInstance, MBeanInfo> serviceMbeans = new HashMap<>();
        LOGGER.debug("getServiceMbeans(): E2eDriver.isDisplayNameRequired=" + E2eDriver.isDisplayNameRequired());
        for (final ObjectInstance mbean : mbeans) {
            if (mbean.getObjectName().getDomain().startsWith("com.ericsson")) {
                LOGGER.debug("getServiceMbeans(): checking mbean=" + mbean);
                try {
                    final MBeanInfo mbeanInfo = jbossConnectionInfo.getMbeanServerConnection().getMBeanInfo(mbean.getObjectName());
                    // If it's a non-JBoss JVM then add all com.ericsson
                    if ( E2eDriver.isDisplayNameRequired() ) {
                        final List<String> mbeanFieldNames = Arrays.asList(mbeanInfo.getDescriptor().getFieldNames());
                        if ( LOGGER.isDebugEnabled() ) {
                            LOGGER.debug("getServiceMbeans(): mbeanFieldNames=" + mbeanFieldNames);
                        }
                        if ( mbeanFieldNames.contains("DisplayName") ) {
                            serviceMbeans.put(mbean, mbeanInfo);
                        }
                    } else {
                        serviceMbeans.put(mbean, mbeanInfo);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed while processing " + mbean, e);
                }
            }
        }
        return serviceMbeans;
    }

    protected Properties loadProperties(final File credentialFile) throws IOException, FileNotFoundException {
        final Properties jbossProperties = new Properties();
        if (credentialFile != null) {
            jbossProperties.load(new FileInputStream(credentialFile));
        }

        return jbossProperties;
    }

    MbeanTextFileWriter createMbeanTextFileWriter(final List<String> matches) {
        return new MbeanTextFileWriter(runnableJbossInstanceToQuery, matches);
    }
}
