package com.ericsson.cifwk.diagmon.agent.eventhandlers.rmi;

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
import com.ericsson.cifwk.diagmon.agent.rmiserver.RMIAgentServer;

public class EventHandlerImpl extends Thread implements EventHandler {
    private BlockingQueue<Event> eventSource;
    private List<Event> toSend;
    private RMIAgentServer server;
    
    private boolean running = true;

    public EventHandlerImpl() {
        toSend = new ArrayList<Event>();
        // We need to be a daemon so the application will exit properly
        this.setDaemon(true);
        start();
    }

    public void run() {
        while (true) {
            if (! isRunning() || eventSource == null) {
                try {Thread.sleep(250);} catch (InterruptedException e) {}
                continue;
            }
            if (toSend.size() >= Config.getInstance().getEventBatchSize()) {
                try {
                    sendBatch();
                } catch (RemoteException e) {
                    if (Logger.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    // XXX: not very clean ...
                    server = null;
                    if (! isRunning()) return;
                    Logger.warning("Server has gone, sleeping for " +
                            (Config.getInstance().getFailureSleepTime() / 1000) + " seconds");
                    chillout();
                }
            }
            // get another event, waiting if required
            Logger.debug("Waiting for events ...");
            try {
                Event evt = eventSource.take();
                toSend.add(evt);
            } catch (InterruptedException e) {
                Logger.warning("RMIEventForwarder interrupted: " + e.getMessage());
            }
        }
    }
    
    private synchronized void sendBatch() throws RemoteException {
        RMIAgentServer svr = getServer();
        if (svr != null) {
            Logger.debug("Sending " + toSend.size() + " events");
            svr.sendEventBatch(toSend);
            toSend.clear();
        }
    }

    private RMIAgentServer getServer() {
        if (server == null) {
            Logger.debug("Establishing RMI connection to " +
                    Config.getInstance().getRmiHost() + ":" + Config.getInstance().getRmiPort() + "/" +
                    Config.getInstance().getRmiServiceName());
            try {
                Registry registry = LocateRegistry.getRegistry(
                        Config.getInstance().getRmiHost(), Config.getInstance().getRmiPort());
                server = (RMIAgentServer) registry.lookup(Config.getInstance().getRmiServiceName());
            } catch (RemoteException e) {
                Logger.debug("Remote exception connecting to " +
                        Config.getInstance().getRmiPort() + ": " + e.getMessage());
                chillout();
            } catch (NotBoundException e) {
                Logger.debug("NotBoundException connecting to " +
                        Config.getInstance().getRmiServiceName() + ": " + e.getMessage());
                chillout();
            }
        }
        return server;
    }
    
    private void chillout() {
        try {
            // during this time we will not be 
            // processing any further events
            Thread.sleep(Config.getInstance().getFailureSleepTime());
        } catch (InterruptedException iex) {
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
        try {
            sendBatch();
        } catch (RemoteException e) {
            
        }
    }

    public void setEventSource(BlockingQueue<Event> eventSource) {
        this.eventSource = eventSource;
    }
}
