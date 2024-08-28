package com.ericsson.cifwk.diagmon.agent.rmiserver;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import com.ericsson.cifwk.diagmon.agent.common.Config;
import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.Event;

public class AgentServer implements RMIAgentServer {
    private int eventCount = 0;
    private AgentServerDataHandler dh;
    private File exitFile;
    
    public void sendEventBatch(List<Event> events) throws RemoteException {
        eventCount += events.size();
        Logger.debug("# got a batch of " + events.size() + " - total " + eventCount);
        dh.submitEvents(events);
    }
    
    public AgentServer(String outFile, String exitFile, int maxtime) {
        dh = new AgentServerDataHandler(outFile);
        // HP23510 - Set timeLeft var, convert from hours to milliseconds [2012-01-26 eronkeo]
        int timeLeft = maxtime * 3600000;
        try {
            Registry r = LocateRegistry.createRegistry(Config.getInstance().getRmiPort());
            RMIAgentServer stub =
                (RMIAgentServer)UnicastRemoteObject.exportObject(this, Config.getInstance().getRmiPort());
            r.rebind("AgentServer", stub);
            System.out.println("bound");
            dh.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (exitFile != null) {
            this.exitFile = new File(exitFile);
        } else {
            this.exitFile = new File("agentserver.exit");
        }
        if (this.exitFile.exists()) {
            if (! this.exitFile.delete()) {
                Logger.error("Cannot delete exit file: " + this.exitFile);
            }
        }

        // HP23510 - Add check to exit after maxtime exceeded [2012-01-26 eronkeo]
        while ((! this.exitFile.exists()) && (timeLeft > 0)) {
            try {
                // Sleep for 1 second
                Thread.sleep(1000);
                timeLeft -= 1000;  
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Logger.debug("Exit file " + this.exitFile + " exists");
        this.exitFile.delete();
        dh.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        // Parse command line options
        // XXX: don't want to introduce dependencies here
        /*
        CLIOptions opts = new CLIOptions("agentserver",
                "Daemon to receive and store Java agent logs\n",
                "Usage Example: agentserver -out <dataFile> -exit <exitFile> -maxtime <hrs> ");
        opts.addOption("out", "Data file to write to", "datafile");
        opts.addOption("exit", "Exit file - if this file exists the service will terminate",
                "exitfile");
        opts.addOption("maxtime", "The maximum time to run for", "time in hours");
        if(args.length == 0 || ! opts.parse(args)) {
            opts.printUsage();
            Runtime.getRuntime().exit(1);
        }
        String outFile = opts.getValue("out");
        String exitFile = opts.getValue("exit");
        int maxtime = opts.getIntValue("maxtime");
        */
        String outFile = null;
        if (args.length >= 1) outFile = args[0];
        String exitFile = null;
        if (args.length >= 2) exitFile = args[1];
        int maxtime = 0;
        if (args.length >= 3) {
            maxtime = Integer.parseInt(args[2]);
        }
        Logger.debug("Starting agent server with outfile " + outFile + ";" +
                "exitfile " + exitFile + "; maxtime " + maxtime);
        RMIAgentServer s = new AgentServer(outFile, exitFile, maxtime);
    }
}
