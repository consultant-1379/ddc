package com.ericsson.cifwk.diagmon.agent.eventservice;

public interface MetricPollerMBean {
    public abstract String getObjectName();
    public abstract String getValue();
    public abstract String getDescription();
    int getPollIntervalSeconds();
    void setPollIntervalSeconds(int secs);
}
