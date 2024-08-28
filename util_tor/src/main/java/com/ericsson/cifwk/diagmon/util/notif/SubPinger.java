package com.ericsson.cifwk.diagmon.util.notif;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("PMD.DoNotUseThreads")
public class SubPinger implements Runnable
{
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(SubPinger.class);

    private final Map<Integer,IConsumer> m_Subs = new HashMap<Integer,IConsumer>();
    private final Map<IConsumer,ActivePinger> m_ActivePings = new HashMap<IConsumer,ActivePinger>();

    private final static SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");

    private int m_Key = 0;
    private Thread m_PingThreads[];
    private volatile boolean m_Stopped = false;
    private final long m_PingInterval;
    private final PrintStream m_PrintStream;

    private Iterator m_Itr;
    private long m_StartTime;

    public SubPinger( final PrintStream printStream, final long pingInterval) {
        m_PrintStream = printStream;
        m_PingInterval = pingInterval;
    }

    public void start() {
        m_Itr = m_Subs.values().iterator();
        m_StartTime = System.currentTimeMillis();

        int numThreads = 10;
        if ( m_Subs.size() < 10 ) {
            numThreads = m_Subs.size();
        }
        m_PingThreads = new Thread[numThreads];
        for ( int i = 0; i < numThreads; i++ ) {
            m_PingThreads[i] = new Thread( this, "SubPinger_" + i );
            m_PingThreads[i].start();
        }
    }

    public void run() {
        while ( !m_Stopped ) {
            final IConsumer cc = getConsumer();
            if ( cc != null ) {
                if ( startPing(cc) ) {
                    if ( m_Log.isDebugEnabled() ) { m_Log.debug("run: ping cc = " + cc); }

                    try {
                        cc.ping();
                    } catch ( Throwable t ) { //NOPMD
                        m_PrintStream.println(t);
                    } finally {
                        endPing(cc);
                    }
                    if ( m_Log.isDebugEnabled() ) { m_Log.debug("run: ping completed cc = " + cc); }
                }
            }
        }
    }

    public synchronized IConsumer getConsumer() {
        if ( m_Stopped ) {
            return null;
        }

        if ( m_Itr.hasNext() ) {
            return (IConsumer) m_Itr.next();
        } else {
            final long timeToPing = System.currentTimeMillis() - m_StartTime;
            if ( timeToPing < m_PingInterval ) {
				try {
					Thread.currentThread().sleep(m_PingInterval - timeToPing);
				} catch (InterruptedException e) {
					return null;
				}
			}

            final LinkedList subs = new LinkedList(m_Subs.values());
            m_Itr = subs.iterator();
            m_StartTime = System.currentTimeMillis();

            return (IConsumer)m_Itr.next();
        }
    }

    public int add( final IConsumer cc ) {
        synchronized (m_Subs) {
            m_Key++;
            m_Subs.put(m_Key, cc );

            return m_Key;
        }
    }

    public void remove( final int id ) {
        synchronized (m_Subs) {
            m_Subs.remove(id);
        }
    }

    public void stop() {
        m_Stopped = true;
        for ( int i = 0; i < m_PingThreads.length; i++ ) {
            m_PingThreads[i].interrupt();
        }

        for ( int i = 0; i < m_PingThreads.length; i++ ) {
            try {
                m_PingThreads[i].join(5000);
            } catch (InterruptedException e) {}
        }
    }

    private boolean startPing( final IConsumer cc ) {
        ActivePinger ap = null;
        synchronized ( m_ActivePings ) {
            ap = (ActivePinger)m_ActivePings.get( cc );
            if ( ap == null ) {
                m_ActivePings.put( cc, new ActivePinger() );
                return true;
            }
        }

        final StringBuffer msg = new StringBuffer();
        msg.append(m_DF.format(new Date()));
        msg.append(" ERROR Hanging ping in thread ");
        msg.append(ap.m_Thr.getName());
        msg.append(" since ");
        msg.append(m_DF.format(new Date(ap.m_StartPing)));
        msg.append(" for ");
        msg.append(cc);
        synchronized(m_PrintStream) {
            m_PrintStream.println(msg.toString());
        }

        ap.m_Thr.interrupt();

        return false;
    }

    private void endPing( final IConsumer cc ) {
        synchronized ( m_ActivePings ) {
            m_ActivePings.remove(cc);
        }
    }

    class ActivePinger {
        final Thread m_Thr;
        final long m_StartPing;

        ActivePinger() {
            m_Thr = Thread.currentThread();
            m_StartPing = System.currentTimeMillis();
        }
    }
}
