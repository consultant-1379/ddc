/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.cifwk.diagmon.e2e;

import javax.management.MBeanServerConnection;

/**
 * This class attempts to read all the JBoss instance configuration details and
 * convert them to XML files for the JBoss instance.
 *
 * DDC calls the public method getInstrumentedBeanProfiles(), which kicks off
 * the daemon thread. This operates asynchronously; as each JBoss instance is
 * contacted, we write a file into the pollDir. Therefore, this method must be
 * called <b>after</b> the InstrDirectoryPoller is instantiated.
 *
 * It may be that some JBoss instances are not running when this is called.
 * Therefore, we keep trying to connect to each configured instance on a timer.
 * In addition, if we can make a connection, we keep the mbeanConnection.
 *
 * However, if the JBoss instance is not available when the collection is first
 * called, we try to make a connection to all the instances for up to one
 * minute. If we can't make a connection, then we continue and collect the other
 * statistics.
 *
 * If a JBoss instance doesn't come up at start of day, we poll for it in a
 * delay loop in the daemon thread.
 */
interface InstrumentedBeanReader {

    long CONNECTION_TRY_WAIT = 30 * 1000L; // how long to wait to try remaining
                                           // configured instances


    /**
     * This is the main entry point for ddc Instr. It kicks off the daemon
     * thread and returns quickly. The daemon thread polls until all JBoss
     * instances for this node have been contacted.
     */
    void getInstrumentedBeanProfiles();

    /**
     * This gets the JBoss instance from LITP. It's the first method called and
     * it's called synchronously. Instance connection details are collected and
     * returned.
     *
     * @return the runnable instance for this virtual machine
     */
    JbossConnectionInfo getRunnableInstance();

    /**
     * Called from the daemon thread to try to contact the JBoss instances. If
     * they are running, then reads the MBeanInfo and writes the XML file for
     * Instr.
     */
    void collectJbossInstanceAndMbeanInfo() throws PluginException;


    /**
     * Given JBoss instance connection info, try to establish a connection to
     * its MBean server. This is only called once we know the JBoss instance is
     * running.
     *
     * @param jbossConnectionInfo JBoss Connection Info
     * @return the MBeanServerConnection if it is reachable, otherwise null
     */
    MBeanServerConnection establishConnectionToInstanceMbeanServer(JbossConnectionInfo jbossConnectionInfo);
}
