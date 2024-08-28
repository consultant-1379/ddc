package com.ericsson.cifwk.diagmon.e2e;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.Descriptor;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Given the JBoss instance connection info and the map of serviceMbeans, this
 * method writes out the XML file to Instr.InstrumentedDir.
 * 
 * If there is an exception, this logs an error and continues - nothing is sent
 * back to the caller
 */
public class MbeanXmlWriter {
    JbossConnectionInfo jbossConnectionInfo;
    Map<ObjectInstance, MBeanInfo> serviceMbeans;
    private final Map<String, List<String>> blackList;
    private final Map<String, Integer> pollIntervals;

    private static final org.apache.logging.log4j.Logger message_log = org.apache.logging.log4j.LogManager
            .getLogger(InstrumentedBeanReaderImpl.class);

    // created at start and then used by subroutines
    Document document;

    private final int DEFAULT_POLL_INTERVAL = 60;

    private final Set<String> allowedTypes = new HashSet<String>();

    public MbeanXmlWriter(final JbossConnectionInfo jbossConnectionInfo,
            final Map<ObjectInstance, MBeanInfo> serviceMbeans) {
        this(jbossConnectionInfo, serviceMbeans, new HashMap<String, List<String>>(), new HashMap<String,Integer>());
    }

    public MbeanXmlWriter(final JbossConnectionInfo jbossConnectionInfo,
            final Map<ObjectInstance, MBeanInfo> serviceMbeans, final Map<String, List<String>> blackList,
            final Map<String,Integer> pollIntervals) {
        this.jbossConnectionInfo = jbossConnectionInfo;
        this.serviceMbeans = serviceMbeans;
        this.blackList = blackList;
        this.pollIntervals = pollIntervals;
        message_log.debug("MbeanXmlWriter():jbossConnectionInfo=" + jbossConnectionInfo + ", serviceMbeans=" + serviceMbeans);

        allowedTypes.add("boolean");
        allowedTypes.add("int");
        allowedTypes.add("long");
        allowedTypes.add("java.util.concurrent.atomic.AtomicInteger");
        allowedTypes.add("java.util.concurrent.atomic.AtomicLong");
        allowedTypes.add("java.lang.Integer");
        allowedTypes.add("java.lang.Long");
    }

    public void writeMbeanXml() {
        if (serviceMbeans == null || serviceMbeans.isEmpty()) {
            message_log.warn("No mbeans found for JBoss instance " + jbossConnectionInfo.getInstanceName());
            return;
        }
        message_log.debug("writeMbeanXml()");
        try {
            document = XmlCreatorUtility.getDocument();
            writeXmlFile();
        } catch (Exception e) {
            message_log.error("Unrecoverable error trying to write XML for JBoss instance=" + jbossConnectionInfo, e);
        }
    }

    protected File writeXmlFile() throws ParserConfigurationException, TransformerConfigurationException,
            TransformerException {
        final File xmlFile = getFile();
        message_log.debug("writeXMLFile():xmlFile=" + xmlFile.getAbsolutePath());

        createInstr();
        message_log.debug("finished createInstr()");

        final DOMSource domSource = new DOMSource(document);
        final StreamResult streamResult = new StreamResult(new StringWriter());

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
        transformer.transform(domSource, streamResult);
        writeIndentedXMLFile(xmlFile, streamResult);

        return xmlFile;
    }

    private void writeIndentedXMLFile(final File xmlFile, final StreamResult streamResult) {
        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(xmlFile);
            final String xmlString = streamResult.getWriter().toString();
            final byte[] contentInBytes = xmlString.getBytes();
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected File getFile() {
        return new File(System.getProperty(Constants.XML_OUTPUT_DIRCETORY_SYS_ARG),
                jbossConnectionInfo.getXmlFilenameForDDC());
    }

    protected void createInstr() {
        final Map<Integer, Map<ObjectInstance, MBeanInfo>> beansByPollInterval = groupByPollInterval();

        final Element instr = document.createElement("instr");
        document.appendChild(instr);

        instr.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        instr.setAttribute("xsi:noNamespaceSchemaLocation", "/opt/ericsson/ERICddc/util/etc/instr/schemas/instr.xsd");
        message_log.debug("  createInstr():<instr>=" + instr.toString());

        for ( Map.Entry<Integer, Map<ObjectInstance, MBeanInfo>> entry : beansByPollInterval.entrySet() ) {
                createProfile(instr, entry.getKey(), entry.getValue());
        }

        message_log.debug("  createInstr():<instr> later=" + instr.toString());
    }

    private Map<Integer, Map<ObjectInstance, MBeanInfo>> groupByPollInterval() {
        message_log.debug("groupByPollInterval: pollIntervals=" + pollIntervals.toString());
        final Map<Integer, Map<ObjectInstance, MBeanInfo>> result = new TreeMap<>();
        for ( Map.Entry<ObjectInstance, MBeanInfo> entry : serviceMbeans.entrySet() ) {
            final String objectName = entry.getKey().getObjectName().getCanonicalName();
            message_log.debug("groupByPollInterval: objectName=" + objectName);
            Integer pollInterval = new Integer(DEFAULT_POLL_INTERVAL);
            for ( final Map.Entry<String,Integer> customPollEntry : pollIntervals.entrySet() ) {
                if ( objectName.matches(customPollEntry.getKey()) ) {
                    pollInterval = customPollEntry.getValue();
                }
            }
            Map<ObjectInstance, MBeanInfo> beansForInterval = result.get(pollInterval);
            if ( beansForInterval == null ) {
                // javax.management.ObjectInstance is doesn't implement
                // Comparable so we'll have to use a HashMap
                beansForInterval = new HashMap<>();
                result.put(pollInterval, beansForInterval);
            }
            beansForInterval.put(entry.getKey(),entry.getValue());
        }
        return result;
    }

    protected void createProfile(final Element instr, final Integer pollInterval, final Map<ObjectInstance, MBeanInfo> beansInProfile ) {
        final Element profile = document.createElement("profile");
        String profileName = jbossConnectionInfo.getInstanceName() + "-Instrumentation";
        if ( pollInterval != DEFAULT_POLL_INTERVAL ) {
                profileName = profileName + "-" + pollInterval.toString();
        }
        profile.setAttribute("name", profileName);
        instr.appendChild(profile);
        message_log.debug("    createProfile(): profile pollInterval=" + pollInterval);

        createPollInterval(profile, pollInterval);
        createProvider(profile, beansInProfile);
    }

    protected void createPollInterval(final Element profile, final Integer pollInterval) {
        final Element pollIntervalElement = document.createElement("pollInterval");
        pollIntervalElement.setTextContent(pollInterval.toString());
        profile.appendChild(pollIntervalElement);
        message_log.debug("      createPollInterval():<pollInterval>=" + pollIntervalElement.toString());
    }

    protected void createProvider(final Element profile, final Map<ObjectInstance, MBeanInfo> beansInProfile) {
        final Element provider = document.createElement("provider");
        provider.setAttribute("type", "jmx");
        provider.setAttribute("name", jbossConnectionInfo.getInstanceName());
        profile.appendChild(provider);
        message_log.debug("      createProvider():<provider>=" + provider.toString());

        final boolean useIP = jbossConnectionInfo.getURL() != null && jbossConnectionInfo.getURL().length() > 0;
        if ( useIP ) {
            createIpService(provider);
        } else {
            createSearchString(provider);
        }

        // Sort the entries by object name to ensure that if have we have the same set of mbeans that
        // we get the same XML output, i.e. the order does change
        final List<Entry<ObjectInstance, MBeanInfo>> entries = new ArrayList<>(beansInProfile.entrySet());
        Collections.sort(entries, new Comparator<Entry<ObjectInstance, MBeanInfo>>(){
            public int compare(Entry<ObjectInstance, MBeanInfo> a,Entry<ObjectInstance, MBeanInfo> b){
                return a.getKey().getObjectName().compareTo(b.getKey().getObjectName());
            }});
        for (final Entry<ObjectInstance, MBeanInfo> entry : entries ) {
            createMetricGroup(provider, entry);
        }

        message_log.debug("      createProvider():<provider> later=" + provider.toString());
    }

    protected void createIpService(final Element provider) {
        final Element ipService = document.createElement("ipService");
        ipService.setAttribute("jmxurl", jbossConnectionInfo.getURL());
        ipService.setAttribute("creds",
                jbossConnectionInfo.getManagementUser() + ":" + jbossConnectionInfo.getManagementPassword());
        provider.appendChild(ipService);
        message_log.debug("        createIpService():<ipService>=" + ipService.toString());
    }

    protected void createSearchString(final Element provider) {
        final Element searchString = document.createElement("searchString");
        searchString.setTextContent(".* -Ds=" + jbossConnectionInfo.getInstanceName() + " .*");
        provider.appendChild(searchString);
    }

    protected void createMetricGroup(final Element provider, final Entry<ObjectInstance, MBeanInfo> entry) {
        final MBeanInfo mbeanInfo = entry.getValue();

        message_log.debug(String.format("               createMetricGroup():mbeanInfo.classname = %s\n"
                + "               createMetricGroup():mbeanInfo.description = %s\n"
                + "               createMetricGroup():mbeanInfo.class = %s\n"
                + "               createMetricGroup():mbeanInfo.descriptor = %s", mbeanInfo.getClassName(),
                mbeanInfo.getDescription(), mbeanInfo.getClass(), mbeanInfo.getDescriptor()));

        final Element metricGroup = getMetricGroup(entry, mbeanInfo);

        metricGroup.setAttribute("name", entry.getKey().getObjectName().getCanonicalName());
        final Element mbeanName = document.createElement("mbeanName");

        mbeanName.setTextContent(entry.getKey().getObjectName().getCanonicalName());
        if(!mbeanInfo.getDescription().isEmpty()){
            mbeanName.setAttribute("description", mbeanInfo.getDescription());
        }
        metricGroup.appendChild(mbeanName);
         message_log.debug("        createMetricGroup():<metricGroup>=" + metricGroup.toString());

        MBeanAttributeInfo[] attributes = entry.getValue().getAttributes();
        if (attributes == null) {
            attributes = new MBeanAttributeInfo[0];
        }

        // Ensure the attributes are sorted by name
        Arrays.sort(attributes, new Comparator<MBeanAttributeInfo>()
        {
           public int compare(MBeanAttributeInfo mai1, MBeanAttributeInfo mai2)
           {
              return mai1.getName().compareTo(mai2.getName());
           }
        });

        final String mbeanNameStr = entry.getKey().getObjectName().getCanonicalName();
        final List<String> blackListedAttributes = getBlackListedAttributes(mbeanNameStr);

        for (final MBeanAttributeInfo attribute : attributes) {
            final String attributeType = attribute.getType();
            message_log.debug("attribute name=" + attribute.getName() + ", type=" + attributeType);
            if (!isBlacklistedAttribute(attribute, blackListedAttributes) &&  allowedTypes.contains(attributeType) ) {
                final Element metric = document.createElement("metric");

                metric.setAttribute("name", attribute.getName());
                Descriptor attributeDesc = attribute.getDescriptor();
                if(attributeDesc != null && attributeDesc.getFieldNames().length !=0){
                    String [] metricDescriptorAttributes={"Category","CollectionType","DisplayName","Interval","Units","VisibleBy"};
                    for (String fieldName : metricDescriptorAttributes) {
                        if(attributeDesc.getFieldValue(fieldName)!=null && !attributeDesc.getFieldValue(fieldName).toString().isEmpty()){
                            metric.setAttribute(fieldName, attributeDesc.getFieldValue(fieldName).toString());
                        }
                    }
                }

                metricGroup.appendChild(metric);
                message_log.debug("          createMetricGroup():<metric>=" + metric.toString());
            }
        }
        provider.appendChild(metricGroup);
    }

    private Element getMetricGroup(final Entry<ObjectInstance, MBeanInfo> entry, final MBeanInfo mbeanInfo) {
        final Element metricGroup = document.createElement("metricGroup");
        String metricGroupName = (String) mbeanInfo.getDescriptor().getFieldValue("DisplayName");
        if (metricGroupName != null && metricGroupName.length() == 0) {
            metricGroupName = entry.getKey().getObjectName().getCanonicalKeyPropertyListString()
                    .replaceAll("type=", "");
        }
        if (metricGroupName != null) {
            metricGroupName = metricGroupName.replaceAll(" ", "_");
        }
        message_log.debug("printing metricGroupName :: " + metricGroupName);
        return metricGroup;
    }

    private List<String> getBlackListedAttributes(final String mbeanNameStr) {
        for (final String match : blackList.keySet()) {
            if (mbeanNameStr.matches(match)) {
                message_log.debug(" blacklisted mbean : " + mbeanNameStr);
                return blackList.get(match);
            }
        }
        return new ArrayList<String>();
    }

    private boolean isBlacklistedAttribute(final MBeanAttributeInfo attribute, final List<String> blackListedAttr) {
        boolean blackListed = false;
        for (final String blackListedAttribute : blackListedAttr) {
            if (attribute.getName().matches(blackListedAttribute)) {
                blackListed = true;
                message_log.debug(" blacklisted attribute: " + attribute.getName());
                break;
            }
        }
        return blackListed;
    }

    public static String readFile(final String path) {
        try {
            final byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded);
        } catch (IOException e) {
            return "??IOException " + e + " ??";
        }
    }
}
