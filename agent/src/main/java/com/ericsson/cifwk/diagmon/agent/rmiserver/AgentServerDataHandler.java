package com.ericsson.cifwk.diagmon.agent.rmiserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.Event;

class AgentServerDataHandler extends Thread {
    private PrintStream out;
    private List<Event> toWrite;
    private boolean doShutdown = false;
    
    AgentServerDataHandler(String logFile) {
        if (logFile != null) {
            File outputFile = new File(logFile);
            try {
                if (! outputFile.createNewFile()) {
                    // file already exists - is it writable?
                    if (! outputFile.canWrite()) {
                        Logger.fatal("Cannot write to logfile: " + logFile);
                        outputFile = null;
                    }
                }
            } catch (IOException e) {
                Logger.fatal("Cannot create logfile: " + logFile + ": " + e.getMessage());
                outputFile = null;
            }
            if (outputFile != null) {
                try {
                    out = new PrintStream(new FileOutputStream(outputFile, true));
                } catch (FileNotFoundException e) {
                    Logger.fatal("Cannot open printstream to logfile: " +
                            logFile + ": " + e.getMessage());
                    out = null;
                }
            }
        } else {
            out = System.out;
        }

        toWrite = new ArrayList<Event>();
    }
    
    synchronized void submitEvents(List<Event> evtList) {
        toWrite.addAll(evtList);
    }
    
    void shutdown() {
        doShutdown = true;
    }
    
    public void run() {
        if (out == null) return;
        do {
            while (toWrite.size() > 0) {
                // XXX: probably inefficient?!
                out.println(toWrite.remove(0));
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        } while (! doShutdown);
    }
}
