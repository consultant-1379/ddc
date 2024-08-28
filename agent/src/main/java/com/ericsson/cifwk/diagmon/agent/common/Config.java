package com.ericsson.cifwk.diagmon.agent.common;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class Config implements ConfigMBean {
    private MBeanServer mbs = null;
    
    /* 
     * port number in OSS ports list
     * http://cdmweb.ericsson.se/WEBLINK/ViewDocs?DocumentName=EAB%2FPJF-06%3A0013&Latest=true
     */
    private int rmiPort = 56999;
    private String rmiHost = "localhost";
    private String rmiServiceName = "AgentServer";
    private String eventHandlerType = "rmi";
    
    private Map<String,String> configValues =
        Collections.synchronizedMap(new HashMap<String,String>());
    
    private static Config instance;

    /*
     * Number of events to batch before forwarding to the RMI service
     */
    private int eventBatchSize = 10;

    /*
     * maximum number of events that will be buffered before they start to be
     * discarded
     */
    private int queueLength = 50;
    
    /*
     * Length of time to sleep if RMI service cannot be contacted
     */
    private int failureSleepTime = 60000;
    
    /*
     * Interval for polling default JMX MBeans
     */
    private int defaultPollIntervalSeconds = 60;
    
    /*
     * Private constructor - there can be only one!
     */
    private Config() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = new ObjectName("com.ericsson.cifwk.diagmon.agent:name=Config");
            mbs.registerMBean(this, name);
        } catch (MalformedObjectNameException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (NullPointerException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            if (Logger.isDebugEnabled()) e.printStackTrace();
        }
    }

    private String getConfigValue(String env, String prop) {
        if (configValues.containsKey(prop)) {
            return configValues.get(prop);
        }
        String value = System.getenv(env);
        if (value == null) {
            value = System.getProperty(prop);
        }
        // set the value in our configValues map
        configValues.put(prop, value);
        return value;
    }
    
    private String getConfigValue(String env, String prop, String defaultVal) {
        String userDef = getConfigValue(env, prop);
        if (userDef == null) return defaultVal;
        return userDef;
    }
    
    private int getIntConfigValue(String env, String prop, int defaultVal) {
        try {
            return Integer.parseInt(getConfigValue(env, prop));
        } catch (NumberFormatException e) {
            
        }
        return defaultVal;
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getRmiPort()
     */
    public int getRmiPort() {
        return getIntConfigValue(
                "DDC_RMI_PORT",
                "com.ericsson.cifwk.diagmon.agent.rmiPort",
                rmiPort);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setRmiPort(int)
     */
    public void setRmiPort(int portNum) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.rmiPort", portNum + "");
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getRmiServiceName()
     */
    public String getRmiServiceName() {
        return getConfigValue(
                "DDC_RMI_SERVICE_NAME",
                "com.ericsson.cifwk.diagmon.agent.rmiServiceName",
                rmiServiceName
        );
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setRmiServiceName(java.lang.String)
     */
    public void setRmiServiceName(String serviceName) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.rmiServiceName", serviceName);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getRmiHost()
     */
    public String getRmiHost() {
        return getConfigValue(
                "DDC_RMI_HOST",
                "com.ericsson.cifwk.diagmon.agent.rmiHost",
                rmiHost
        );
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setRmiHost(java.lang.String)
     */
    public void setRmiHost(String rmiHost) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.rmiHost", rmiHost);
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getEventBatchSize()
     */
    public int getEventBatchSize() {
        return getIntConfigValue(
                "DDC_EVENT_BATCH_SIZE",
                "com.ericsson.cifwk.diagmon.agent.eventBatchSize",
                eventBatchSize);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setEventBatchSize(int)
     */
    public void setEventBatchSize(int eventBatchSize) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.eventBatchSize", eventBatchSize + "");
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getFailureSleepTime()
     */
    public int getFailureSleepTime() {
        return getIntConfigValue(
                "DDC_FAILURE_SLEEP_TIME",
                "com.ericsson.cifwk.diagmon.agent.failureSleepTime",
                failureSleepTime);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setFailureSleepTime(int)
     */
    public void setFailureSleepTime(int failureSleepTime) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.failureSleepTime", failureSleepTime + "");
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getQueueLength()
     */
    public int getQueueLength() {
        return getIntConfigValue(
                "DDC_QUEUE_LENGTH",
                "com.ericsson.cifwk.diagmon.agent.queueLength",
                queueLength);
    }
    
    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setQueueLength(int)
     */
    public void setQueueLength(int queueLength) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.queueLength", queueLength + "");
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#getDefaultPollIntervalSeconds()
     */
    public int getDefaultPollIntervalSeconds() {
        return getIntConfigValue(
                "DDC_DEFAULT_POLL_INTERVAL_SECONDS",
                "com.ericsson.cifwk.diagmon.agent.defaultPollIntervalSeconds",
                defaultPollIntervalSeconds);
    }

    /* (non-Javadoc)
     * @see com.ericsson.cifwk.diagmon.agent.common.ConfigMBean#setDefaultPollIntervalSeconds(int)
     */
    public void setDefaultPollIntervalSeconds(int defaultPollIntervalSeconds) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.defaultPollIntervalSeconds",
                defaultPollIntervalSeconds + "");
    }
    
    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    public String getEventHandlerType() {
        return getConfigValue("DDC_EVENT_HANDLER_TYPE",
                "com.ericsson.cifwk.diagmon.agent.eventHandlerType",
                eventHandlerType);
    }

    public void setEventHandlerType(String type) {
        configValues.put("com.ericsson.cifwk.diagmon.agent.eventHandlerType", type);
    }
}
