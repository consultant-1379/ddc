package com.ericsson.cifwk.diagmon.util.kstat;

public class KStat {
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(KStat.class);
    private static boolean libLoaded = false;
    
    private static KStat stat;

    private KStat() throws KStatException {
        if (libLoaded == false) {
            try {
                System.loadLibrary("ddc_kstat");
                libLoaded = true;
            } catch (Exception e) {
                m_Log.error("Error loading kstat library: " + e.getMessage());
                if (m_Log.isDebugEnabled()) e.printStackTrace();
                throw new KStatException("could not load DDC Kstat library: " + e.getMessage());
            } catch (UnsatisfiedLinkError e) {
                m_Log.error("Error loading kstat library: " + e.getMessage());
                if (m_Log.isDebugEnabled()) e.printStackTrace();
                throw new KStatException("could not load DDC Kstat library: " + e.getMessage());
            }
        }
    }
    
    public static KStat getKStat() throws KStatException {
        if (stat == null) stat = new KStat();
        return stat;
    }

    public synchronized String getStatistic(String name) throws KStatException {
        if (libLoaded == false)
            throw new KStatException("DDC Kstat library not loaded");
        try {
            return get_kstat(name);
        } catch (Exception e) {
            m_Log.error("error getting kstat: " + e.getMessage());
            if (m_Log.isDebugEnabled()) e.printStackTrace();
        }
        // should not get here
        return "";
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("USAGE: jkstat <kstat>");
            System.exit(1);
        }
        String stat = args[0];
        KStat ks = getKStat();
        System.out.println(stat + ": " + ks.get_kstat(stat));
    }

    private native String get_kstat(String name);
}
