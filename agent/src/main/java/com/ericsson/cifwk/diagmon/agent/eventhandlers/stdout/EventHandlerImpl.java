package com.ericsson.cifwk.diagmon.agent.eventhandlers.stdout;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.ericsson.cifwk.diagmon.agent.common.Config;
import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.Event;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventHandler;

public class EventHandlerImpl extends Thread implements EventHandler {
    private BlockingQueue<Event> eventSource;
    
    private boolean running = true;

    public EventHandlerImpl() {
        // We need to be a daemon so the application will exit properly
        this.setDaemon(true);
        start();
    }

    public void run() {
        while (true) {
            if (! isRunning() || eventSource == null) {
                Logger.debug("stdout event handler is stopped");
                try {Thread.sleep(250);} catch (InterruptedException e) {}
                continue;
            }
            Logger.debug("Waiting for events ...");
            try {
                Event evt = eventSource.take();
                System.out.println(evt.toString());
            } catch (InterruptedException e) {
                Logger.warning("stdout event handler interrupted: " + e.getMessage());
            }
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void disable() {
        setRunning(false);
    }

    public void enable() {
        setRunning(true);
    }

    public BlockingQueue<Event> getEventSource() {
        return this.eventSource;
    }

    public boolean isEnabled() {
        return running;
    }
    
    public void flush() {
        // do nothing here
    }

    public void setEventSource(BlockingQueue<Event> eventSource) {
        this.eventSource = eventSource;
    }
}
