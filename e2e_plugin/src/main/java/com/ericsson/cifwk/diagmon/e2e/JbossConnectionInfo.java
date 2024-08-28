package com.ericsson.cifwk.diagmon.e2e;

import javax.management.MBeanServerConnection;

public class JbossConnectionInfo {

    private String instanceName;
    private String ipAddress;
    private String managementPassword;
    private String managementUser;
    private String managementPort;
    private MBeanServerConnection mbeanServerConnection;
    private String url;

    public JbossConnectionInfo() {
        this.instanceName = "";
        this.ipAddress = "";
        this.managementPassword = "";
        this.managementUser = "";
        this.managementPort = "";
        this.url = "";
        this.mbeanServerConnection = null;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(final String instanceName) {
        this.instanceName = instanceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getManagementPassword() {
        return managementPassword;
    }

    public void setManagementPassword(final String managementPassword) {
        this.managementPassword = managementPassword;
    }

    public MBeanServerConnection getMbeanServerConnection() {
        return mbeanServerConnection;
    }

    public void setMbeanServerConnection(final MBeanServerConnection mbeanServerConnection) {
        this.mbeanServerConnection = mbeanServerConnection;
    }

    public String getManagementUser() {
        return managementUser;
    }

    public void setManagementUser(final String managementUser) {
        this.managementUser = managementUser;
    }

    public boolean hasManagementUser() {
        return !managementUser.isEmpty();
    }

    public String getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(final String managementPort) {
        this.managementPort = managementPort;
    }

    public String getXmlFilenameForDDC() {
        return "e2e_" + instanceName + ".xml";
    }

    public void setURL(final String url) {
        this.url = url;
    }

    public String getURL() {
        return this.url;
    }

    @Override
    public String toString() {
        return "JbossConnectionInfo[" + "instanceName=" + instanceName + ",ipAddress=" + ipAddress
            + ",managementUser=" + managementUser + ",managementPort=" + managementPort + ", url=" + url + "]";
    }

}
