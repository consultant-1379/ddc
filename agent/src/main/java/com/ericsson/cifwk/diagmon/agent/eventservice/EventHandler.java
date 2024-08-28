package com.ericsson.cifwk.diagmon.agent.eventservice;

import java.util.concurrent.BlockingQueue;

public interface EventHandler {
    /**
     * Enable the event handler.
     */
    public void enable();
    
    /**
     * Disable the event handler.
     */
    public void disable();
    
    /**
     * Determine the state of the handler
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled();
    
    /**
     * Force this event handler to deal with any pending events.
     */
    public void flush();
    
    /**
     * Set the source of events for this event handler
     * @param eventSource - the source of the events
     */
    public void setEventSource(BlockingQueue<Event> eventSource);
    
    /**
     * Get the queue from which this event handler expects to receive events. This
     * should be the same object as passed in using setEventSource.
     * @return the event queue
     */
    public BlockingQueue<Event> getEventSource();
}
