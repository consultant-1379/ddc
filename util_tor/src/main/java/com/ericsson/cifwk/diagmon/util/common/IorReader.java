package com.ericsson.cifwk.diagmon.util.common;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD.DoNotUseThreads")
public class IorReader {
    protected static final String GET_CMD_V4 = "GET /cello/ior_files/nameroot.ior HTTP/1.0\r\n\r\n";
    protected static final String GET_CMD_V6 = "GET /cello/ior_files/ipv6_nameroot.ior HTTP/1.0\r\n\r\n";
    protected static IorReader m_Instance;
    private static SSLSocketFactory sslSocketFactory;

    final private ThreadPoolExecutor requestExecutor;
    final private BlockingQueue<Runnable> requestQueue;
    protected Map m_TimedOutMap;

    private static final int NOT_STARTED = 0;
    private static final int RUNNING = 1;
    private static final int SUCCESS = 2;
    private static final int FAILED = 3;
    private static final int TIMED_OUT = 4;

    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(IorReader.class);

    public static synchronized IorReader getInstance() {
        if (m_Instance == null) {
            m_Instance = new IorReader();
        }

        return m_Instance;
    }

    protected IorReader() {
        requestQueue = new LinkedBlockingQueue<>();
        requestExecutor = new ThreadPoolExecutor(
                0,
                20,
                1,
                TimeUnit.MINUTES,
                requestQueue
        );
        m_TimedOutMap = new HashMap();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public String getIOR(final String host, final long timeout) throws Exception {
        if (m_Log.isDebugEnabled()) {
            m_Log.debug("getIOR() host = " + host + ", timeout(ms) = " + timeout);
        }

        // Don't allow a request to a host that is still hanging
        synchronized (m_TimedOutMap) {
            if (m_TimedOutMap.containsKey(host)) {
                throw new Exception("Request for " + host + " already hanging");
            }
        }

        final GetIorJob job = new GetIorJob(host);
        requestExecutor.submit(job);
        return job.getResult(timeout);
    }

    protected void addTimedOutHost(final String host) {
        synchronized (m_TimedOutMap) {
            Integer timeOutCount = (Integer) m_TimedOutMap.get(host);
            if (m_Log.isDebugEnabled()) {
                m_Log.debug("addTimedOutHost() host = " + host + " timeOutCount = " + timeOutCount);
            }

            if (timeOutCount == null) {
                timeOutCount = new Integer(1);
            } else {
                timeOutCount = new Integer(timeOutCount.intValue() + 1);
            }
            m_TimedOutMap.put(host, timeOutCount);
        }
    }

    protected void removeTimedOutHost(final String host) {
        synchronized (m_TimedOutMap) {
            Integer timeOutCount = (Integer) m_TimedOutMap.remove(host);
            if (m_Log.isDebugEnabled()) {
                m_Log.debug("removeTimedOutHost() host = " + host + " timeOutCount = " + timeOutCount);
            }
            if (timeOutCount != null) {
                if (timeOutCount.intValue() > 1) {
                    timeOutCount = new Integer(timeOutCount.intValue() - 1);
                    m_TimedOutMap.put(host, timeOutCount);
                }
            }
        }
    }

    class GetIorJob implements Runnable {
        final String m_Host;
        Socket m_Socket;

        int m_state = NOT_STARTED;
        String m_Result;

        public GetIorJob(final String host) {
            m_Host = host;
        }

        @Override
        public void run() {
            try {
                setState(RUNNING);

                String GET_CMD = GET_CMD_V4;
                final InetAddress address = InetAddress.getByName(m_Host);
                if (address instanceof Inet6Address) {
                    GET_CMD = GET_CMD_V6;
                }

                if (m_Log.isDebugEnabled()) {
                    m_Log.debug("GetIorJob.run() Opening socket to host " + m_Host);
                }

                // First try https
                m_Socket = openSocket(address, true);
                if ( m_Socket == null ) {
                    // Now try http
                    m_Socket = openSocket(address, false);
                }
                if ( m_Socket == null) {
                    setResult(false, "Failed to open socket");
                    return;
                }

                if (m_Log.isDebugEnabled()) {
                    m_Log.debug("GetIorJob.run() Sending GET request host=" + m_Host + ", cmd=" + GET_CMD);
                }
                m_Socket.getOutputStream().write(GET_CMD.getBytes());

                final BufferedReader in = new BufferedReader(new InputStreamReader(m_Socket.getInputStream()));
                if (m_Log.isDebugEnabled()) {
                    m_Log.debug("GetIorJob.run() Reading header from host " + m_Host);
                }
                final String httpResult = in.readLine();
                if (m_Log.isDebugEnabled()) {
                    m_Log.debug("GetIorJob.run() Header from host " + m_Host + " is \"" + httpResult + "\"");
                }
                if (httpResult == null) {
                    setResult(false, "Stream closed while reading header");
                } else if (httpResult.indexOf("200 OK") >= 0) {
                    do {
                    } while (in.readLine().length() > 0);
                    final String ior = in.readLine();
                    if (m_Log.isDebugEnabled()) {
                        m_Log.debug("GetIorJob.run() IOR from host " + m_Host + " is \"" + ior + "\"");
                    }
                    setResult(true, ior);
                } else {
                    setResult(false, httpResult);
                }
            } catch (Exception e) {
                if (m_Log.isInfoEnabled()) {
                    m_Log.info("GetIorJob.run() Exception processing " + m_Host, e);
                }
                if (m_state != TIMED_OUT) {
                    setResult(false, e.toString());
                }
            } finally {
                closeSocket();
                if (m_state == TIMED_OUT) {
                    removeTimedOutHost(m_Host);
                }
            }
        }

        private Socket openSocket(final InetAddress ip, final boolean secure) {
            m_Log.debug("GetIorJob.openSocket() ip=" + ip + ", secure=" + secure);

            try {
                if (secure) {

                    // Install the all-trusting trust manager
                    final SSLSocketFactory factory = getSSLSocketFactory();
                    final SSLSocket socket = (SSLSocket) factory.createSocket(ip, 443);
                    socket.startHandshake();
                    return socket;
                } else {
                    return new Socket(ip, 80);
                }
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
                m_Log.debug("GetIorJob.openSocket() ip=" + ip, e);
                return null;
            }
        }

        synchronized void closeSocket() {
            if (m_Socket != null) {
                try {
                    m_Socket.close();
                } catch (Exception e) {}
                m_Socket = null;
            }
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        public synchronized String getResult(final long timeout) throws Exception {
            // Wait until a thread enters run
            while (m_state == NOT_STARTED) {
                this.wait();
            }

            // Now wait until the request finishes or times out
            this.wait(timeout);

            if (m_Log.isDebugEnabled()) {
                m_Log.debug("GetIorJob.getResult() host " + m_Host +
                        " m_state " + m_state +
                        ", m_Result = " + m_Result);
            }
            if (m_state == SUCCESS) {
                return m_Result;
            } else {
                // If we timed out
                if (m_Result == null) {
                    setState(TIMED_OUT);
                    addTimedOutHost(m_Host);
                    closeSocket();
                    throw new Exception("Timed out");
                } else {
                    return null; // throw new Exception(m_Result);
                }
            }
        }

        private synchronized void setResult(final boolean success, final String result) {
            if (m_state == RUNNING) {
                m_Result = result;
                if (success) {
                    setState(SUCCESS);
                } else {
                    setState(FAILED);
                }
            }
        }

        private synchronized void setState(final int newState) {
            if (m_Log.isDebugEnabled()) {
                m_Log.debug("GetIorJob.setState() m_state = " + m_state + " newState = " + newState);
            }
            m_state = newState;
            notify();
        }
    }

    private static synchronized SSLSocketFactory getSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        if ( sslSocketFactory == null ) {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                        }

                        public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                        }
                    }
            };
            final SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslSocketFactory = sc.getSocketFactory();
        }

        return sslSocketFactory;
    }
}	    
