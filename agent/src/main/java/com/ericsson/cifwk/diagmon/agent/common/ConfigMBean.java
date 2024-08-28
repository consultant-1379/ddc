package com.ericsson.cifwk.diagmon.agent.common;

public interface ConfigMBean {

    public abstract int getRmiPort();

    public abstract void setRmiPort(int portNum);

    public abstract String getRmiServiceName();

    public abstract void setRmiServiceName(String serviceName);

    public abstract String getRmiHost();

    public abstract void setRmiHost(String rmiHost);

    public abstract int getEventBatchSize();

    public abstract void setEventBatchSize(int eventBatchSize);

    public abstract int getFailureSleepTime();

    public abstract void setFailureSleepTime(int failureSleepTime);

    public abstract int getQueueLength();

    public abstract void setQueueLength(int queueLength);

    public abstract int getDefaultPollIntervalSeconds();

    public abstract void setDefaultPollIntervalSeconds(
            int defaultPollIntervalSeconds);

    public abstract String getEventHandlerType();
    
    public abstract void setEventHandlerType(String type);
}
