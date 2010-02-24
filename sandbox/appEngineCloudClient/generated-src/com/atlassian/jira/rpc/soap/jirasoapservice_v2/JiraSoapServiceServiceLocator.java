/**
 * JiraSoapServiceServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.jira.rpc.soap.jirasoapservice_v2;

public class JiraSoapServiceServiceLocator extends org.apache.axis.client.Service implements com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapServiceService {

    public JiraSoapServiceServiceLocator() {
    }


    public JiraSoapServiceServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public JiraSoapServiceServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for JirasoapserviceV2
    private java.lang.String JirasoapserviceV2_address = "http://jira.atlassian.com/rpc/soap/jirasoapservice-v2";

    public java.lang.String getJirasoapserviceV2Address() {
        return JirasoapserviceV2_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String JirasoapserviceV2WSDDServiceName = "jirasoapservice-v2";

    public java.lang.String getJirasoapserviceV2WSDDServiceName() {
        return JirasoapserviceV2WSDDServiceName;
    }

    public void setJirasoapserviceV2WSDDServiceName(java.lang.String name) {
        JirasoapserviceV2WSDDServiceName = name;
    }

    public com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService getJirasoapserviceV2() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(JirasoapserviceV2_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getJirasoapserviceV2(endpoint);
    }

    public com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService getJirasoapserviceV2(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.atlassian.jira.rpc.soap.jirasoapservice_v2.JirasoapserviceV2SoapBindingStub _stub = new com.atlassian.jira.rpc.soap.jirasoapservice_v2.JirasoapserviceV2SoapBindingStub(portAddress, this);
            _stub.setPortName(getJirasoapserviceV2WSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setJirasoapserviceV2EndpointAddress(java.lang.String address) {
        JirasoapserviceV2_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService.class.isAssignableFrom(serviceEndpointInterface)) {
                com.atlassian.jira.rpc.soap.jirasoapservice_v2.JirasoapserviceV2SoapBindingStub _stub = new com.atlassian.jira.rpc.soap.jirasoapservice_v2.JirasoapserviceV2SoapBindingStub(new java.net.URL(JirasoapserviceV2_address), this);
                _stub.setPortName(getJirasoapserviceV2WSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("jirasoapservice-v2".equals(inputPortName)) {
            return getJirasoapserviceV2();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://jira.atlassian.com/rpc/soap/jirasoapservice-v2", "JiraSoapServiceService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://jira.atlassian.com/rpc/soap/jirasoapservice-v2", "jirasoapservice-v2"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("JirasoapserviceV2".equals(portName)) {
            setJirasoapserviceV2EndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
