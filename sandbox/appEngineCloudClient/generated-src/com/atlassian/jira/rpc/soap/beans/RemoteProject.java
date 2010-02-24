/**
 * RemoteProject.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.jira.rpc.soap.beans;

public class RemoteProject  extends com.atlassian.jira.rpc.soap.beans.AbstractNamedRemoteEntity  implements java.io.Serializable {
    private java.lang.String description;

    private com.atlassian.jira.rpc.soap.beans.RemoteScheme issueSecurityScheme;

    private java.lang.String key;

    private java.lang.String lead;

    private com.atlassian.jira.rpc.soap.beans.RemoteScheme notificationScheme;

    private com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme permissionScheme;

    private java.lang.String projectUrl;

    private java.lang.String url;

    public RemoteProject() {
    }

    public RemoteProject(
           java.lang.String id,
           java.lang.String name,
           java.lang.String description,
           com.atlassian.jira.rpc.soap.beans.RemoteScheme issueSecurityScheme,
           java.lang.String key,
           java.lang.String lead,
           com.atlassian.jira.rpc.soap.beans.RemoteScheme notificationScheme,
           com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme permissionScheme,
           java.lang.String projectUrl,
           java.lang.String url) {
        super(
            id,
            name);
        this.description = description;
        this.issueSecurityScheme = issueSecurityScheme;
        this.key = key;
        this.lead = lead;
        this.notificationScheme = notificationScheme;
        this.permissionScheme = permissionScheme;
        this.projectUrl = projectUrl;
        this.url = url;
    }


    /**
     * Gets the description value for this RemoteProject.
     * 
     * @return description
     */
    public java.lang.String getDescription() {
        return description;
    }


    /**
     * Sets the description value for this RemoteProject.
     * 
     * @param description
     */
    public void setDescription(java.lang.String description) {
        this.description = description;
    }


    /**
     * Gets the issueSecurityScheme value for this RemoteProject.
     * 
     * @return issueSecurityScheme
     */
    public com.atlassian.jira.rpc.soap.beans.RemoteScheme getIssueSecurityScheme() {
        return issueSecurityScheme;
    }


    /**
     * Sets the issueSecurityScheme value for this RemoteProject.
     * 
     * @param issueSecurityScheme
     */
    public void setIssueSecurityScheme(com.atlassian.jira.rpc.soap.beans.RemoteScheme issueSecurityScheme) {
        this.issueSecurityScheme = issueSecurityScheme;
    }


    /**
     * Gets the key value for this RemoteProject.
     * 
     * @return key
     */
    public java.lang.String getKey() {
        return key;
    }


    /**
     * Sets the key value for this RemoteProject.
     * 
     * @param key
     */
    public void setKey(java.lang.String key) {
        this.key = key;
    }


    /**
     * Gets the lead value for this RemoteProject.
     * 
     * @return lead
     */
    public java.lang.String getLead() {
        return lead;
    }


    /**
     * Sets the lead value for this RemoteProject.
     * 
     * @param lead
     */
    public void setLead(java.lang.String lead) {
        this.lead = lead;
    }


    /**
     * Gets the notificationScheme value for this RemoteProject.
     * 
     * @return notificationScheme
     */
    public com.atlassian.jira.rpc.soap.beans.RemoteScheme getNotificationScheme() {
        return notificationScheme;
    }


    /**
     * Sets the notificationScheme value for this RemoteProject.
     * 
     * @param notificationScheme
     */
    public void setNotificationScheme(com.atlassian.jira.rpc.soap.beans.RemoteScheme notificationScheme) {
        this.notificationScheme = notificationScheme;
    }


    /**
     * Gets the permissionScheme value for this RemoteProject.
     * 
     * @return permissionScheme
     */
    public com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme getPermissionScheme() {
        return permissionScheme;
    }


    /**
     * Sets the permissionScheme value for this RemoteProject.
     * 
     * @param permissionScheme
     */
    public void setPermissionScheme(com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme permissionScheme) {
        this.permissionScheme = permissionScheme;
    }


    /**
     * Gets the projectUrl value for this RemoteProject.
     * 
     * @return projectUrl
     */
    public java.lang.String getProjectUrl() {
        return projectUrl;
    }


    /**
     * Sets the projectUrl value for this RemoteProject.
     * 
     * @param projectUrl
     */
    public void setProjectUrl(java.lang.String projectUrl) {
        this.projectUrl = projectUrl;
    }


    /**
     * Gets the url value for this RemoteProject.
     * 
     * @return url
     */
    public java.lang.String getUrl() {
        return url;
    }


    /**
     * Sets the url value for this RemoteProject.
     * 
     * @param url
     */
    public void setUrl(java.lang.String url) {
        this.url = url;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RemoteProject)) return false;
        RemoteProject other = (RemoteProject) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.description==null && other.getDescription()==null) || 
             (this.description!=null &&
              this.description.equals(other.getDescription()))) &&
            ((this.issueSecurityScheme==null && other.getIssueSecurityScheme()==null) || 
             (this.issueSecurityScheme!=null &&
              this.issueSecurityScheme.equals(other.getIssueSecurityScheme()))) &&
            ((this.key==null && other.getKey()==null) || 
             (this.key!=null &&
              this.key.equals(other.getKey()))) &&
            ((this.lead==null && other.getLead()==null) || 
             (this.lead!=null &&
              this.lead.equals(other.getLead()))) &&
            ((this.notificationScheme==null && other.getNotificationScheme()==null) || 
             (this.notificationScheme!=null &&
              this.notificationScheme.equals(other.getNotificationScheme()))) &&
            ((this.permissionScheme==null && other.getPermissionScheme()==null) || 
             (this.permissionScheme!=null &&
              this.permissionScheme.equals(other.getPermissionScheme()))) &&
            ((this.projectUrl==null && other.getProjectUrl()==null) || 
             (this.projectUrl!=null &&
              this.projectUrl.equals(other.getProjectUrl()))) &&
            ((this.url==null && other.getUrl()==null) || 
             (this.url!=null &&
              this.url.equals(other.getUrl())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getDescription() != null) {
            _hashCode += getDescription().hashCode();
        }
        if (getIssueSecurityScheme() != null) {
            _hashCode += getIssueSecurityScheme().hashCode();
        }
        if (getKey() != null) {
            _hashCode += getKey().hashCode();
        }
        if (getLead() != null) {
            _hashCode += getLead().hashCode();
        }
        if (getNotificationScheme() != null) {
            _hashCode += getNotificationScheme().hashCode();
        }
        if (getPermissionScheme() != null) {
            _hashCode += getPermissionScheme().hashCode();
        }
        if (getProjectUrl() != null) {
            _hashCode += getProjectUrl().hashCode();
        }
        if (getUrl() != null) {
            _hashCode += getUrl().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(RemoteProject.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteProject"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("", "description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("issueSecurityScheme");
        elemField.setXmlName(new javax.xml.namespace.QName("", "issueSecurityScheme"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteScheme"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("key");
        elemField.setXmlName(new javax.xml.namespace.QName("", "key"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lead");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lead"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("notificationScheme");
        elemField.setXmlName(new javax.xml.namespace.QName("", "notificationScheme"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteScheme"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("permissionScheme");
        elemField.setXmlName(new javax.xml.namespace.QName("", "permissionScheme"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemotePermissionScheme"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("projectUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("", "projectUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("url");
        elemField.setXmlName(new javax.xml.namespace.QName("", "url"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
