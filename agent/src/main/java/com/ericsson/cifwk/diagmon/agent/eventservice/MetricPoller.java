package com.ericsson.cifwk.diagmon.agent.eventservice;

public abstract class MetricPoller extends Thread implements MetricPollerMBean {
    private int pollIntervalSeconds = 5;

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }
    
    public void setPollIntervalSeconds(int secs) {
        this.pollIntervalSeconds = secs;
    }
    
    public void run() {
        EventService.get().sendEvent(System.currentTimeMillis(),
                getObjectName(),getDescription(), getValue());
    }
}
