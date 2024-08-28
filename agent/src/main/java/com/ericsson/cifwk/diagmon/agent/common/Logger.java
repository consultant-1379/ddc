package com.ericsson.cifwk.diagmon.agent.common;

/**
 * @author EEICJON
 * Generic logging class to print debug messages
 * based on the presence or otherwise of a debug
 * environment variable. Re-inventing the wheel?
 * yes - mainly to keep external dependencies to
 * an absolute minimum.
 *
 */
public class Logger {
    static final int DEBUG = 4;
    static final int INFO = 3;
    static final int WARNING = 2;
    static final int ERROR = 1;
    static final int FATAL = 0;
    
    /**
     * Start with a "fatal" log level so we don't clutter
     * the output files ...
     */
    private static int LOG_LEVEL = FATAL;
    
    static {
        // Initialise the log level
        // TODO: migrate this to the Config.java
        String tmpLL = System.getenv("DDC_AGENT_LOGLEVEL");
        if (tmpLL == null) {
            tmpLL = System.getProperty("com.ericsson.cifwk.diagmon.loglevel");
        }
        
        if (tmpLL != null) {
            if (tmpLL.equalsIgnoreCase("DEBUG")) LOG_LEVEL = DEBUG;
            else if (tmpLL.equalsIgnoreCase("INFO")) LOG_LEVEL = INFO;
            else if (tmpLL.equalsIgnoreCase("WARNING")) LOG_LEVEL = WARNING;
            else if (tmpLL.equalsIgnoreCase("ERROR")) LOG_LEVEL = ERROR;
            else if (tmpLL.equalsIgnoreCase("FATAL")) LOG_LEVEL = FATAL;
        }
    }
    
    /**
     * @param msg - the string to log
     */
    public static void debug(String msg) {
        if (LOG_LEVEL >= DEBUG) {
            doLog("DEBUG", msg);
        }
    }
    
    public static void info(String msg) {
        if (LOG_LEVEL >= INFO) {
            doLog("INFO", msg);
        }
    }

    public static void warning(String msg) {
        if (LOG_LEVEL >= WARNING) {
            doLog("WARNING", msg);
        }
    }
    
    public static void error(String msg) {
        if (LOG_LEVEL >= ERROR) {
            doLog("ERROR", msg);
        }
    }
    
    public static void fatal(String msg) {
        if (LOG_LEVEL >= FATAL) {
            doLog("FATAL", msg);
        }
    }
    
    public static boolean isDebugEnabled() {
        if (LOG_LEVEL >= DEBUG) return true;
        return false;
    }
    
    private static void doLog(String level, String message) {
        System.out.println(Time.getTimestamp() + ": " + level + ": " + message);
    }
}
