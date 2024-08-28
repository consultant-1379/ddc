package com.ericsson.cifwk.diagmon.util.jms;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import javax.jms.Session;
import javax.jms.Message;

public class JmsClient {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        try {
            new JmsClient().run(args);
            System.exit(0);
        } catch ( Throwable t ) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public void run(String args[]) throws Exception {
        com.ericsson.cifwk.diagmon.util.common.Logging.init();


        final Options options = new Options();
        options.addOption("d",true,"dps-api jar file path");
        options.addOption("output",true,"Output file path");
        options.addOption("exit",true,"Exit file path");
        options.addOption("topic",true,"Topic to listen to");
        options.addOption("maxtime",true,"Maximum execution time");
        final CommandLineParser parser = new GnuParser();
        final CommandLine line = parser.parse( options, args );
        
        PrintWriter output = null;
        if( line.hasOption( "output" ) ) {
            output = new PrintWriter(new FileWriter(line.getOptionValue( "output" ),true));
        } else {
            output = new PrintWriter(System.out);
        }

        File exitFile = null;
        if ( line.hasOption("exit") ) {
            exitFile = new File(line.getOptionValue("exit"));
        }

        long maxTime = 0;
        if ( line.hasOption("maxtime") ) {
            maxTime = System.currentTimeMillis() +
                (Integer.parseInt(line.getOptionValue("maxtime")) * 1000);
        }

        String topicName = "dps-notification-event";
        if( line.hasOption( "topic" ) ) {
        	topicName = line.getOptionValue("topic");
        }
        
        boolean isActiveMQ = false;
        try {
            Class.forName("org.wildfly.naming.client.WildFlyInitialContextFactory");
            isActiveMQ = true;
        } catch( ClassNotFoundException ignored ) {}


        final Properties env = new Properties();
        String connectionFactoryName = "jms/RemoteConnectionFactory";
        if ( isActiveMQ ) {
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            env.put(Context.PROVIDER_URL, "http-remoting://jms01:8080");
            env.put(Context.SECURITY_PRINCIPAL, "hqcluster");
            env.put(Context.SECURITY_CREDENTIALS, "3ric550N!");
            connectionFactoryName = connectionFactoryName + "-amq";
            topicName = topicName + "-amq";
        } else {
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            env.put(Context.PROVIDER_URL, "remote://jms01:4447");
            env.put(Context.SECURITY_PRINCIPAL, "hqcluster");
            env.put(Context.SECURITY_CREDENTIALS, "3ric550N!");
        }
        Context ctx = new InitialContext(env);
        final ConnectionFactory cf = (ConnectionFactory) ctx.lookup(connectionFactoryName);
        final Connection connection = cf.createConnection();
        connection.start();

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Topic topic = (Topic)ctx.lookup("jms/topic/" + topicName);
        final MessageConsumer consumer = session.createConsumer(topic);

        final AtomicBoolean exitFlag = new AtomicBoolean(false);
        final ExitChecker exitChecker = new ExitChecker(exitFile,maxTime,exitFlag,output);
        new Thread(exitChecker,"ExitChecker").start();

        while ( ! exitFlag.get() ) {
            final Message msg = consumer.receive(5000);
            if ( msg != null ) {
                printMessage(output,msg);
            }
        }

        consumer.close();
        session.close();
        connection.close();
    }

    class ExitChecker implements Runnable {
        private final File exitFile;
        private final long maxTime;
        private final AtomicBoolean exitFlag;
        private final PrintWriter output;

        ExitChecker(File exitFile, long maxTime, AtomicBoolean exitFlag, PrintWriter output) {
            this.exitFile = exitFile;
            this.maxTime = maxTime;
            this.exitFlag = exitFlag;
            this.output = output;
        }

        public void run() {
            try {
                while ( ! exitFlag.get() ) {
                    if ( exitFile != null ) {
                        if ( exitFile.exists() ) {
                            exitFile.delete();
                            exitFlag.set(true);
                        }
                    }

                    if ( maxTime > 0 ) {
                        if ( System.currentTimeMillis() > maxTime ) {
                            exitFlag.set(true);
                        }
                    }

                    if ( ! exitFlag.get() ) {
                        Thread.sleep(10000);
                        output.flush();
                    }
                }
            } catch ( Throwable t ) {
                t.printStackTrace();
                exitFlag.set(true);
            }
        }
    }

    private void printMessage(PrintWriter output, Message msg) throws JMSException {
        long jmsTimeStamp = msg.getJMSTimestamp();
        output.println(sdf.format(new Date(jmsTimeStamp)) + " " + (System.currentTimeMillis()-jmsTimeStamp));
        //              for ( Enumeration itr = msg.getPropertyNames(); itr.hasMoreElements(); ) {
        //                      String propName = (String)itr.nextElement();
        //                      System.out.println(propName + "=" + msg.getStringProperty(propName));
        //              }
        if ( msg instanceof ObjectMessage ) {
            ObjectMessage oMsg = (ObjectMessage)msg;
            output.println(oMsg.getObject());
        } else {
            output.println(msg.getClass().getName());
        }
        output.println();
    }
}
