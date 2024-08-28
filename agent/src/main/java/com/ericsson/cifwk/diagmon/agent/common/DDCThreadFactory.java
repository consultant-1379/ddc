package com.ericsson.cifwk.diagmon.agent.common;

import java.util.concurrent.ThreadFactory;

public class DDCThreadFactory implements ThreadFactory {
    // counter for the next thread number
    private static int threadNum = 1;

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("DDC-Agent-poolThread-" + threadNum++);
        t.setDaemon(true);
        return t;
    }

}
