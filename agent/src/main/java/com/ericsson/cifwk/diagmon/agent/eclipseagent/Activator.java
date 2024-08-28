package com.ericsson.cifwk.diagmon.agent.eclipseagent;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import com.ericsson.cifwk.diagmon.agent.common.Config;
import com.ericsson.cifwk.diagmon.agent.common.Logger;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventHandler;
import com.ericsson.cifwk.diagmon.agent.eventservice.EventService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ericsson.cifwk.diagmon.agent.eclipseagent";

	// The shared instance
	private static Activator plugin;
	
	// the bundleContext we rely on
	private BundleContext context;
	
	// our listener object
	private Listener listener = new Listener();
	
	// Our RMI Event Forwarder
	private EventHandler eventHandler;
	
	/**
	 * The constructor
	 */
	public Activator() {
	    Logger.debug("Activator constructor");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		plugin = this;
		Logger.debug("Activator start");
		context.addBundleListener(listener);
		context.addFrameworkListener(listener);
		context.addServiceListener(listener);
		
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		Logger.debug("Activator stop");
		context.removeBundleListener(listener);
		context.removeFrameworkListener(listener);
		context.removeServiceListener(listener);
	    eventHandler.disable();
		eventHandler.flush();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
