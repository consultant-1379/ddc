package com.ericsson.cifwk.diagmon.util.instr.providers.versant;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ericsson.cifwk.diagmon.util.instr.MetricDescription;
import com.ericsson.cifwk.diagmon.util.instr.MetricGroupDescription;
import com.ericsson.cifwk.diagmon.util.instr.Provider;
import com.ericsson.cifwk.diagmon.util.instr.InstrException;
import com.ericsson.cifwk.diagmon.util.instr.config.ConfigTreeNode;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

import com.versant.admin.JmxUtil;

public class ProviderImpl extends Provider {
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager.getLogger(ProviderImpl.class);

    private MBeanServer mbs;
    private VersantConfig config;
    private boolean connected = false;

    private static final String DB_URL = "dps_integration@db1-service";

    public void init(final ConfigTreeNode base) throws InstrException {
        config = new VersantConfig(base);
        mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
        JmxUtil.publishServerStats(DB_URL,"versant","shversant");

        connect();
    }

    public String getName() {
        return "VDB_dps_integration";
    }

    public List<MetricGroupDescription> getMetricGroupDescriptions() {
        final List<MetricDescription> metricDescriptions = new LinkedList<>();
        for ( final String metric : config.metrics ) {
            if ( metric.equals("db_xact_active") || metric.equals("db_running_threads")) {
                metricDescriptions.add(new MetricDescription(metric, metric, MetricDescription.Type.Gauge));
            } else {
                metricDescriptions.add(new MetricDescription(metric, metric, MetricDescription.Type.Counter));
            }
        }
        final List<MetricGroupDescription> results = new LinkedList<>();
        results.add(new MetricGroupDescription(getName(), metricDescriptions));
        return results;
    }

    public void logValues() throws InstrException {
        final String[] metricNames = config.metrics.toArray(new String[config.metrics.size()]);

        try {
            if ( ! connected ) {
                connect();
            }

            final AttributeList values = mbs.getAttributes(new ObjectName("com.versant.jpa:type=Databases,category=" + DB_URL + ",sub=EnabledStatistics"),
                                                           metricNames);
            if ( m_Log.isDebugEnabled() ) { m_Log.debug("logValues: values=" + values); }
            final Map<String,Attribute> valuesByName = new HashMap<String,Attribute>(values.size());
            for ( Attribute value : values.asList() ) {
                valuesByName.put(value.getName(),value);
            }

            final ConfigTreeNode groupoutput = new ConfigTreeNode(getName());
            for ( String metric : metricNames ) {
                final ConfigTreeNode outputMetric = new ConfigTreeNode(metric);
                final String value = String.valueOf(valuesByName.get(metric).getValue());
                if ( m_Log.isDebugEnabled() ) { m_Log.debug("logValues: metric=" + metric + ", value=" + value); }
                outputMetric.addData(value);
                groupoutput.addChild(outputMetric);
            }

            output(groupoutput);
        } catch (InstanceNotFoundException | MalformedObjectNameException | ReflectionException | RuntimeMBeanException | InstrException  e) {
            m_Log.warn("Failed to read stats from backend", e);
            disconnect();
        }

    }

    public void shutdown() {
        disconnect();
    }

    private void connect() throws InstrException {
        try {
            setStatsState(true);
            connected = true;
        } catch (InstanceNotFoundException | MalformedObjectNameException | ReflectionException | InvalidAttributeValueException | AttributeNotFoundException | MBeanException e) {
            throw new InstrException("Failed to activate stats", e);
        }

    }

    private void disconnect() {
        connected = false;
/*
        TORF-283615: Don't disable stats at shutdown
        try {
            setStatsState(false);
        } catch (InstanceNotFoundException | MalformedObjectNameException | ReflectionException | InvalidAttributeValueException | AttributeNotFoundException | MBeanException e) {
            m_Log.warn("Failed deativate stats", e);
        }
*/
    }

    private void setStatsState(final boolean state) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, InvalidAttributeValueException, AttributeNotFoundException, MBeanException {
        final String[] metricNames = config.metrics.toArray(new String[config.metrics.size()]);
        final ObjectName availStat = new ObjectName("com.versant.jpa:type=Databases,category=" + DB_URL + ",sub=AvailableStatistics");
        for ( String metricName : metricNames ) {
            mbs.setAttribute(availStat,new Attribute(metricName, new Boolean(state)));
        }
    }
}
