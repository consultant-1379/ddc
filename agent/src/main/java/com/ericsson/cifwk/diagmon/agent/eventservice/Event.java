package com.ericsson.cifwk.diagmon.agent.eventservice;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Iterator;

import com.ericsson.cifwk.diagmon.agent.common.Time;

public class Event implements Serializable {

    private static final long serialVersionUID = -1344509057932199683L;
    private long timeInMilliseconds;
    private String name;
    private String description;
    private String value;
    private String id; // identifier for event source
    private static String globalId;
    
    static {
        // the ericsson standard "-Ds=" is the default name
        String name = System.getProperty("s");
        // otherwise, are we using a binary wrapper?
        if (name == null) {
            name = (new java.io.File(System.getenv("_")).getName());
        }
        // otherwise, we really don't know ...
        if (name == null) name = "unknown";
        globalId = System.getProperty("user.name") + "-" +
            ManagementFactory.getRuntimeMXBean().getName() + "@" + name + "-" +
            System.currentTimeMillis();
    }
    
    public void setTimeInMilliseconds(long timeInMilliseconds) {
        this.timeInMilliseconds = timeInMilliseconds;
    }
    public long getTimeInMilliseconds() {
        return timeInMilliseconds;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    
    Event(long timeInMilliseconds,
            String name, String description, String value) {
        this.timeInMilliseconds = timeInMilliseconds;
        this.name = name;
        this.description = description;
        this.value = value;
        this.id = globalId;
    }
    
    public String toString() {
        return Time.getTimestamp(this.timeInMilliseconds) +
            ";" + this.id +
            ";" + this.name + ";" + this.value +
            ";" + this.description;
    }
}
