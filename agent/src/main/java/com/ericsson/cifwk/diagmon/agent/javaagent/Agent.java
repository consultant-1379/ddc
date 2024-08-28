package com.ericsson.cifwk.diagmon.agent.javaagent;

import java.lang.instrument.Instrumentation;

import com.ericsson.cifwk.diagmon.agent.common.Config;
import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventHandler;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Agent {
	
	public static void premain(String agentArguments, Instrumentation instrumentation) {
	    EventHandler eventHandler;

		// get the configured event handler
		String handlerName = Config.getInstance().getEventHandlerType();
		try {
            if (Logger.isDebugEnabled()) Logger.debug("Initialising event handler "
                    + handlerName);
            Class<?> eventHandlerClass =
                Class.forName("com.ericsson.cifwk.diagmon.agent.eventhandlers." +
                        handlerName + ".EventHandlerImpl");
            java.lang.reflect.Constructor<?> constructor =
                eventHandlerClass.getConstructor(new Class<?>[] {});
            eventHandler = (EventHandler)constructor.newInstance(new Object[] {});
            eventHandler.setEventSource(EventService.getEventQueue());
        } catch (ClassCastException e) {
            Logger.error("event handler " + handlerName
                    + " does not implement the EventHandler interface");
        } catch (ClassNotFoundException e) {
            Logger.error("event handler " + handlerName + ": " +
                    "Class not found: " + e.getMessage()); 
        } catch (Exception e) {
            if (Logger.isDebugEnabled()) {
                Logger.debug("Exception processing event handler " + handlerName + ":");
                e.printStackTrace();
            } else {
                Logger.error("Exception " + e.getClass() +
                        " handling event handler " + handlerName + ": " + e.getMessage());
            }
        }
		EventService.get().registerMetric("java.lang:type=Threading",
		        "ThreadCount", "Thread Count", Config.getInstance().getDefaultPollIntervalSeconds());
		EventService.get().registerMetric("java.lang:type=Memory",
		        "HeapMemoryUsage", "Memory Utilisation", Config.getInstance().getDefaultPollIntervalSeconds());
	}
}
