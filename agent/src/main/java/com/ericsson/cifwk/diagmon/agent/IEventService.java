package com.ericsson.cifwk.diagmon.agent;

public interface IEventService {
    /**
     * Send a single event.
     *
     * @param timeInMilliseconds
     * @param name
     * @param description
     * @param value
     */
    public void sendEvent(long timeInMilliseconds,
            String name,
            String description,
            String value);


    /**
     * Register a JMX MBean parameter to poll periodically.
     *
     * @param jmxBeanObjectName the name of the MBean object to collect from
     * @param paramName the name of the parameter to collect
     * @param description description of the parameter
     * @param collectionPeriod the interval in seconds between collections
     */
    public void registerMetric(String jmxBeanObjectName,
            String paramName,
            String description,
            int collectionPeriod);
}
