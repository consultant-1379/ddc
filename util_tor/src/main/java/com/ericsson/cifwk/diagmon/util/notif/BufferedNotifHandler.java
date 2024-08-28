package com.ericsson.cifwk.diagmon.util.notif;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ericsson.cifwk.diagmon.util.common.StructuredEventPrinter;

@SuppressWarnings("PMD.DoNotUseThreads")
public class BufferedNotifHandler extends NotifHandler implements RejectedExecutionHandler {
    private static SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");

    private final ThreadPoolExecutor m_Printers;
    private int overFlowCount = 0;

    protected static class PrintEvent implements Runnable {
        private final long timestamp;
        private final org.omg.CosNotification.StructuredEvent se;
        private final String source;
        private final PrintStream out;

        public PrintEvent(  final PrintStream out, final org.omg.CosNotification.StructuredEvent se,
                            final String source, final long timestamp ) {
            this.out = out;
            this.se = se;
            this.source = source;
            this.timestamp = timestamp;
        }

        public void run() {
            StructuredEventPrinter.printEvent( out, se, source, timestamp );
        }
    }

    public BufferedNotifHandler(final PrintStream printOut) throws Exception { //NOPMD
        super(printOut);

        final String qSizePropName = BufferedNotifHandler.class.getName() + ".qsize";
        final int qSize = Integer.parseInt(System.getProperty(qSizePropName, "5000"));
        final BlockingQueue<Runnable> q = new LinkedBlockingQueue<Runnable>(qSize); //NOPMD

        final String threadPropName = BufferedNotifHandler.class.getName() + ".threads";
        final int numThreads = Integer.parseInt(System.getProperty(threadPropName, "2"));
        m_Printers = new ThreadPoolExecutor( numThreads, numThreads, 0, TimeUnit.SECONDS, q );
    }

    protected void printStructEvent( final org.omg.CosNotification.StructuredEvent se, final String source,
                                     final long timestamp ) {
        m_Printers.execute(new PrintEvent(m_PrintOut, se, source, timestamp));
    }

    public synchronized void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) { //NOPMD
        overFlowCount++;
        if ( (overFlowCount % 100) == 1 ) {
            synchronized(m_PrintOut) {
                m_PrintOut.println(m_DF.format(new Date()) + " ERROR overFlowCount=" + overFlowCount);
                m_PrintOut.println();
            }
        }
    }

    protected void close() {
        m_Printers.shutdownNow();
    }
}

