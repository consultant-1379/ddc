package com.ericsson.cifwk.diagmon.agent.eventservice;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sun.tools.tree.ThisExpression;

import com.ericsson.cifwk.diagmon.agent.IEventService;
import com.ericsson.cifwk.diagmon.agent.common.Config;
import com.ericsson.cifwk.diagmon.agent.common.DDCThreadFactory;

public class EventService implements IEventService {
    // the default event service.
    private static EventService svc;
    
    // event queue - used to retrieve the next event for forwarding
    private static BlockingQueue<Event> eventQueue;
    private static int threadPoolSize = 5;
    // pool of threads which handle polling of registered metrics
    private static ScheduledExecutorService pool;
    
    private int droppedEvents = 0;
    
    private EventService() {
        pool = new ScheduledThreadPoolExecutor(threadPoolSize,
                new DDCThreadFactory());
    }

    public void registerMetric(String jmxBeanObjectName, String paramName,
            String description, int collectionPeriod) {
         MetricPoller mp = new JMXMetricPoller(jmxBeanObjectName, paramName,
                 description, collectionPeriod);
         pool.scheduleAtFixedRate(mp, 0, mp.getPollIntervalSeconds(), TimeUnit.SECONDS);
    }

    public void sendEvent(long timeInMilliseconds, String name,
            String description, String value) {
        boolean success = getEventQueue().offer(new Event(timeInMilliseconds, name, description, value));
        if (success && this.droppedEvents > 0) {
            int tmp = droppedEvents;
            droppedEvents = 0;
            sendEvent(System.currentTimeMillis(),
                    "DDC_AGENT_DROPPED_EVENTS",
                    "Events dropped by DDC Agent", "" + tmp);
            // check to see if that event was successful, otherwise
            // keep the count of dropped events
            if (droppedEvents != 0) droppedEvents += tmp;
        } else if (! success) {
            // didn't send the event, increment the counter
            droppedEvents++;
        }
    }
    
    public static EventService get() {
        if (svc == null) svc = new EventService();
        return svc;
    }

    public static BlockingQueue<Event> getEventQueue() {
        if (eventQueue == null) {
            eventQueue = new ArrayBlockingQueue<Event>(Config.getInstance().getQueueLength());
        }
        return eventQueue;
    }

    
}
