package com.ericsson.cifwk.diagmon.agent.rmiserver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.ericsson.cifwk.diagmon.agent.eventservice.Event;

public interface RMIAgentServer extends Remote {
    public void sendEventBatch(List<Event> events) throws RemoteException;
}
