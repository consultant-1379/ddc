package com.ericsson.cifwk.diagmon.agent.eclipseagent;

import org.eclipse.ui.IStartup;

import com.ericsson.cifwk.diagmon.agent.common.Logger;

/**
 * @author EEICJON
 *
 */
public class Startup implements IStartup {

    /* (non-Javadoc)
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    public void earlyStartup() {
        Logger.debug("Early startup initialising");
    }
}
