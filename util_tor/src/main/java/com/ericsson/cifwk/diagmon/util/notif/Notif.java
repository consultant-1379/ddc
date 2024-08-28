package com.ericsson.cifwk.diagmon.util.notif;

import java.security.KeyStore;
import java.util.*;
import java.io.*;

import org.omg.CORBA.ORB;
import com.ericsson.cifwk.diagmon.util.common.CLIOptions;
import com.ericsson.cifwk.diagmon.util.common.StructuredEventPrinter;

@SuppressWarnings({"PMD.DoNotUseThreads","PMD.DoNotCallSystemExit"})
public class Notif {
    private final Map<String,IConsumer> m_ConsumerMap = new HashMap<String,IConsumer>();
    private ORB orb;

    protected static final long SUB_PING_INTERVAL = 60000;
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(Notif.class);

    public static void main(final String args[]) {
        CLIOptions opts = null;
        // ADDING CLI BEGIN ========================================
        opts = new CLIOptions("notif", "Utility to subscribe internal notifications in ENM and/or notifications from nodes\n", "Usage Example: notif -config <configFile> -save <saveFile>      -maxtime <secs>");
        opts.addOption("config","specify configuration file for notification subscription Example format per line AM::::*","configFile");
        opts.addOption("save","To specify the resultant file","saveFile");
        opts.addOption("maxtime","To specify the time-out in seconds","secs");
        opts.addOption("port","An optional port number to listen by this utility","portNum");
        opts.addOption("terse","Print event in terse mode");
        opts.addOption("orbprops","Props file for the ORB", "propsFile");


        if(args.length == 0 || ! opts.parse(args)) {
            opts.printUsage();
            System.exit(0);
        }

        // ADDING CLI END =======================================
        String configFile = null;
        String saveFile = null;
        String propsFile = null;
        long endTime = -1;

        // ADDING CLI BEGIN ======================================
        if(opts.hasOption("help")) {
            opts.printUsage();
            System.exit(0);
        }

        com.ericsson.cifwk.diagmon.util.common.Logging.init();
        String tmpValue = opts.getValue("loglevel");
        if(tmpValue != null) {
            com.ericsson.cifwk.diagmon.util.common.Logging.setLevel(tmpValue);
        }

        tmpValue = opts.getValue("config");
        if(tmpValue != null) {
            configFile = tmpValue;
        }

        tmpValue = opts.getValue("save");
        if(tmpValue != null) {
            saveFile = tmpValue;
        }

        tmpValue = opts.getValue("orbprops");
        if(tmpValue != null) {
            System.out.println("orbprops=" + tmpValue);
            propsFile = tmpValue;
        }

        tmpValue = opts.getValue("maxtime");
        if(tmpValue != null) {
            try {
                endTime = System.currentTimeMillis() + ((60 * 60 * 1000) * Integer.parseInt(tmpValue));
            } catch(NumberFormatException nfe) {
                opts.printUsage();
                System.exit(1);
            }
        }



        // ADDING CLI END =======================================

        try {
            if(configFile == null ) {
                System.out.println("-config option expected");
                opts.printUsage();
                System.exit(1);
            }
            new Notif().run( configFile, saveFile, propsFile, endTime );
            System.exit(0);
        } catch ( Throwable e ) { // NOPMD
            e.printStackTrace();
            System.exit(1);
        }
    }


    private void run(final String configFile, final String saveFile, final String propsFile, final long endTime)
        throws Exception //NOPMD
    {
        if ( m_Log.isDebugEnabled() ) { m_Log.debug("run: entered configFile=" + configFile); }

        final Properties props = new Properties();
        if ( propsFile != null ) {
            props.load(new FileReader(propsFile));
        }
        orb = ORB.init(new String[0], props);
        final String keyStorePath = props.getProperty("kspath");
        if ( keyStorePath != null ) {
            final String keyStorePass = props.getProperty("kspass");
            final FileInputStream ins = new FileInputStream(keyStorePath);
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(ins, keyStorePass.toCharArray());
            final com.borland.security.provider.CertificateWallet wallet =
                    new com.borland.security.provider.CertificateWallet(keyStore, keyStorePass.toCharArray());
            final com.borland.security.Context ctx =
                    (com.borland.security.Context) orb.resolve_initial_references("VBSecurityContext");
            ctx.login(wallet);
        }

        final Runnable orbRunner = new Runnable() {
                public void run() {
                    orb.run();
                }
            };
        new Thread(orbRunner).start();

        PrintStream printOut = System.out;
        if ( saveFile != null ) {
            printOut = new PrintStream(new FileOutputStream(saveFile, true));
        }

        StructuredEventPrinter.init();

        final SubPinger pinger = new SubPinger( printOut, SUB_PING_INTERVAL);

        final NotifHandler notifHandler = new BufferedNotifHandler(printOut);

        setupSubs( configFile, notifHandler, pinger, printOut );

        pinger.start();

        if ( saveFile == null ) {
            final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter \"end\" to exit");
            String input = "";
            while ( !input.equals("end") ) {
                input = stdin.readLine();
            }
        } else {
            boolean shutdown = false;
            final File exitFile = new File(saveFile + ".exit");
            while ( !shutdown ) {
                Thread.sleep(10000);
                if (exitFile.exists()) {
                    shutdown = true;
                    exitFile.delete();
                } else if (endTime != -1) {
                    shutdown = System.currentTimeMillis() > endTime;
                }
            }
            printOut.println("Shutting down " + new Date());
        }

        for ( final Object consumer : m_ConsumerMap.values() ) {
            printOut.println("Detaching consumer");
            final IConsumer con = (IConsumer)consumer;
            con.detach();
        }

        printOut.println("Stopping pinger");
        pinger.stop();
        notifHandler.close();
    }

    private void setupSubs( final String configFile, final NotifHandler notifHandler,
                            final SubPinger pinger, final PrintStream printOut )
        throws Exception { // NOPMD
        if ( m_Log.isDebugEnabled() ) { m_Log.debug("setupSubs: entered"); }

        final LineNumberReader in = new LineNumberReader(new FileReader(configFile));
        String line;
        int subIndex = 0;
        while ( (line = in.readLine()) != null ) {
            IConsumer con = null;
            line = line.trim();

            if ( line.length() == 0 || line.startsWith("#") ) {
                continue;
            }

            if ( m_Log.isDebugEnabled() ) { m_Log.debug("setupSubs: line=" + line); }

            try {
                final String parts[] = line.split("@");
                if ( m_Log.isDebugEnabled() ) { m_Log.debug("setupSubs: parts=" + Arrays.toString(parts)); }
                final Iterator<String> tkn = Arrays.asList(parts).iterator();

                final String cat = tkn.next();
                if ( cat.equals("CELLO_CONFIG") ) {
                    final String celloHost = tkn.next();
                    final String moFilter[] = new String[0];
                    con = new CelloConfigConsumer(printOut, notifHandler, celloHost, pinger, moFilter, orb);
                } else {
                    final String filter = tkn.next();
                    con = new ThreegppConsumer(printOut, notifHandler, cat, filter, pinger, orb);
                }
                m_ConsumerMap.put(String.valueOf(subIndex), con );
                subIndex++;
            } catch ( Throwable e ) { //NOPMD
                System.out.println("Error with processing line " + in.getLineNumber() +
                                   ", " + e );
                e.printStackTrace(printOut);
            }
        }
        in.close();
    }
}

